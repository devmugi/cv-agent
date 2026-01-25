plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.agent"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
        }
    }

    // iOS targets disabled - dependencies don't support iOS yet
    // listOf(
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "SharedAgent"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedDomain)
            api(projects.sharedCareerProjects)
            api(projects.sharedAgentApi)

            // Compose (for ViewModel)
            implementation(libs.compose.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Logging (via shared-agent-api)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.mock)
        }
    }
}
