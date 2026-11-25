plugins {
    id("kotdex.compose")
}

dependencies {
    api(project(":kotdex-core"))
    api(libs.compose.runtime)
}
