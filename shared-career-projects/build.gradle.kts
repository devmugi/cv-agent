plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "cvagent.career.generated.resources"
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.career"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
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

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Compose Resources
            implementation(libs.compose.components.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
