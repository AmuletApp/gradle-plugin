package com.github.redditvanced.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import com.github.redditvanced.gradle.models.PluginManifest
import com.github.redditvanced.gradle.task.CompileDexTask
import com.github.redditvanced.gradle.task.CompileResourcesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.compile.AbstractCompile

private const val TASK_GROUP = "reddit vanced"

@Suppress("unused")
abstract class Plugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.extensions.create("redditVanced", RedditVancedExtension::class.java, project)
		configureRedditConfiguration(project)

		val intermediates = project.buildDir.resolve("intermediates")
		val pluginClassFile = intermediates.resolve("pluginClass")

		project.tasks.register("compileResources", CompileResourcesTask::class.java) { task ->
			task.group = TASK_GROUP

			val processManifestTask = project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
			task.dependsOn(processManifestTask)

			val android = project.extensions.getAndroid()
			task.input.set(android.sourceSets.getByName("main").res.srcDirs.single())
			task.manifestFile.set(processManifestTask.manifestOutputFile)

			task.outputFile.set(intermediates.resolve("res.apk"))

			task.doLast { _ ->
				val resApkFile = task.outputFile.asFile.get()

				if (resApkFile.exists()) {
					project.tasks.named("make", AbstractCopyTask::class.java) {
						it.from(project.zipTree(resApkFile)) { copySpec ->
							copySpec.exclude("AndroidManifest.xml")
						}
					}
				}
			}
		}

		project.tasks.register("compileDex", CompileDexTask::class.java) { task ->
			task.group = TASK_GROUP
			task.pluginClassFile.set(pluginClassFile)

			val kotlinTask = project.tasks.getByName("compileDebugKotlin") as AbstractCompile
			task.dependsOn(kotlinTask)
			task.input.from(kotlinTask.destinationDirectory)
		}


	}
}

internal fun ExtensionContainer.getRedditVanced() =
	getByType(RedditVancedExtension::class.java)

internal fun ExtensionContainer.getAndroid() =
	getByName("android") as BaseExtension

abstract class RedditVancedExtension(project: Project) {
	/**
	 * The build method to use for this project.
	 * This should never be changed from [ProjectType.PLUGIN] for plugins.
	 */
	var projectType: Property<ProjectType> =
		project.objects.property(ProjectType::class.java).convention(ProjectType.PLUGIN)

	var authors: ListProperty<PluginManifest.Author> =
		project.objects.listProperty(PluginManifest.Author::class.java).empty()

	/**
	 * Add an author to this plugin that will be displayed on the plugin card in-app.
	 * Only available for [ProjectType.PLUGIN] projects.
	 */
	fun author(name: String, discordId: Long? = null, redditUsername: String? = null) =
		authors.add(PluginManifest.Author(name, discordId, redditUsername))

	/**
	 * Indicates that the updater should use this for updating plugins, instead of the plugin store.
	 * Setting this will make your plugin not eligible to be published on the plugin store.
	 * Example: `https://raw.githubusercontent.com/DiamondMiner88/aliucord-plugins/builds/plugin.rvp`
	 */
	var customUpdaterUrl: Property<String> =
		project.objects.property(String::class.java)

	/**
	 * Entire history of the changelog, markdown supported.
	 */
	var changelog: Property<String> =
		project.objects.property(String::class.java)

	internal val minimumRedditVersion: Property<Int> =
		project.objects.property(Int::class.java)

	/**
	 * The class that's annotated with `@RedditVancedPlugin`
	 */
	internal var pluginClass: Property<String> =
		project.objects.property(String::class.java)
}

enum class ProjectType {
	/* Internals, do not use */
	CORE,

	/* Internals, do not use */
	INJECTOR,

	/* A regular RedditVanced plugin */
	PLUGIN,
}
