plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.career"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedCareerProjects"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Arcane Design System
            implementation(libs.arcane.foundation)
            implementation(libs.arcane.components)

            // Compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)

            // Material Icons
            implementation(libs.compose.material.icons.extended.multiplatform)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
