import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

// Load local.properties for API keys
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.eval"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.sharedDomain)
            implementation(projects.sharedCareerProjects)
            implementation(projects.sharedAgentApi)
            implementation(projects.sharedAgent)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Logging
            implementation(libs.kermit)
        }

        androidMain.dependencies {
            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Tracing (via arize-tracing module)
            implementation(projects.arizeTracing)

            // CLI
            implementation(libs.clikt)

            // YAML parsing
            implementation(libs.kaml)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// Custom task to run evaluation
tasks.register<JavaExec>("runEval") {
    description = "Run CV Agent evaluation"
    group = "verification"

    // This will be configured after the compilation is done
    dependsOn("compileAndroidMain")

    doFirst {
        val androidUnitTestTask = tasks.named<Test>("testAndroidUnitTest").get()
        classpath = androidUnitTestTask.classpath + files("build/classes/kotlin/android/main")
        mainClass.set("io.github.devmugi.cv.agent.eval.MainKt")

        // Pass through command line args
        val evalArgs = project.findProperty("evalArgs")?.toString()?.split(" ") ?: emptyList()
        args = evalArgs
    }
}

// Configure test tasks with eval environment variables
tasks.withType<Test>().configureEach {
    if (name == "testAndroidUnitTest") {
        // Pass API keys from local.properties
        localProperties.getProperty("GROQ_API_KEY")?.let { environment("GROQ_API_KEY", it) }
        localProperties.getProperty("PHOENIX_HOST")?.let { environment("PHOENIX_HOST", it) }

        // Pass environment variables for evaluation configuration
        environment("EVAL_VARIANT", project.findProperty("evalVariant") ?: "BASELINE")
        environment("EVAL_MODEL", project.findProperty("evalModel") ?: "llama-3.3-70b-versatile")
        environment("EVAL_PROJECT_MODE", project.findProperty("evalProjectMode") ?: "CURATED")
        environment("EVAL_FORMAT", project.findProperty("evalFormat") ?: "TEXT")
        environment("EVAL_QUESTIONS", project.findProperty("evalQuestions") ?: "SIMPLE")
        environment("EVAL_DELAY_MS", project.findProperty("evalDelayMs") ?: "5000")
        environment("EVAL_REPORT_DIR", project.findProperty("evalReportDir") ?: "eval/reports")

        // Pass comparison run IDs
        environment("BASELINE_RUN_ID", project.findProperty("baselineRun") ?: "")
        environment("VARIANT_RUN_ID", project.findProperty("variantRun") ?: "")

        // Filter based on which task is requested
        val runEval = project.gradle.startParameter.taskNames.any { it.contains("eval") && !it.contains("compare") }
        val runCompare = project.gradle.startParameter.taskNames.any { it.contains("compare") }

        if (runEval) {
            filter {
                includeTestsMatching("*EvalRunnerTest*")
            }
        } else if (runCompare) {
            filter {
                includeTestsMatching("*EvalCompareTest*")
            }
        }
    }
}

// Convenience task to run evaluation
tasks.register("eval") {
    description = "Run CV Agent evaluation"
    group = "verification"
}

// Convenience task to compare two runs
tasks.register("compare") {
    description = "Compare two evaluation runs"
    group = "verification"
}

// Wire up dependencies after evaluation
afterEvaluate {
    tasks.findByName("testAndroidUnitTest")?.let { testTask ->
        tasks.named("eval") {
            dependsOn(testTask)
        }
        tasks.named("compare") {
            dependsOn(testTask)
        }
    }
}
