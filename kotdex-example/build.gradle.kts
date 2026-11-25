plugins {
    id("kotdex.application")
}

dependencies {
    implementation(project(":kotdex-compose"))
    runtimeOnly(libs.logback)
}

application {
    mainClass = "com.kotdex.example.MainKt"
}
