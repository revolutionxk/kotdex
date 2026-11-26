import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
