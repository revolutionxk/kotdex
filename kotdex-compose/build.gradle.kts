plugins {
    id("kotdex.compose")
}

description = "Compose runtime for live Discord messages"

dependencies {
    api(project(":kotdex-core"))
    api(libs.compose.runtime)
}
