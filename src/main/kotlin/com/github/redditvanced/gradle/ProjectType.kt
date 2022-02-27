package com.github.redditvanced.gradle

import java.io.File

enum class ProjectType {
	/** Internals, do not use */
	CORE,

	/** Internals, do not use */
	INJECTOR,

	/** A RedditVanced plugin */
	PLUGIN,

	/**
	 * A regular project, used only for the Reddit APK dependency
	 * Doesn't apply tasks.
	 */
	REGULAR;

	/**
	 * Get the remote path on a device for a build
	 */
	fun getRemotePath(buildFile: File): String {
		return "/storage/emulated/0/RedditVanced/" + when (this) {
			PLUGIN -> "plugins/${buildFile.name}"
			INJECTOR -> "build/injector.zip"
			CORE -> "build/core.zip"
			REGULAR -> throw IllegalStateException("Regular projects do not have a remote path!")
		}
	}
}
