plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.arize.tracing"
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
            baseName = "ArizeTracing"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Logging
            api(libs.kermit)
        }

        androidMain.dependencies {
            // OpenTelemetry (JVM only)
            implementation(libs.opentelemetry.api)
            implementation(libs.opentelemetry.sdk)
            implementation(libs.opentelemetry.exporter.otlp)
            implementation(libs.opentelemetry.semconv)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
