package io.hpkl.gradle.codegen

import org.pkl.commons.cli.CliBaseOptions

data class JavaCodeGeneratorOptions(
    /** The characters to use for indenting generated Java code. */
    val indent: String = "  ",

    val durationClass : String?,

    val durationUnitClass : String?,

    val dataSizeClass : String?,

    val dataSizeUnitClass : String?,

    val pairClass : String?,

    /**
     * Whether to generate public getter methods and protected final fields instead of public final
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

    val namedAnnotation: String? = null,

    val nonNullAnnotation: String? = null,

    /** Whether to make generated classes implement [java.io.Serializable] */
    val implementSerializable: Boolean = false,

    /**
     * A mapping from Pkl module name prefixes to their replacements.
     *
     * Can be used when the class or package name in the generated source code should be different
     * from the corresponding name derived from the Pkl module declaration .
     */
    val renames: Map<String, String> = emptyMap(),

    val generateAnnotationClasses: Boolean = false,

    val setDefaultValues: Boolean = true,

    val baseCliBaseOptions: CliBaseOptions,

    val durationClassConverter: String?,

    val dataSizeConverter: String?
)