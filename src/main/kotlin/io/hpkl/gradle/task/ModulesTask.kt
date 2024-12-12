package io.hpkl.gradle.task


import io.hpkl.gradle.spec.ModulesSpec
import io.hpkl.gradle.utils.PluginUtils
import io.hpkl.gradle.utils.PluginUtils.parseModuleNotation
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.pkl.commons.cli.CliBaseOptions
import org.pkl.core.evaluatorSettings.Color
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


abstract class ModulesTask(
    override val spec: ModulesSpec,
    project: Project,
    objects: ObjectFactory,
    providerFactory: ProviderFactory
) : BasePklTask(spec, project, objects, providerFactory) {

    private val parsedSourceModulesCache: MutableMap<List<Any>, Pair<List<File>, List<URI>>> = mutableMapOf()

    val parsedSourceModules: Provider<Pair<List<File>, List<URI>>>
        // Used for input tracking purposes only.
        get() = spec.sourceModules.map {
            parsedSourceModulesCache.computeIfAbsent(it) { modules ->
                this.splitFilesAndUris(modules)
            }
        }

    val sourceModuleFiles: FileCollection
        // We use @InputFiles and FileCollection here to ensure that file contents are tracked.
        get() {
            return project.files(parsedSourceModules.map { it.first })
        }

    val sourceModuleUris: Provider<List<URI>>
        // We use @Input and just a list value because we can only track the URIs themselves
        get() {
            return parsedSourceModules.map { it.second }
        }

    /**
     * Returns the sourceModules property as a list of URIs.
     *
     *
     * This method ensures that the order of source modules in the sourceModules property is
     * preserved all the way to the CLI API invocation.
     */

    override fun getSourceModulesAsUris(): List<URI> {
        return spec.sourceModules.get().stream()
            .map { PluginUtils.parseModuleNotationToUri(it) }
            .collect(Collectors.toList())
    }

    val projectDirPath: Provider<String>
        get() {
            return spec.projectDir.map { it.asFile.absolutePath }
        }


    /**
     * A source module can be either a file or a URI. Files can be tracked, so this method splits a
     * collection of module notations (which can be strings, URIs, URLs, Files or Paths) into a list
     * of files (for content-based tracking) and URIs (for simple value-based tracking). These lists
     * are then exposed as separate read-only properties to make Gradle track them as proper inputs.
     */
    private fun splitFilesAndUris(modules: List<Any>): Pair<List<File>, List<URI>> {
        val files: ArrayList<File> = ArrayList()
        val uris: ArrayList<URI> = ArrayList()
        for (m: Any in modules) {
            val parsed: Any = parseModuleNotation(m)
            if (parsed is File) {
                files.add(parsed)
            } else if (parsed is URI) {
                uris.add(parsed)
            }
        }
        return Pair(files, uris)
    }

    override fun runTask() {
        if (cliBaseOptions.normalizedSourceModules.isEmpty()) {
            throw InvalidUserDataException("No source modules specified.")
        }
        doRunTask()
    }

    override val cliBaseOptions: CliBaseOptions
        get() {
            if (cachedOptions == null) {
                cachedOptions =
                    CliBaseOptions(
                        getSourceModulesAsUris(),
                        patternsFromStrings(spec.allowedModules.get()),
                        patternsFromStrings(spec.allowedResources.get()),
                        spec.environmentVariables.get(),
                        spec.externalProperties.get(),
                        parseModulePath(),
                        project.projectDir.toPath(),
                        mapAndGetOrNull<String, Path>(getEvalRootDirPath(), Paths::get),
                        mapAndGetOrNull<Any, URI>(spec.settingsModule) {
                            PluginUtils.parseModuleNotationToUri(it)
                        },
                        if (spec.projectDir.isPresent) spec.projectDir.get().asFile.toPath() else null,
                        spec.evalTimeout.getOrNull(),
                        mapAndGetOrNull<Directory, Path>(spec.moduleCacheDir) { it1: Directory -> it1.asFile.toPath() },
                        if (spec.color.getOrElse(false)) Color.ALWAYS else Color.NEVER,
                        spec.noCache.getOrElse(false),
                        spec.omitProjectSettings.getOrElse(false),
                        spec.noProject.getOrElse(false),
                        false,
                        spec.testPort.getOrElse(-1),
                        emptyList(),
                        null,
                        listOf(),
                        emptyMap(),
                        emptyMap()
                    )
            }

            return cachedOptions!!
        }
}