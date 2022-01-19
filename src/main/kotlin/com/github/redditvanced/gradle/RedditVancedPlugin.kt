package com.github.redditvanced.gradle

import com.github.redditvanced.gradle.models.PluginManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class RedditVancedPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.extensions.create("redditVanced", RedditVancedExtension::class.java, project)
		configureRedditConfiguration(project)
	}

	fun ExtensionContainer.getRedditVanced() =
		getByType(RedditVancedExtension::class.java)
}

abstract class RedditVancedExtension(project: Project) {
	/**
	 * The build method to use for this project.
	 * This should never be changed from [ProjectType.PLUGIN] for plugins.
	 */
	val projectType: Property<ProjectType> =
		project.objects.property(ProjectType::class.java).convention(ProjectType.PLUGIN)

	val authors: ListProperty<PluginManifest.Author> =
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
	val customUpdaterUrl: Property<String> =
		project.objects.property(String::class.java)

	/**
	 * Entire history of the changelog, markdown supported.
	 */
	val changelog: Property<String> =
		project.objects.property(String::class.java)

	internal val minimumRedditVersion: Property<Int> =
		project.objects.property(Int::class.java)

	/**
	 * The class that's annotated with `@RedditVancedPlugin`
	 */
	internal val pluginClass: Property<String> =
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
