package io.hpkl.gradle.codegen.kotlin

import org.pkl.commons.cli.CliBaseOptions
import org.pkl.core.DataSize
import org.pkl.core.DataSizeUnit
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class KotlinCodeGeneratorOptions(
    /** The characters to use for indenting generated Kotlin code. */
    val indent: String = "  ",

    val durationClass: String = Duration::class.java.name,

    val durationUnitClass: String = DurationUnit::class.java.name,

    val dataSizeClass: String = DataSize::class.java.name,

    val dataSizeUnitClass: String = DataSizeUnit::class.java.name,

    /** Whether to preserve Pkl doc comments by generating corresponding KDoc comments. */
    val generateKdoc: Boolean = false,

    /** Whether to generate config classes for use with Spring Boot. */
    val generateSpringBootConfig: Boolean = false,

    /** Whether to generate classes that implement [java.io.Serializable]. */
    val implementSerializable: Boolean = false,

    val mutableObjects: Boolean = false,

    /**
     * A mapping from Pkl module name prefixes to their replacements.
     *
     * Can be used when the class or package name in the generated source code should be different
     * from the corresponding name derived from the Pkl module declaration .
     */
    val renames: Map<String, String> = emptyMap(),

    val springConfigAnnotation: String = "SpringConfigProperties",

    val generateAnnotationClasses: Boolean = false,

    val setDefaultValues: Boolean = true,

    val baseCliBaseOptions: CliBaseOptions = CliBaseOptions(
        allowedResources = listOf("env:", "prop:", "file:", "modulepath:", "https:", "package:")
            .map { Pattern.compile(it) },
        allowedModules = listOf("repl:", "file:", "modulepath:", "https:", "pkl:", "package:", "projectpackage:")
            .map { Pattern.compile(it) },
    ),

    val durationClassConverter: String? = null,

    val dataSizeConverter: String? = null,
)
