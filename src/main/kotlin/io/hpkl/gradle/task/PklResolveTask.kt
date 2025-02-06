package io.hpkl.gradle.task

import io.hpkl.gradle.cli.CliProjectResolver
import io.hpkl.gradle.spec.ModulesSpec
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import java.io.PrintWriter
import java.nio.file.Path

class PklResolveTask(
    override val spec: ModulesSpec,
    project: Project,
    objects: ObjectFactory,
    providerFactory: ProviderFactory,
) : BasePklTask(spec, project, objects, providerFactory) {

    override fun doRunTask() {
        if (spec.projectDir.isPresent) {
            val projectDirectories = listOf(spec.projectDir.map { Path.of(it.asFile.absolutePath) }.get())

            CliProjectResolver(
                cliBaseOptions,
                projectDirectories,
                PrintWriter(System.out),
                PrintWriter(System.err),
            ).run()
        }
    }
}
