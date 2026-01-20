plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

ktlint {
    version.set("1.5.0")
    android.set(true)
    verbose.set(true)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

tasks.register("qualityCheck") {
    dependsOn("ktlintCheck", "detekt")
    group = "verification"
    description = "Run all quality checks (Ktlint + Detekt)"
}
