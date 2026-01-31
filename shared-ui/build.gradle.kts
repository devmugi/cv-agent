plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedUi"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.sharedDomain)
            implementation(projects.sharedCareerProjects)
            implementation(projects.sharedAnalytics)

            // Arcane Design System
            implementation(libs.arcane.foundation)
            implementation(libs.arcane.components)
            implementation(libs.arcane.chat)

            // Compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)

            // Material Icons
            implementation(libs.compose.material.icons.extended.multiplatform)

            // Simple Icons (social/brand icons)
            implementation(libs.compose.icons.simple)

            // Lifecycle (for collectAsState)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Markdown
            implementation(libs.multiplatform.markdown.renderer)
            implementation(libs.multiplatform.markdown.renderer.m3)

            // Navigation 3
            implementation(libs.navigation3.ui)

            // Serialization (for type-safe routes)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
        }
    }
}
