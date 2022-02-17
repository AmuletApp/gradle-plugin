package com.github.redditvanced.gradle

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.github.redditvanced.gradle.models.AccountCredentials
import com.github.redditvanced.gradle.models.RemoteData
import com.github.redditvanced.gradle.utils.createProgressLogger
import com.github.redditvanced.gradle.utils.downloadFromStream
import com.github.theapache64.gpa.api.Play
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.nio.file.Files

private const val DATA_URL = "https://raw.githubusercontent.com/RedditVanced/RedditVanced/builds/data.json"

fun configureRedditConfiguration(project: Project) {
	val extension = project.extensions.getRedditVanced()

	project.configurations.register("redditApk") {
		it.isTransitive = false
	}
	project.afterEvaluate {
		val dependencies = project.configurations.getByName("redditApk").dependencies
		if (dependencies.size == 0) return@afterEvaluate
		require(dependencies.size == 1) {
			"Only one Reddit APK dependency is allowed per project but ${dependencies.size} were present!"
		}

		val dependency = dependencies.single()

		val version = when (dependency.version) {
			"SNAPSHOT" -> {
				project.logger.lifecycle("Fetching core's reddit version")
				val (_, _, result) = DATA_URL.httpGet().responseObject<RemoteData>()
				val data = result.get()

				project.logger.lifecycle("Fetched Reddit version: ${data.latestRedditVersionName} (${data.latestRedditVersionCode})")
				data.latestRedditVersionCode
			}
			else -> dependency.version?.toIntOrNull()
				?: throw GradleException("Invalid Reddit APK version code")
		}
		extension.projectRedditVersion.set(version)

		extension.cacheDir.mkdirs()

		if (!extension.apkFile.exists() && !extension.jarFile.exists()) {
			project.logger.lifecycle("Getting Google credentials (dummy account)")
			val credentials = "${extension.backendUrl.get()}/google"
				.httpGet()
				.set("User-Agent", "RedditVanced")
				.responseObject<AccountCredentials>().third.get()

			project.logger.lifecycle("Logging into Google")
			val account = runBlocking {
				Play.login(credentials.username, credentials.password)
			}
			val play = Play.getApi(account)

			project.logger.lifecycle("Retrieving APK details from Google Play")
			val apk = runBlocking {
				play.delivery("com.reddit.frontpage", version, 1)
					?: throw GradleException("Failed to retrieve APK details (null)")
			}

			project.logger.lifecycle("Downloading main part of Reddit APK")
			val stream = try {
				apk.openApp()
			} catch (t: Throwable) {
				t.printStackTrace()
				throw GradleException("Failed to checkout Reddit apk from GPlay!")
			}

			// TODO: check if correct size
			downloadFromStream(stream, apk.appSize, extension.apkFile, createProgressLogger(project, "Download Reddit APK"))
		}

		if (!extension.jarFile.exists()) {
			project.logger.lifecycle("Converting APK to jar")

			val reader = MultiDexFileReader.open(Files.readAllBytes(extension.apkFile.toPath()))
			Dex2jar.from(reader)
				.skipDebug(false)
				.topoLogicalSort()
				.noCode(true)
				.to(extension.jarFile.toPath())
		}

		project.dependencies.add("compileOnly", project.files(extension.jarFile))
	}
}
