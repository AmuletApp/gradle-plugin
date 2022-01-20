package com.github.redditvanced.gradle

import org.gradle.api.Project
import se.vidstige.jadb.*
import java.nio.charset.StandardCharsets

object AdbUtils {
	fun getTargetDevice(project: Project): JadbDevice {
		val android = project.extensions.getAndroid()

		AdbServerLauncher(Subprocess(), android.adbExecutable.absolutePath).launch()
		val jadbConnection = JadbConnection()
		val devices = jadbConnection.devices.filter {
			try {
				it.state == JadbDevice.State.Device
			} catch (e: JadbException) {
				false
			}
		}

		require(devices.size == 1) { "Only one ADB device should be connected, but ${devices.size} were!" }

		return devices[0]
	}

	fun launchReddit(device: JadbDevice, waitForDebugger: Boolean) {
		val args = mutableListOf("start", "-S", "-n", "com.github.redditvanced/com.reddit.frontpage.main.MainActivity")
		if (waitForDebugger)
			args.add("-D")

		val response = device
			.executeShell("am", *args.toTypedArray())
			.readAllBytes()
			.toString(StandardCharsets.UTF_8)

		if (response.contains("Error"))
			throw Error(response)
	}
}
