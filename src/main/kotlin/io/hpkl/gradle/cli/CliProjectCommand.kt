package io.hpkl.gradle.cli

import org.pkl.commons.cli.CliBaseOptions
import org.pkl.commons.cli.CliBaseOptions.Companion.getProjectFile
import org.pkl.commons.cli.CliCommand
import org.pkl.commons.cli.CliException
import org.pkl.core.module.ProjectDependenciesManager.PKL_PROJECT_FILENAME
import java.nio.file.Files
import java.nio.file.Path

abstract class CliProjectCommand(cliOptions: CliBaseOptions, private val projectDirs: List<Path>) :
    CliCommand(cliOptions) {

    protected val normalizedProjectFiles: List<Path> by lazy {
        if (projectDirs.isEmpty()) {
            val projectFile =
                cliOptions.normalizedWorkingDir.getProjectFile(cliOptions.normalizedRootDir)
                    ?: throw CliException(
                        "No project visible to the working directory. Ensure there is a PklProject file in the workspace, or provide an explicit project directory as an argument.",
                    )
            return@lazy listOf(projectFile.normalize())
        }
        projectDirs.map(cliOptions.normalizedWorkingDir::resolve).map { dir ->
            val projectFile = dir.resolve(PKL_PROJECT_FILENAME)
            if (!Files.exists(projectFile)) {
                throw CliException("Directory $dir does not contain a PklProject file.")
            }
            projectFile.normalize()
        }
    }
}
