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

    // iOS targets disabled - dependencies don't support iOS yet
    // listOf(
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "SharedAgentApi"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        commonMain.dependencies {
            api(projects.arizeTracing)
            api(projects.sharedDomain)
            api(projects.sharedIdentity)

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
            // OpenTelemetry dependencies now come from arize-tracing module
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

// Exclude integration/evaluation tests from default test task (they hit real API)
tasks.withType<Test>().configureEach {
    if (name == "testAndroidUnitTest" || name == "testDebugUnitTest" || name == "testReleaseUnitTest") {
        exclude("**/AgentEvaluationTest.class")
        exclude("**/GroqApiClientIntegrationTest.class")
    }
}

// Dedicated task for running evaluation tests (real API calls)
tasks.register<Test>("evaluationTests") {
    description = "Run evaluation tests that make real Groq API calls"
    group = "verification"

    // Use the same test configuration as androidUnitTest
    testClassesDirs = tasks.named<Test>("testAndroidUnitTest").get().testClassesDirs
    classpath = tasks.named<Test>("testAndroidUnitTest").get().classpath

    // Only include evaluation tests
    include("**/AgentEvaluationTest.class")
    include("**/GroqApiClientIntegrationTest.class")

    // Ensure sequential execution
    maxParallelForks = 1
}
