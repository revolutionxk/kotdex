plugins {
    id("com.diffplug.spotless")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val ktlint = libs.findVersion("ktlint").get().requiredVersion
val composeRules = libs.findLibrary("compose-rules-ktlint").get().get()
val editorConfig = rootProject.file(".editorconfig")

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlint)
            .setEditorConfigPath(editorConfig)
            .customRuleSets(listOf("${composeRules.module}:${composeRules.versionConstraint.requiredVersion}"))
    }
    kotlinGradle {
        target("*.gradle.kts", "build-logic/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(ktlint).setEditorConfigPath(editorConfig)
    }
}
