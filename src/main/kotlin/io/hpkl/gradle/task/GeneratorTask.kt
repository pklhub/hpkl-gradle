package io.hpkl.gradle.task

import io.hpkl.gradle.PklPojoExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class GeneratorTask @Inject constructor(
    private val extension: PklPojoExtension,
    private val objects: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : DefaultTask() {

    @OutputDirectories
    open fun getJavaOutputDirs(): List<Directory> {
        return this.extension.javaCodeGenerators.map { spec ->
            spec.outputDir.get().dir("java")
        }.distinct()
    }

    @OutputDirectories
    open fun getKotlinOutputDirs(): List<Directory> {
        return this.extension.kotlinCodeGenerators.map { spec ->
            spec.outputDir.get().dir("kotlin")
        }.distinct()
    }

    @InputFiles
    open fun getInputFiles(): List<File> {
        return this.extension.javaCodeGenerators.flatMap { spec ->
            spec.transitiveModules.files + spec.getSourceModuleFiles().files
        }.distinct() + this.extension.kotlinCodeGenerators.flatMap { spec ->
            spec.transitiveModules.files + spec.getSourceModuleFiles().files
        }.distinct()
    }

    @TaskAction
    fun runTask() {
        outputs.previousOutputFiles.forEach(File::delete)
        this.extension.javaCodeGenerators.forEach { spec ->
            val task = JavaCodeGenTask(spec, this.project, objects, providerFactory)
            task.runTask()

            val sourceSets: SourceSetContainer? = project.extensions.findByType(
                SourceSetContainer::class.java,
            )
            val mainSourceSet = sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)

            if (mainSourceSet != null) {
                val srcDirs = mainSourceSet.java.srcDirs.toMutableSet()
                srcDirs.addAll(getJavaOutputDirs().map { it.asFile })
                mainSourceSet.java.setSrcDirs(srcDirs)
            }
        }
        this.extension.kotlinCodeGenerators.forEach { spec ->
            val task = KotlinCodeGenTask(spec, this.project, objects, providerFactory)
            task.runTask()

            val sourceSets: SourceSetContainer? = project.extensions.findByType(
                SourceSetContainer::class.java,
            )
            val mainSourceSet = sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)

            if (mainSourceSet != null) {
                val srcDirs = mainSourceSet.allJava.srcDirs.toMutableSet()
                srcDirs.addAll(getKotlinOutputDirs().map { it.asFile })
                mainSourceSet.allJava.setSrcDirs(srcDirs)
            }
        }
    }
}
