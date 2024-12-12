package io.hpkl.gradle.cli

import io.hpkl.gradle.codegen.JavaCodeGeneratorOptions
import org.pkl.commons.cli.CliBaseOptions
import java.nio.file.Path

data class CliJavaCodeGeneratorOptions (
    /** Base options shared between CLI commands. */
    val base: CliBaseOptions,

    /** The directory where generated source code is placed. */
    val outputDir: Path,

    /** The characters to use for indenting generated source code. */
    val indent: String = "  ",

    val durationClass : String?,

    val dataSizeClass : String?,

    val durationUnitClass : String?,

    val dataSizeUnitClass : String?,

    val pairClass : String?,

    /**
     * Whether to generate public getter methods and private/protected fields instead of public
     * fields.
     */
    val generateGetters: Boolean = false,

    val generateSetters: Boolean = false,

    val generateEmptyConstructor: Boolean = false,

    /** Whether to generate Javadoc based on doc comments for Pkl modules, classes, and properties. */
    val generateJavadoc: Boolean = false,

    /** Whether to generate config classes for use with Spring Boot. */
    val generateSpringBootConfig: Boolean = false,

    val springConfigAnnotation: String = "SpringConfigProperties",

    /**
     * Fully qualified name of the annotation to use on constructor parameters. If this options is not
     * set, [org.pkl.config.java.mapper.Named] will be used.
     */
    val namedAnnotation: String? = null,

    /**
     * Fully qualified name of the annotation to use on non-null properties. If this option is not
     * set, [org.pkl.config.java.mapper.NonNull] will be used.
     */
    val nonNullAnnotation: String? = null,

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
) {
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("deprecated without replacement")
    fun toJavaCodegenOptions() = toJavaCodeGeneratorOptions()

    internal fun toJavaCodeGeneratorOptions() =
        JavaCodeGeneratorOptions(
            indent,
            durationClass,
            durationUnitClass,
            dataSizeClass,
            dataSizeUnitClass,
            pairClass,
            generateGetters,
            generateSetters,
            generateEmptyConstructor,
            generateJavadoc,
            generateSpringBootConfig,
            springConfigAnnotation,
            namedAnnotation,
            nonNullAnnotation,
            implementSerializable,
            renames,
            generateAnnotationClasses
        )
}