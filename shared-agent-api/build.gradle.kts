plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.api"
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
            baseName = "SharedAgentApi"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedDomain)

            // Networking - api because GroqApiClient constructor exposes HttpClient
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Logging
            api(libs.kermit)
        }

        androidMain.dependencies {
            // OpenTelemetry (JVM only for now)
            implementation(libs.opentelemetry.api)
            implementation(libs.opentelemetry.sdk)
            implementation(libs.opentelemetry.exporter.otlp)
            implementation(libs.opentelemetry.semconv)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)

                // OpenTelemetry for tracing tests
                implementation(libs.opentelemetry.api)
                implementation(libs.opentelemetry.sdk)
                implementation(libs.opentelemetry.exporter.otlp)

                // For evaluation tests
                implementation(projects.sharedAgent)
                implementation(projects.sharedCareerProjects)
            }
        }
    }
}
