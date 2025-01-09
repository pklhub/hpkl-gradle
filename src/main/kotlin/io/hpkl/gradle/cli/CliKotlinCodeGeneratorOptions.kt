package io.hpkl.gradle.cli

import io.hpkl.gradle.codegen.kotlin.KotlinCodeGeneratorOptions
import org.pkl.commons.cli.CliBaseOptions
import java.nio.file.Path

class CliKotlinCodeGeneratorOptions(
    /** Base options shared between CLI commands. */
    val base: CliBaseOptions,

    /** The directory where generated source code is placed. */
    val outputDir: Path,

    /** The characters to use for indenting generated source code. */
    val indent: String = "  ",

    val durationClass: String,

    val dataSizeClass: String,

    val durationUnitClass: String,

    val dataSizeUnitClass: String,

    val mutableObjects: Boolean = false,

    /** Whether to generate Javadoc based on doc comments for Pkl modules, classes, and properties. */
    val generateKdoc: Boolean = false,

    /** Whether to generate config classes for use with Spring Boot. */
    val generateSpringBootConfig: Boolean = false,

    val springConfigAnnotation: String = "SpringConfigProperties",

    /** Whether to make generated classes implement [java.io.Serializable] */
    val implementSerializable: Boolean = false,

    /**
     * A rename mapping for class names.
     *
     * When you need to have Java class or package names different from the default names derived from
     * Pkl module names, you can define a rename mapping, where the key is a prefix of the original
     * Pkl module name, and the value is the desired replacement.
     */
    val renames: Map<String, String> = emptyMap(),

    val generateAnnotationClasses: Boolean = false,

    val setDefaultValues: Boolean = false,

    val durationClassConverter: String? = null,

    val dataSizeConverter: String? = null,
) {
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("deprecated without replacement")
    fun toKotlinCodegenOptions() = toKotlinCodeGeneratorOptions()

    internal fun toKotlinCodeGeneratorOptions() =
        KotlinCodeGeneratorOptions(
            indent = indent,
            durationClass = durationClass,
            durationUnitClass = durationUnitClass,
            dataSizeClass = dataSizeClass,
            dataSizeUnitClass = dataSizeUnitClass,
            generateKdoc = generateKdoc,
            mutableObjects = mutableObjects,
            generateSpringBootConfig = generateSpringBootConfig,
            springConfigAnnotation = springConfigAnnotation,
            implementSerializable = implementSerializable,
            renames = renames,
            generateAnnotationClasses = generateAnnotationClasses,
            setDefaultValues = setDefaultValues,
            baseCliBaseOptions = base,
            durationClassConverter = durationClassConverter,
            dataSizeConverter = dataSizeConverter,
        )
}
