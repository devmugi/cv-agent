import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.devmugi.cv.agent.ui.screenshots"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }
}

dependencies {
    // Modules under test
    implementation(projects.sharedUi)
    implementation(projects.sharedDomain)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    // Design system
    implementation(libs.arcane.foundation)
    implementation(libs.arcane.components)
    implementation(libs.arcane.chat)

    // Screenshot testing (using captureRoboImage directly, no JUnit rule needed)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.compose.ui.test.junit4)
}
