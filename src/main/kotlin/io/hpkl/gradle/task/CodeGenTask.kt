package io.hpkl.gradle.task

import io.hpkl.gradle.spec.CodeGenSpec
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory


abstract class CodeGenTask(
    override val spec: CodeGenSpec,
    project: Project,
    objects: ObjectFactory,
    providerFactory: ProviderFactory
) : ModulesTask(spec, project, objects, providerFactory) {
    val outputDir: DirectoryProperty
        get() = this.spec.outputDir
}