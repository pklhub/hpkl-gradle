package io.hpkl.gradle.spec

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.pkl.core.DataSize
import org.pkl.core.DataSizeUnit
import org.pkl.core.Pair
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

abstract class JavaCodeGenSpec
    @Inject constructor(name: String, project: Project, objects: ObjectFactory)
    : CodeGenSpec(name, project, objects) {
    val generateGetters : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val generateSetters : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val generateEmptyConstructor: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val generateJavadoc : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val namedAnnotation : Property<String> =
        objects.property(String::class.java)

    val nonNullAnnotation : Property<String> =
        objects.property(String::class.java)

    val durationClass : Property<String> =
        objects.property(String::class.java)
            .convention(Duration::class.java.canonicalName)

    val dataSizeClass : Property<String> =
        objects.property(String::class.java)
            .convention(DataSize::class.java.canonicalName)

    val durationUnitClass : Property<String> =
        objects.property(String::class.java)
            .convention(ChronoUnit::class.java.canonicalName)

    val dataSizeUnitClass : Property<String> =
        objects.property(String::class.java)
            .convention(DataSizeUnit::class.java.canonicalName)

    val pairClass : Property<String> =
        objects.property(String::class.java)
            .convention(Pair::class.java.canonicalName)

    val generateAnnotationClasses : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val springConfigAnnotation: Property<String> =
        objects.property(String::class.java)
            .convention("SpringConfigProperties")

    val setDefaultValues: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val durationClassConverter: Property<String> =
        objects.property(String::class.java)

    val dataSizeConverter: Property<String> =
        objects.property(String::class.java)
}