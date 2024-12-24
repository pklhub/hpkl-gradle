package io.hpkl.gradle

import io.hpkl.gradle.task.GeneratorTask
import org.gradle.api.Plugin
import org.gradle.api.Project


class PklPojoGradlePlugin : Plugin<Project> {
    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
        val extension = project.extensions
            .create("hpkl", PklPojoExtension::class.java)
        val task = project.tasks.register(
            "generatePklPojo",
            GeneratorTask::class.java,
            extension
        )

        project.tasks.findByName("compileJava")?.dependsOn(task.name)
        project.tasks.findByName("compileKotlin")?.dependsOn(task.name)
    }
}