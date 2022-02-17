package com.github.redditvanced.gradle.task

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.redditvanced.gradle.getRedditVanced
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

abstract class RequestPublishPluginTask : DefaultTask() {
	@TaskAction
	fun publishPlugin() {
		val extension = project.extensions.getRedditVanced()
		val baseUrl = extension.backendUrl.get()

		if (gitHasLocalCommit()) {
			logger.lifecycle("Your git repository has local commits! Are you sure you want to continue?")
			ynConfirm()
		}

		if (gitHasChanges()) {
			logger.lifecycle("Your git repository has local changes! Are you sure you want to continue?")
			ynConfirm()
		}

		if (getRemoteURL("upstream").isNotEmpty()) {
			logger.error("You cannot publish a plugin from a fork!")
			exitProcess(1)
		}

		val origin = getRemoteURL("origin")
		if (origin.isEmpty()) {
			logger.error("No remote origin detected! Please publish this repository to GitHub!")
			exitProcess(1)
		}

		val originMatch = "^(?:git@|https://)github.com[:/](.*).git$"
			.toRegex()
			.find(origin)
			?.groupValues
			?.first()
			?.split('/')

		if (originMatch == null || originMatch.size != 2) {
			logger.error("Could not determine repository from origin $origin. Expecting a format of https://github.com/xxxxx/xxxxx.git")
			exitProcess(1)
		}

		val (owner, repo) = originMatch
		val (_, _, result) = "$baseUrl/publish/$owner/$repo?plugin=${project.name}&targetCommit=${getGitHash()}".httpPost().response()

		when (result) {
			is Result.Failure ->
				throw result.getException()
			is Result.Success ->
				logger.info("Successfully requested publish for plugin ${project.name}")
		}
	}

	private fun getRemoteURL(remote: String) =
		exec("git config --get remote.$remote.url")

	private fun getCurrentBranch(): String =
		exec("git name-rev --name-only HEAD")

	private fun gitHasLocalCommit(): Boolean =
		exec("git log origin/${getCurrentBranch()}..HEAD").isNotEmpty()

	private fun gitHasChanges(): Boolean =
		exec("git status -s").isNotEmpty()

	private fun getGitHash(): String =
		exec("git rev-parse HEAD")

	private fun exec(command: String): String {
		val proc = ProcessBuilder(command)
			.directory(project.rootDir)
			.redirectOutput(ProcessBuilder.Redirect.PIPE)
			.redirectError(ProcessBuilder.Redirect.PIPE)
			.start()
		proc.waitFor(5, TimeUnit.SECONDS)

		if (proc.errorStream.available() != 0)
			throw Error(proc.errorStream.bufferedReader().readText().trim())

		return proc.inputStream.bufferedReader().readText().trim()
	}

	private fun ynConfirm(): Boolean {
		val input = System.console().readLine() ?: ""
		return if (input[0].toLowerCase() == 'y') true
		else {
			project.logger.error("Aborting...")
			exitProcess(1)
		}
	}
}
