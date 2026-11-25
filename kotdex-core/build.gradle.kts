plugins {
    id("kotdex.library")
}

dependencies {
    api(libs.jda)
    api(libs.coroutines.core)
    implementation(kotlin("reflect"))
    implementation(libs.classgraph)
}
