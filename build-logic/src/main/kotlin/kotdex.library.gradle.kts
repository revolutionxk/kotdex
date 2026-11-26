import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    `java-library`
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.findLibrary("mockk").get())
    testImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    testRuntimeOnly(libs.findLibrary("logback").get())
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name = project.name
        description = provider { project.description }
        inceptionYear = "2023"
        url = "https://github.com/revolutionxk/kotdex"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "revolutionxk"
                name = "Revolution"
                url = "https://github.com/revolutionxk"
            }
        }

        scm {
            url = "https://github.com/revolutionxk/kotdex"
            connection = "scm:git:git://github.com/revolutionxk/kotdex.git"
            developerConnection = "scm:git:ssh://git@github.com/revolutionxk/kotdex.git"
        }
    }
}
