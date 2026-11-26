pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("androidx\\..*")
            }
        }
    }
}

rootProject.name = "kotdex"

include("kotdex-core")
include("kotdex-compose")
include("kotdex-example")
