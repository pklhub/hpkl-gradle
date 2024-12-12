package io.hpkl.gradle.task

import io.hpkl.gradle.cli.CliJavaCodeGenerator
import io.hpkl.gradle.cli.CliJavaCodeGeneratorOptions
import io.hpkl.gradle.spec.JavaCodeGenSpec
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory


class JavaCodeGenTask(
    override val spec:JavaCodeGenSpec,
    project: Project,
    objects: ObjectFactory,
    providerFactory: ProviderFactory
) : CodeGenTask(spec, project, objects, providerFactory) {
    override fun doRunTask() {
        CliJavaCodeGenerator(
                CliJavaCodeGeneratorOptions(
                    cliBaseOptions,
                    project.file(outputDir).toPath(),
                    spec.indent.get(),
                    spec.durationClass.orNull,
                    spec.dataSizeClass.orNull,
                    spec.durationUnitClass.orNull,
                    spec.dataSizeUnitClass.orNull,
                    spec.pairClass.orNull,
                    spec.generateGetters.get(),
                    spec.generateSetters.get(),
                    spec.generateEmptyConstructor.get(),
                    spec.generateJavadoc.get(),
                    spec.generateSpringBootConfig.get(),
                    spec.springConfigAnnotation.get(),
                    spec.namedAnnotation.orNull,
                    spec.nonNullAnnotation.orNull,
                    spec.implementSerializable.get(),
                    spec.renames.get(),
                    spec.generateAnnotationClasses.get()
                )
        )
        .run()
    }
}