package io.hpkl.gradle.spec

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.pkl.core.util.IoUtils
import java.net.URI
import java.time.Duration

abstract class BasePklSpec(val name: String, val project: Project, val objects: ObjectFactory) {

    val transitiveModules: ConfigurableFileCollection =
        objects.fileCollection()

    val allowedModules: ListProperty<String> =
        objects.listProperty(String::class.java).convention(
            listOf("repl:", "file:", "modulepath:", "https:", "pkl:", "package:", "projectpackage:"),
        )

    val allowedResources = objects.listProperty(String::class.java).convention(
        listOf("env:", "prop:", "file:", "modulepath:", "https:", "package:"),
    )

    val environmentVariables: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    val externalProperties: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    val modulePath: ConfigurableFileCollection = objects.fileCollection()

    val settingsModule: Property<Any> = objects.property(Any::class.java)

    val evalRootDir: DirectoryProperty =
        objects.directoryProperty().convention(
            project.rootProject.layout.projectDirectory,
        )

    val moduleCacheDir: DirectoryProperty = objects.directoryProperty().let {
        it.set(IoUtils.getDefaultModuleCacheDir().toFile())
        it
    }

    val color: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val noCache: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    // use same type (Duration) as Gradle's `Task.timeout`
    val evalTimeout: Property<Duration> = objects.property(Duration::class.java)

    val testPort: Property<Int> =
        objects.property(Int::class.java).convention(-1)

    val httpProxy: Property<URI> = objects.property(URI::class.java)

    val httpNoProxy: ListProperty<String> =
        objects.listProperty(String::class.java)
            .convention(listOf())
}
