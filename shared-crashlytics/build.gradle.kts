plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.crashlytics"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
        }
    }

    // iOS targets disabled - enable when needed
    // listOf(iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "SharedCrashlytics" }
    // }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedIdentity)
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.firebase.crashlytics)
        }
    }
}
