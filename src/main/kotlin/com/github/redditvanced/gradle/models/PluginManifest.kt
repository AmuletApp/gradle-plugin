package com.github.redditvanced.gradle.models

data class PluginManifest(
	val name: String,
	val version: String,
	val pluginClass: String,
	val changelog: String = "",
	val description: String = "",
	val authors: List<Author> = emptyList(),
	val loadResources: Boolean = false,
	val requiresRestart: Boolean = false,
) {
	data class Author(
		val name: String,
		val discordId: Long?,
		val redditUsername: String?,
	)
}
