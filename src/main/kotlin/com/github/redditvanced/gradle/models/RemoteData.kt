package com.github.redditvanced.gradle.models

// TODO: add dependency for common instead of duplicating data classes
data class RemoteData(
	/**
	 * All usernames that are blocked from using RedditVanced
	 */
	val blacklistedUsers: List<String> = listOf(),

	/**
	 * Latest core build available
	 */
	val latestCoreVersion: String,

	val latestRedditVersionName: String,
	val latestRedditVersionCode: String,
)
