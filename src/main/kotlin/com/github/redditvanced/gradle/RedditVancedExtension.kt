package com.github.redditvanced.gradle

import com.android.build.gradle.BaseExtension
import com.github.redditvanced.gradle.models.PluginManifest
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File

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

	var redditVancedBackend: Property<String> =
		project.objects.property(String::class.java).convention("https://redditvanced.domain")

	internal val minimumRedditVersion: Property<Int> =
		project.objects.property(Int::class.java)

	/**
	 * The class that's annotated with `@RedditVancedPlugin`
	 */
	internal var pluginClass: Property<String> =
		project.objects.property(String::class.java)

	internal var redditVersionCode: Property<Int> =
		project.objects.property(Int::class.java)

	internal val cacheDir = File(project.gradle.gradleUserHomeDir, "caches/redditvanced")

	internal val apkFile: File
		get() = File(cacheDir, "discord-${redditVersionCode.get()}.apk")

	internal val jarFile: File
		get() = File(cacheDir, "discord-${redditVersionCode.get()}-sources.apk")
}

internal fun ExtensionContainer.getRedditVanced() =
	getByType(RedditVancedExtension::class.java)

internal fun ExtensionContainer.getAndroid() =
	getByName("android") as BaseExtension
