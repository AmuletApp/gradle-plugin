package com.github.redditvanced.gradle.task

import com.github.redditvanced.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import se.vidstige.jadb.RemoteFile

abstract class DeployWithAdbTask : DefaultTask() {
	@get:Input
	@set:Option(option = "wait-for-debugger", description = "Enables debugging flag when starting the discord activity")
	var waitForDebugger: Boolean = false

	@TaskAction
	fun deployWithAdb() {
		val extension = project.extensions.getRedditVanced()

		val make = project.tasks.getByName("make") as AbstractCopyTask
		val buildFile = make.outputs.files.singleFile

		val device = AdbUtils.getTargetDevice(project)

		val path = extension.projectType.get().getRemotePath(buildFile)
		device.push(buildFile, RemoteFile(path))

		if (extension.projectType.get() != ProjectType.INJECTOR)
			AdbUtils.launchReddit(device, waitForDebugger)

		logger.lifecycle("Deployed ${buildFile.name} to ${device.serial}")
	}
}
