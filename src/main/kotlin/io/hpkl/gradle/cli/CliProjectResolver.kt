package io.hpkl.gradle.cli

import org.pkl.commons.cli.CliBaseOptions
import org.pkl.core.SecurityManagers
import org.pkl.core.module.ProjectDependenciesManager
import org.pkl.core.packages.PackageResolver
import org.pkl.core.project.ProjectDependenciesResolver
import java.io.Writer
import java.nio.file.Path

class CliProjectResolver(
    baseOptions: CliBaseOptions,
    projectDirs: List<Path>,
    private val consoleWriter: Writer = System.out.writer(),
    private val errWriter: Writer = System.err.writer(),
) : CliProjectCommand(baseOptions, projectDirs) {
    override fun doRun() {
        for (projectFile in normalizedProjectFiles) {
            val project = loadProject(projectFile)
            val packageResolver =
                PackageResolver.getInstance(
                    SecurityManagers.standard(
                        allowedModules,
                        allowedResources,
                        SecurityManagers.defaultTrustLevels,
                        rootDir,
                    ),
                    httpClient,
                    moduleCacheDir,
                )
            val dependencies = ProjectDependenciesResolver(project, packageResolver, errWriter).resolve()
            val depsFile =
                projectFile.parent.resolve(ProjectDependenciesManager.PKL_PROJECT_DEPS_FILENAME).toFile()
            depsFile.outputStream().use { dependencies.writeTo(it) }
            consoleWriter.appendLine(depsFile.toString())
            consoleWriter.flush()
        }
    }
}
