plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "cvagent.shared.generated.resources"
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.sharedDomain)
            export(projects.sharedUi)
            export(projects.sharedCareerProjects)
            export(projects.sharedAgentApi)
            export(projects.sharedAgent)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedDomain)
            api(projects.sharedUi)
            api(projects.sharedCareerProjects)
            api(projects.sharedAgentApi)
            api(projects.sharedAgent)

            // Compose (for ViewModel)
            implementation(libs.compose.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Networking (for HttpClient setup)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // DI
            implementation(libs.koin.core)

            // DataStore
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)

            // Resources
            api(libs.compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
