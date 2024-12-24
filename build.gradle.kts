import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.gradle.plugin-publish") version "1.2.1"
    `kotlin-dsl`
    `java-library`
}

group = "io.hpkl.gradle"

val pklVersion = "0.27.1"
val junitVersion = "5.11.3"

dependencies {
    implementation("org.pkl-lang:pkl-core:${pklVersion}")
    implementation("org.pkl-lang:pkl-commons-cli:${pklVersion}")
    implementation("org.pkl-lang:pkl-commons:${pklVersion}")
    implementation("com.palantir.javapoet:javapoet:0.6.0")
    implementation("com.squareup:kotlinpoet:1.6.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")

    testApi("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testApi("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testApi("org.junit.jupiter:junit-jupiter-params:${junitVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.assertj:assertj-core:3.26.3")

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.7.10")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.7.10")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-util:1.7.10")
}

gradlePlugin {
    website = "https://github.com/hpklio"
    vcsUrl = "https://github.com/hpklio/hpkl-gradle"
    plugins {
        create("hpkl") {
            id = "io.hpkl"
            implementationClass = "io.hpkl.gradle.PklPojoGradlePlugin"
            displayName = "HPKL Gradle plugin"
            description = "Gradle plugin for hpkl to generate java pojos"
            tags = listOf("pkl", "generator", "hpkl", "java")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // enable checking of stdlib return types
    systemProperty("org.pkl.testMode", "true")

    reports.named("html") { enabled = true }

    testLogging { exceptionFormat = TestExceptionFormat.FULL }
}

repositories {
    mavenCentral()
}

publishing {
    repositories {
        mavenLocal()
    }
}

kotlin {
    jvmToolchain(17)
}