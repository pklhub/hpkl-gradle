package io.hpkl.gradle

import io.hpkl.gradle.spec.JavaCodeGenSpec
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class PklPojoExtension @Inject constructor(objects: ObjectFactory) {

    var javaCodeGenerators: NamedDomainObjectContainer<JavaCodeGenSpec>
        = objects.domainObjectContainer(JavaCodeGenSpec::class.java)

    fun javaCodeGenerators(
        action: Action<NamedDomainObjectContainer<JavaCodeGenSpec>>
    ) {
        action.execute(javaCodeGenerators)
    }
}