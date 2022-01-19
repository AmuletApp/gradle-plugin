package com.github.redditvanced.gradle

import com.github.redditvanced.gradle.models.PluginManifest
import com.github.redditvanced.gradle.models.RemoteData
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.internal.impldep.com.google.gson.Gson
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipFile

private const val DATA_URL = "https://raw.githubusercontent.com/RedditVanced/RedditVanced/builds/data.json"
private const val APK_URL = "https://redditvanced.app/reddit/%s"

abstract class RedditVancedPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.extensions.create("redditVanced", RedditVancedExtension::class.java, project)

		val cacheDir = File(project.gradle.gradleUserHomeDir, "caches/redditvanced")
		project.configurations.register("redditApk") {
			it.isTransitive = false
		}
		project.afterEvaluate {
			val dependencies = project.configurations.getByName("redditApk").dependencies
			val gson = Gson()

			dependencies.forEach {
				val version = when (it.version) {
					"SNAPSHOT" -> {
						project.logger.lifecycle("Fetching core's reddit version")
						val reader = BufferedReader(InputStreamReader(URL(DATA_URL).openStream()))
						val data = gson.fromJson(reader, RemoteData::class.java)

						project.logger.lifecycle("Fetched Reddit version: ${data.latestRedditVersionName} (${data.latestRedditVersionCode})")
						data.latestRedditVersionCode
					}
					else -> it.version
				}

				val tmpApkFile = File(System.getProperty("java.io.tmpdir"), "reddit-${version}-tmp.jar")
				val apkFile = File(cacheDir, "reddit-${version}.apk")
				val jarFile = File(cacheDir, "reddit-${version}.jar")
				cacheDir.mkdirs()

				if (!apkFile.exists()) {
					project.logger.lifecycle("Downloading Reddit APK")
					val url = URL(APK_URL.format(version))
					val reader = BufferedReader(InputStreamReader(url.openStream()))
					val data = gson.fromJson(reader, RedditAPK::class.java)

					val apkUrl = URL(data.downloadUrl)
					apkUrl.download(tmpApkFile, createProgressLogger(project, "Download Reddit APK"))

					project.logger.lifecycle("Extracting main apk from xapk")
					val zip = ZipFile(tmpApkFile)
					val stream = zip.getInputStream(zip.getEntry("com.reddit.frontpage.apk"))

					apkFile.outputStream().use { outStream ->
						stream.copyTo(outStream)
						outStream.close()
					}
					stream.close()
					tmpApkFile.delete()
				}

				if (!jarFile.exists()) {
					project.logger.lifecycle("Converting APK to jar")

					val reader = MultiDexFileReader.open(Files.readAllBytes(jarFile.toPath()))
					Dex2jar.from(reader)
						.skipDebug(false)
						.topoLogicalSort()
						.noCode(true)
						.to(jarFile) // TODO: kotlin pls stop
				}

				project.dependencies.add("compileOnly", project.files(jarFile))
			}
		}
	}

	private data class RedditAPK(
		val versionCode: String,
		val versionName: String,
		val size: Int,
		val downloadUrl: String,
	)

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
