package io.hpkl.gradle.spec

import io.hpkl.gradle.utils.PluginUtils.parseModuleNotation
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import java.net.URI

abstract class ModulesSpec(name: String, project: Project, objects: ObjectFactory) :
    BasePklSpec(name, project, objects) {

    val sourceModules: ListProperty<Any> =
        objects.listProperty(Any::class.java)

    val projectDir: DirectoryProperty = objects.directoryProperty()

    val omitProjectSettings: Property<Boolean> =
        objects.property(Boolean::class.java)

    val noProject: Property<Boolean> =
        objects.property(Boolean::class.java)

    private val parsedSourceModulesCache: MutableMap<List<Any>, Pair<List<File>, List<URI>>> = mutableMapOf()

    fun getParsedSourceModules(): Provider<Pair<List<File>, List<URI>>> {
        return sourceModules.map {
            parsedSourceModulesCache
            parsedSourceModulesCache.computeIfAbsent(it, this::splitFilesAndUris)
        }
    }

    private fun splitFilesAndUris(modules: List<Any>): Pair<List<File>, List<URI>> {
        val files = ArrayList<File>()
        val uris = ArrayList<URI>()
        for (m in modules) {
            val parsed = parseModuleNotation(m)
            if (parsed is File) {
                files.add(parsed)
            } else if (parsed is URI) {
                uris.add(parsed)
            }
        }
        return Pair(files, uris)
    }

    fun getSourceModuleFiles(): FileCollection {
        return project.files(getParsedSourceModules().map { it.first })
    }
}
