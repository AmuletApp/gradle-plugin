package com.github.redditvanced.gradle

import com.github.redditvanced.gradle.models.RedditAPK
import com.github.redditvanced.gradle.models.RemoteData
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import org.gradle.api.Project
import org.gradle.internal.impldep.com.google.gson.Gson
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipFile

private const val DATA_URL = "https://raw.githubusercontent.com/RedditVanced/RedditVanced/builds/data.json"
private const val APK_URL = "https://redditvanced.app/reddit/%s"

fun configureRedditConfiguration(project: Project) {
	val extension = project.extensions.getRedditVanced()

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

			val tmpApkFile = File(System.getProperty("java.io.tmpdir"), "reddit-${version}-tmp.xapk")
			extension.cacheDir.mkdirs()

			if (!extension.apkFile.exists()) {
				project.logger.lifecycle("Downloading Reddit APK")
				val url = URL(APK_URL.format(version))
				val reader = BufferedReader(InputStreamReader(url.openStream()))
				val data = gson.fromJson(reader, RedditAPK::class.java)

				val apkUrl = URL(data.downloadUrl)
				apkUrl.download(tmpApkFile, createProgressLogger(project, "Download Reddit APK"))

				project.logger.lifecycle("Extracting main apk from xapk")
				val zip = ZipFile(tmpApkFile)
				val stream = zip.getInputStream(zip.getEntry("com.reddit.frontpage.apk"))

				extension.apkFile.outputStream().use { outStream ->
					stream.copyTo(outStream)
					outStream.close()
				}
				stream.close()
				tmpApkFile.delete()
			}

			if (!extension.jarFile.exists()) {
				project.logger.lifecycle("Converting APK to jar")

				val reader = MultiDexFileReader.open(Files.readAllBytes(extension.jarFile.toPath()))
				Dex2jar.from(reader)
					.skipDebug(false)
					.topoLogicalSort()
					.noCode(true)
					.to(extension.jarFile.toPath())
			}

			project.dependencies.add("compileOnly", project.files(extension.jarFile))
		}
	}
}
