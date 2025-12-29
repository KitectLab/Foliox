package io.github.kitectlab.foliox.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.ByteArrayOutputStream

internal fun gitLibraryVersionProvider(project: Project): Provider<String> {
    return project.provider {
        if (!BuildConst.isRelease) {
            return@provider BuildConst.snapshotVersion
        }
        try {
            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream() // Capture error output too

            val execResult = project.providers.exec {
                commandLine(
                    "git",
                    "describe",
                    "--tags",
                    "--abbrev=0",
                    "--dirty=-SNAPSHOT"
                ) // Add --dirty for uncommitted changes
                workingDir = project.rootDir
                isIgnoreExitValue = true // We'll check the exit value manually
                standardOutput = outputStream
                errorOutput = errorStream // Capture stderr
            }.result.get()

            if (execResult.exitValue == 0) {
                val tag = outputStream.toString().trim()
                project.logger.lifecycle("GitVersionPlugin: Read git tag successfully: $tag")
                tag.ifEmpty { BuildConst.snapshotVersion }
            } else {
                val errorMsg = errorStream.toString().trim()
                project.logger.warn("GitVersionPlugin: Could not determine git tag. Exit code: ${execResult.exitValue}. Error: $errorMsg. Using fallback.")
                BuildConst.snapshotVersion
            }
        } catch (e: Exception) {
            project.logger.error(
                "GitVersionPlugin: Exception while trying to read git tag. Using fallback.",
                e
            )
            BuildConst.snapshotVersion
        }
    }
}