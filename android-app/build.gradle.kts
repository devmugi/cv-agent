import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "io.github.devmugi.cv.agent"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].assets.srcDirs(
        layout.buildDirectory.dir("careerComposeResources")
    )

    defaultConfig {
        applicationId = "io.github.devmugi.cv.agent"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("Boolean", "ENABLE_PHOENIX_TRACING", "true")
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("Boolean", "ENABLE_PHOENIX_TRACING", "false")
        }
        all {
            val apiKey = localProperties.getProperty("GROQ_API_KEY")
                ?: project.findProperty("GROQ_API_KEY")?.toString()
                ?: ""
            buildConfigField("String", "GROQ_API_KEY", "\"$apiKey\"")

            // Phoenix tracing host (set in local.properties for real device testing)
            val phoenixHost = localProperties.getProperty("PHOENIX_HOST") ?: ""
            buildConfigField("String", "PHOENIX_HOST", "\"$phoenixHost\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.sharedCareerProjects)
    implementation(projects.sharedAgentApi)

    // Arcane Design System (needed for ArcaneTheme and ArcaneToastHost in MainActivity)
    implementation(libs.arcane.foundation)
    implementation(libs.arcane.components)

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kermit)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

// Copy career module compose resources with correct namespace path
val copyCareerComposeResources by tasks.registering(Copy::class) {
    dependsOn(":shared-career-projects:prepareComposeResourcesTaskForCommonMain")
    from("../shared-career-projects/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources")
    into(layout.buildDirectory.dir("careerComposeResources/composeResources/cvagent.career.generated.resources"))
}

// Ensure career module compose resources are prepared before merging assets
afterEvaluate {
    tasks.named("mergeDebugAssets") {
        dependsOn(copyCareerComposeResources)
    }
    tasks.findByName("mergeReleaseAssets")?.dependsOn(copyCareerComposeResources)
}
