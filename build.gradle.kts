import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    kotlin("jvm") version "2.1.0"
}

group = "nb"
version = "1.0-0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("dtx")
    manifest {
        attributes(mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        ))
    }
    from(sourceSets.main.get().output)
}

tasks.register<Jar>("examplesJar") {
    archiveFileName.set("dtx-${archiveVersion.get()}-examples.jar")
    from(sourceSets.test.get().output)
    dependsOn(tasks.jar)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    jvmToolchain(14)
}
