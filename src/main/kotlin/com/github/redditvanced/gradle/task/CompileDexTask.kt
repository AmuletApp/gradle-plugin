package com.github.redditvanced.gradle.task

import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions
import com.android.builder.dexing.ClassFileInputs
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.github.redditvanced.gradle.ProjectType
import com.github.redditvanced.gradle.getAndroid
import com.github.redditvanced.gradle.getRedditVanced
import com.google.common.io.Closer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

abstract class CompileDexTask : DefaultTask() {
	@InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	val input: ConfigurableFileCollection = project.objects.fileCollection()

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@Suppress("UnstableApiUsage")
	@TaskAction
	fun compileDex() {
		val android = project.extensions.getAndroid()
		val redditvanced = project.extensions.getRedditVanced()

		val dexOutputDir = outputFile.get().asFile.parentFile

		val closer = Closer.create()
		val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
			DexParameters(
				minSdkVersion = android.defaultConfig.maxSdkVersion ?: 24,
				debuggable = true,
				dexPerClass = false,
				withDesugaring = true,
				desugarBootclasspath = ClassFileProviderFactory(android.bootClasspath.map(File::toPath))
					.also { closer.register(it) },
				desugarClasspath = ClassFileProviderFactory(listOf<Path>()).also { closer.register(it) },
				coreLibDesugarConfig = null,
				coreLibDesugarOutputKeepRuleFile = null,
				messageReceiver = MessageReceiverImpl(
					SyncOptions.ErrorFormatMode.HUMAN_READABLE,
					LoggerFactory.getLogger(CompileDexTask::class.java)
				)
			)
		)

		val fileStreams = input.map { input ->
			val inputs = ClassFileInputs.fromPath(input.toPath())
			inputs.entries { _, _ -> true }
		}.toTypedArray()

		val files = Arrays.stream(fileStreams).flatMap { it }.collect(Collectors.toList())

		dexBuilder.convert(
			files.stream(),
			dexOutputDir.toPath()
		)

		if (redditvanced.projectType.get() == ProjectType.PLUGIN) {
			for (file in files) {
				val reader = ClassReader(file.readAllBytes())

				val classNode = ClassNode()
				reader.accept(classNode, 0)

				for (annotation in classNode.visibleAnnotations.orEmpty() + classNode.invisibleAnnotations.orEmpty()) {
					if (annotation.desc == "Lcom/github/redditvanced/core/annotations/RedditVancedPlugin;") {
						require(!redditvanced.pluginClass.isPresent) { "Only 1 active plugin class per project is supported" }
						redditvanced.pluginClass.set(classNode.name.replace('/', '.'))

						annotation.values?.chunked(2)?.forEach {
							when (it[0]) {
								"loadResources" -> redditvanced.loadResources.set(it[1] as Boolean)
								"requiresRestart" -> redditvanced.requiresRestart.set(it[1] as Boolean)
							}
						}
					}
				}
			}

			require(redditvanced.pluginClass.isPresent) {
				"No plugin class found, make sure your plugin class is annotated with @RedditVancedPlugin"
			}
		}

		logger.lifecycle("Compiled dex to ${outputFile.get()}")
	}
}
