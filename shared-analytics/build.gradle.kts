plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.analytics"
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
            baseName = "SharedAnalytics"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedIdentity)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.firebase.analytics)
        }
    }
}
