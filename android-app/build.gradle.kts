import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

// Load credentials from local.properties
val groqApiKey: String = localProperties.getProperty("GROQ_API_KEY")
    ?: project.findProperty("GROQ_API_KEY")?.toString()
    ?: ""
val arizeApiKey: String = localProperties.getProperty("ARIZE_API_KEY") ?: ""
val arizeSpaceId: String = localProperties.getProperty("ARIZE_SPACE_ID") ?: ""
val arizeOtlpEndpoint: String = localProperties.getProperty("ARIZE_OTLP_ENDPOINT") ?: "https://otlp.arize.com/v1"

// Load keystore properties for release signing
val releaseStoreFile: String? = localProperties.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword: String? = localProperties.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = localProperties.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = localProperties.getProperty("RELEASE_KEY_PASSWORD")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
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

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            // Dev flavor: always collect to dev Firebase, always localhost Phoenix
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "true")
            buildConfigField("Boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("Boolean", "ENABLE_PHOENIX_TRACING", "true")
            buildConfigField("String", "PHOENIX_ENDPOINT", "\"http://10.0.2.2:6006/v1/traces\"")
            buildConfigField("String", "ARIZE_API_KEY", "\"\"")
            buildConfigField("String", "ARIZE_SPACE_ID", "\"\"")
        }
        create("prod") {
            dimension = "environment"

            // Prod flavor: default to localhost Phoenix (overridden to Arize Cloud for prodRelease)
            buildConfigField("Boolean", "ENABLE_PHOENIX_TRACING", "true")
            buildConfigField("String", "PHOENIX_ENDPOINT", "\"http://10.0.2.2:6006/v1/traces\"")
            buildConfigField("String", "ARIZE_API_KEY", "\"\"")
            buildConfigField("String", "ARIZE_SPACE_ID", "\"\"")
        }
    }

    buildTypes {
        getByName("debug") {
            // Debug-specific settings (analytics/crashlytics controlled by androidComponents below)
        }
        getByName("release") {
            isMinifyEnabled = false
            ndk {
                debugSymbolLevel = "FULL"
            }
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        all {
            buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
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

// Handle prodDebug vs prodRelease: analytics/crashlytics OFF for prodDebug, ON for prodRelease
androidComponents {
    onVariants { variant ->
        val isProd = variant.flavorName == "prod"
        val isDebug = variant.buildType == "debug"

        when {
            isProd && isDebug -> {
                // prodDebug: collection OFF (for local testing without polluting prod Firebase)
                variant.buildConfigFields?.put(
                    "ENABLE_ANALYTICS",
                    BuildConfigField("Boolean", "false", "Analytics disabled for prodDebug")
                )
                variant.buildConfigFields?.put(
                    "ENABLE_CRASHLYTICS",
                    BuildConfigField("Boolean", "false", "Crashlytics disabled for prodDebug")
                )
            }
            isProd && !isDebug -> {
                // prodRelease: collection ON, Arize Cloud tracing
                variant.buildConfigFields?.put(
                    "ENABLE_ANALYTICS",
                    BuildConfigField("Boolean", "true", "Analytics enabled for prodRelease")
                )
                variant.buildConfigFields?.put(
                    "ENABLE_CRASHLYTICS",
                    BuildConfigField("Boolean", "true", "Crashlytics enabled for prodRelease")
                )
                variant.buildConfigFields?.put(
                    "PHOENIX_ENDPOINT",
                    BuildConfigField("String", "\"${arizeOtlpEndpoint}/traces\"", "Arize Cloud endpoint")
                )
                variant.buildConfigFields?.put(
                    "ARIZE_API_KEY",
                    BuildConfigField("String", "\"$arizeApiKey\"", "Arize API key")
                )
                variant.buildConfigFields?.put(
                    "ARIZE_SPACE_ID",
                    BuildConfigField("String", "\"$arizeSpaceId\"", "Arize Space ID")
                )
            }
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.sharedAnalytics)
    implementation(projects.sharedCareerProjects)
    implementation(projects.sharedAgentApi)
    implementation(projects.sharedCrashlytics)

    // Arcane Design System (needed for ArcaneTheme and ArcaneToastHost in MainActivity)
    implementation(libs.arcane.foundation)
    implementation(libs.arcane.components)

    implementation(libs.androidx.activity.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kermit)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

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

// Ensure career module compose resources are prepared before merging assets and lint for all variants
afterEvaluate {
    // Asset merge tasks
    listOf(
        "mergeDevDebugAssets",
        "mergeDevReleaseAssets",
        "mergeProdDebugAssets",
        "mergeProdReleaseAssets"
    ).forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn(copyCareerComposeResources)
    }

    // Lint tasks that also need the career resources
    tasks.matching { task ->
        task.name.contains("lintVitalAnalyze") ||
            task.name.contains("LintVitalReportModel") ||
            task.name.contains("lintAnalyze")
    }.configureEach {
        dependsOn(copyCareerComposeResources)
    }
}
