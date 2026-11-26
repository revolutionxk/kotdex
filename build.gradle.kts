plugins {
    id("kotdex.documentation")
}

dependencies {
    dokka(project(":kotdex-core"))
    dokka(project(":kotdex-compose"))
}
