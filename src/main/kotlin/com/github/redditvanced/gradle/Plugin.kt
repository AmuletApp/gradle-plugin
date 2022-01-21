package com.github.redditvanced.gradle

import com.android.build.gradle.tasks.ProcessLibraryManifest
import com.github.redditvanced.gradle.task.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.compile.AbstractCompile

private const val TASK_GROUP = "reddit vanced"

@Suppress("unused")
abstract class Plugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.extensions.create("redditVanced", RedditVancedExtension::class.java, project)
		configureRedditConfiguration(project)

		val intermediates = project.buildDir.resolve("intermediates")
		val pluginClassFile = intermediates.resolve("pluginClass")

		project.tasks.register("compileResources", CompileResourcesTask::class.java) { task ->
			task.group = TASK_GROUP

			val processManifestTask = project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
			task.dependsOn(processManifestTask)

			val android = project.extensions.getAndroid()
			task.input.set(android.sourceSets.getByName("main").res.srcDirs.single())
			task.manifestFile.set(processManifestTask.manifestOutputFile)

			task.outputFile.set(intermediates.resolve("res.apk"))

			task.doLast { _ ->
				val resApkFile = task.outputFile.asFile.get()

				if (resApkFile.exists()) {
					project.tasks.named("make", AbstractCopyTask::class.java) {
						it.from(project.zipTree(resApkFile)) { copySpec ->
							copySpec.exclude("AndroidManifest.xml")
						}
					}
				}
			}
		}

		project.tasks.register("compileDex", CompileDexTask::class.java) { task ->
			task.group = TASK_GROUP
			task.pluginClassFile.set(pluginClassFile)

			val kotlinTask = project.tasks.getByName("compileDebugKotlin") as AbstractCompile
			task.dependsOn(kotlinTask)
			task.input.from(kotlinTask.destinationDirectory)
		}

		project.tasks.register("genSources", GenSourcesTask::class.java) { task ->
			task.group = TASK_GROUP
		}

		project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) { task ->
			task.group = TASK_GROUP
			task.dependsOn("make")
		}

		project.tasks.register("uninstallWithAdb", UninstallWithAdbTask::class.java) { task ->
			task.group = TASK_GROUP
			task.dependsOn("make")
		}

		project.tasks.register("requestPublishPlugin", RequestPublishPluginTask::class.java) { task ->
			task.group = TASK_GROUP
			task.enabled = project.extensions.getRedditVanced().projectType.get() == ProjectType.PLUGIN
		}
	}
}
