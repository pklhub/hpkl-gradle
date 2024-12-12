package io.hpkl.gradle.codegen

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
     * A mapping from Pkl module name prefixes to their replacements.
     *
     * Can be used when the class or package name in the generated source code should be different
     * from the corresponding name derived from the Pkl module declaration .
     */
    val renames: Map<String, String> = emptyMap(),

    val generateAnnotationClasses: Boolean = false
)