plugins {
    id("kotdex.documentation")
    id("kotdex.spotless")
}

dependencies {
    dokka(project(":kotdex-core"))
    dokka(project(":kotdex-compose"))
}
