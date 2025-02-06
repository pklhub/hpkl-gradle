package io.hpkl.gradle.task

import io.hpkl.gradle.PklPojoExtension
import io.hpkl.gradle.spec.ModulesSpec
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class ResolveTask @Inject constructor(
    private val extension: PklPojoExtension,
    private val objects: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : DefaultTask() {

    fun specs(): List<ModulesSpec> {
        return extension.javaCodeGenerators.map { it } + extension.kotlinCodeGenerators.map { it }
    }

    @InputFiles
    open fun getInputFiles(): Provider<List<File>> {
        return specs().map { s ->
            s.projectDir.file("PklProject").map { it.asFile }
        }.fold<Provider<File>, Provider<List<File>>>(providerFactory.provider { listOf() }) { acc, next ->
            acc.zip(
                next,
                { a, b -> a + listOf(b) },
            )
        }.orElse(providerFactory.provider { listOf() })
    }

    @TaskAction
    fun runTask() {
        specs().forEach { spec ->
            PklResolveTask(
                spec,
                this.project,
                objects,
                providerFactory,
            ).runTask()
        }
    }
}
