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

    // iOS targets disabled - enable when needed
    // listOf(
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "SharedAnalytics"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        commonMain.dependencies {
            // No dependencies - pure analytics abstraction
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            // Firebase Analytics (Phase 2)
            // implementation(platform(libs.firebase.bom))
            // implementation(libs.firebase.analytics)
        }
    }
}
