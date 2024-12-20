package io.hpkl.gradle.spec

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

abstract class CodeGenSpec(name: String, project: Project, objects: ObjectFactory) :
    ModulesSpec(name, project, objects) {

    val outputDir : DirectoryProperty =
        objects.directoryProperty().convention(
            project.layout.buildDirectory.map { it: Directory ->
                it.dir("generated").dir("pkl").dir(name)
            }
        )

    val sourceSet : Property<SourceSet> =
        objects.property(SourceSet::class.java).convention(
            project.providers.provider {
                val sourceSets : SourceSetContainer? = project.extensions.findByType(
                    SourceSetContainer::class.java
                )
                sourceSets?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
            }
        )

    val indent : Property<String> =
        objects.property(String::class.java).convention("  ")

    val generateSpringBootConfig : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val implementSerializable : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val renames: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

    init {
        val localModulePath = project.files()

        val resourceSourceDirectoriesExceptSpecOutput =
            sourceSet.flatMap { sourceSet: SourceSet ->
                outputDir
                .map { specOutputDir: Directory ->
                    sourceSet
                        .resources
                        .sourceDirectories
                        .filter { f: File ->
                            !f.absolutePath
                                .startsWith(
                                    specOutputDir.asFile.absolutePath
                                )
                        }.files
                }
        }
        localModulePath.from(resourceSourceDirectoriesExceptSpecOutput).from(
            sourceSet.map<FileCollection> { obj: SourceSet -> obj.compileClasspath }
        )

        modulePath.from(localModulePath)
    }
}