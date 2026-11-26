plugins {
    id("kotdex.library")
}

dependencies {
    api(libs.jda)
    api(libs.kotlinx.coroutines.core)
    implementation(kotlin("reflect"))
    implementation(libs.classgraph)
}
