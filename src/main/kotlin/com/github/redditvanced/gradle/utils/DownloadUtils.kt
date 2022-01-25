/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// Credit to 6pak, https://github.com/Aliucord/gradle/commit/8ca3becd77b6d7676cf8287feb56027b19d07de7

package com.github.redditvanced.gradle.utils

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.service.ServiceRegistry
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun createProgressLogger(project: Project, loggerCategory: String): ProgressLogger {
	return createProgressLogger((project as ProjectInternal).services, loggerCategory)
}

fun createProgressLogger(services: ServiceRegistry, loggerCategory: String): ProgressLogger {
	val progressLoggerFactory = services.get(ProgressLoggerFactory::class.java)
	return progressLoggerFactory.newOperation(loggerCategory).also { it.description = loggerCategory }
}

fun downloadFromStream(stream: InputStream, size: Long, output: File, progressLogger: ProgressLogger) {
	val tempFile = File.createTempFile(output.name, ".part", output.parentFile)
	progressLogger.started()

	var finished = false
	try {
		var processedBytes: Long = 0
		FileOutputStream(tempFile).use { os ->
			val buf = ByteArray(1024 * 10)
			var read: Int
			while (stream.read(buf).also { read = it } >= 0) {
				os.write(buf, 0, read)
				processedBytes += read
				progressLogger.progress("Downloading discord apk ${toLengthText(processedBytes)}/$size")
			}
			os.flush()
			finished = true
		}
	} catch (t: Throwable) {
		tempFile.delete()
		progressLogger.completed(t.message, true)

		throw t
	} finally {
		if (finished) tempFile.renameTo(output)
		else tempFile.delete()
		stream.close()
		progressLogger.completed()
	}
}

private fun toLengthText(bytes: Long): String {
	return if (bytes < 1024) {
		"$bytes B"
	} else if (bytes < 1024 * 1024) {
		(bytes / 1024).toString() + " KB"
	} else if (bytes < 1024 * 1024 * 1024) {
		String.format("%.2f MB", bytes / (1024.0 * 1024.0))
	} else {
		String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
	}
}
