package io.hpkl.gradle.task

import io.hpkl.gradle.spec.BasePklSpec
import io.hpkl.gradle.utils.PluginUtils
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.pkl.commons.cli.CliBaseOptions
import org.pkl.core.evaluatorSettings.Color
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

abstract class BasePklTask(
    open val spec: BasePklSpec,
    val project: Project,
    val objects: ObjectFactory,
    val providerFactory: ProviderFactory,
) {

    fun getParsedSettingsModule(): Provider<Any?> {
        return spec.settingsModule.map(PluginUtils::parseModuleNotation)
    }

    fun getSettingsModuleFile(): Provider<File?> {
        return getParsedSettingsModule().map {
            if (it is File) {
                it
            } else {
                null
            }!!
        }
    }

    fun getSettingsModuleUri(): Provider<URI?> {
        return getParsedSettingsModule().map {
            if (it is URI) {
                it
            } else {
                null
            }!!
        }
    }

    fun getEvalRootDir(): DirectoryProperty = this.spec.evalRootDir

    fun getEvalRootDirPath(): Provider<String> {
        return getEvalRootDir().map { it.asFile.absolutePath }
    }

    open fun runTask() {
        doRunTask()
    }

    protected abstract fun doRunTask()

    var cachedOptions: CliBaseOptions? = null

    // Must be called during task execution time only.
    protected open val cliBaseOptions: CliBaseOptions
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
                        mapAndGetOrNull(getEvalRootDirPath(), Paths::get),
                        mapAndGetOrNull(spec.settingsModule, PluginUtils::parseModuleNotationToUri),
                        null,
                        spec.evalTimeout.getOrNull(),
                        mapAndGetOrNull(
                            spec.moduleCacheDir,
                        ) { it.asFile.toPath() },
                        if (spec.color.getOrElse(false)) Color.ALWAYS else Color.NEVER,
                        spec.noCache.getOrElse(false),
                        omitProjectSettings = false,
                        noProject = false,
                        testMode = false,
                        testPort = spec.testPort.getOrElse(-1),
                        caCertificates = Collections.emptyList(),
                        httpProxy = spec.httpProxy.getOrNull(),
                        httpNoProxy = spec.httpNoProxy.getOrElse(emptyList()),
                        externalModuleReaders = emptyMap(),
                        externalResourceReaders = emptyMap(),
                    )
            }
            return cachedOptions!!
        }

    protected open fun getSourceModulesAsUris(): List<URI> {
        return Collections.emptyList()
    }

    protected open fun parseModulePath(): List<Path> {
        return spec.modulePath.files.stream().map(File::toPath).collect(Collectors.toList())
    }

    protected open fun patternsFromStrings(patterns: List<String>): List<Pattern> {
        return patterns.stream().map(Pattern::compile).collect(Collectors.toList())
    }

    protected open fun <T, U> mapAndGetOrNull(provider: Provider<T>, f: (T) -> U): U? {
        return provider.orNull?.let { return f(it) }
    }
}
