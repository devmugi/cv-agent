plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    source.setFrom(
        files(
            fileTree("$projectDir") {
                include("**/src/**/*.kt")
                exclude("**/shared-design-system/**")
            },
        ),
    )
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
        exclude("**/shared-design-system/**")
    }
}

tasks.register("qualityCheck") {
    dependsOn("ktlintCheck", "detekt")
    group = "verification"
    description = "Run all quality checks (Ktlint + Detekt)"
}
