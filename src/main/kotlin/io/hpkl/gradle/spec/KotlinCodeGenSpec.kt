package io.hpkl.gradle.spec

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.pkl.core.DataSize
import org.pkl.core.DataSizeUnit
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

abstract class KotlinCodeGenSpec @Inject constructor(name: String, project: Project, objects: ObjectFactory)
    : CodeGenSpec(name, project, objects) {
    val mutableObjects : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val generateKdoc : Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    val durationClass : Property<String> =
        objects.property(String::class.java)
            .convention(Duration::class.java.canonicalName)

    val dataSizeClass : Property<String> =
        objects.property(String::class.java)
            .convention(DataSize::class.java.canonicalName)

    val durationUnitClass : Property<String> =
        objects.property(String::class.java)
            .convention(DurationUnit::class.java.canonicalName)

    val dataSizeUnitClass : Property<String> =
        objects.property(String::class.java)
            .convention(DataSizeUnit::class.java.canonicalName)

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