plugins {
    id("kotdex.library")
}

description = "Annotation driven Discord bot framework for JDA"

dependencies {
    api(libs.jda)
    api(libs.kotlinx.coroutines.core)
    implementation(kotlin("reflect"))
    implementation(libs.classgraph)
}
