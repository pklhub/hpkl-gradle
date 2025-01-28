package io.hpkl.gradle.task

import io.hpkl.gradle.cli.CliKotlinCodeGenerator
import io.hpkl.gradle.cli.CliKotlinCodeGeneratorOptions
import io.hpkl.gradle.spec.KotlinCodeGenSpec
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

class KotlinCodeGenTask(
    override val spec: KotlinCodeGenSpec,
    project: Project,
    objects: ObjectFactory,
    providerFactory: ProviderFactory,
) : CodeGenTask(spec, project, objects, providerFactory) {
    override fun doRunTask() {
        CliKotlinCodeGenerator(
            CliKotlinCodeGeneratorOptions(
                cliBaseOptions,
                project.file(outputDir).toPath(),
                spec.indent.get(),
                spec.durationClass.get(),
                spec.dataSizeClass.get(),
                spec.durationUnitClass.get(),
                spec.dataSizeUnitClass.get(),
                spec.mutableObjects.get(),
                spec.generateKdoc.get(),
                spec.generateSpringBootConfig.get(),
                spec.springConfigAnnotation.get(),
                spec.implementSerializable.get(),
                spec.renames.get(),
                spec.generateAnnotationClasses.get(),
                spec.setDefaultValues.get(),
                spec.durationClassConverter.orNull,
                spec.dataSizeConverter.orNull,
            ),
            project.logger,
        )
            .run()
    }
}
