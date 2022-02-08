package com.github.redditvanced.gradle.task

import com.github.redditvanced.gradle.getRedditVanced
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import jadx.api.impl.SimpleCodeWriter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.function.Function

abstract class GenSourcesTask : DefaultTask() {
	@TaskAction
	fun genSources() {
		val extension = project.extensions.getRedditVanced()
		val sourcesJar = File(extension.cacheDir, "reddit-${extension.projectRedditVersion.get()}-sources.jar")

		val args = JadxArgs().apply {
			setInputFile(extension.apkFile)
			outDirSrc = sourcesJar
			isSkipResources = true
			isShowInconsistentCode = true
			isRespectBytecodeAccModifiers = true
			isFsCaseSensitive = true
			isDebugInfo = false
			isInlineAnonymousClasses = false
			isInlineMethods = false
			isReplaceConsts = true

			codeCache = NoOpCodeCache()
			codeWriterProvider = Function { SimpleCodeWriter(it) }
			threadsCount = Runtime.getRuntime()
				.availableProcessors()
				.times(0.80f).toInt()
				.coerceAtLeast(1)
		}

		JadxDecompiler(args).use { decompiler ->
			decompiler.load()
			decompiler.save()
		}

		project.logger.lifecycle("Decompiled to ${args.outDirSrc.absolutePath}")
	}
}
