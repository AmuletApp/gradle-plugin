package com.github.redditvanced.gradle

import java.io.File

enum class ProjectType {
	/* Internals, do not use */
	CORE,

	/* Internals, do not use */
	INJECTOR,

	/* A regular RedditVanced plugin */
	PLUGIN;

	fun getRemotePath(buildFile: File): String {
		return "/storage/emulated/0/RedditVanced/" + when (this) {
			PLUGIN -> "plugins/${buildFile.name}"
			INJECTOR -> "build/injector.dex"
			CORE -> "build/core.zip"
		}
	}
}
