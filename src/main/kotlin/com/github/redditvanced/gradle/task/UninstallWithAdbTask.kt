package com.github.redditvanced.gradle.task

import com.github.redditvanced.gradle.AdbUtils
import com.github.redditvanced.gradle.ProjectType
import com.github.redditvanced.gradle.getRedditVanced
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskAction

abstract class UninstallWithAdbTask : DefaultTask() {
	@TaskAction
	fun deployWithAdb() {
		val extension = project.extensions.getRedditVanced()

		val make = project.tasks.getByName("make") as AbstractCopyTask
		val buildFile = make.outputs.files.singleFile

		val device = AdbUtils.getTargetDevice(project)

		val path = extension.projectType.get().getRemotePath(buildFile)
		device.execute("rm", path)

		if (extension.projectType.get() != ProjectType.INJECTOR)
			AdbUtils.launchReddit(device, false)

		logger.lifecycle("Deleted $path from ${device.serial}")
	}
}
