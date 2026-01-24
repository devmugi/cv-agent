plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
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

            // OpenTelemetry
            implementation(libs.opentelemetry.api)
            implementation(libs.opentelemetry.sdk)
            implementation(libs.opentelemetry.exporter.otlp)
            implementation(libs.opentelemetry.semconv)

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

// Alternative: Run through test framework
tasks.register<Test>("eval") {
    description = "Run CV Agent evaluation as a test"
    group = "verification"

    // Use the same test configuration as androidUnitTest
    dependsOn("compileTestKotlinAndroid")

    doFirst {
        val androidUnitTestTask = tasks.named<Test>("testAndroidUnitTest").get()
        testClassesDirs = androidUnitTestTask.testClassesDirs
        classpath = androidUnitTestTask.classpath
    }

    // Only include evaluation runner test
    include("**/EvalRunnerTest.class")

    // Ensure sequential execution
    maxParallelForks = 1

    // Pass environment variables
    environment("EVAL_VARIANT", project.findProperty("evalVariant") ?: "BASELINE")
    environment("EVAL_MODEL", project.findProperty("evalModel") ?: "llama-3.3-70b-versatile")
    environment("EVAL_PROJECT_MODE", project.findProperty("evalProjectMode") ?: "CURATED")
    environment("EVAL_FORMAT", project.findProperty("evalFormat") ?: "TEXT")
    environment("EVAL_QUESTIONS", project.findProperty("evalQuestions") ?: "SIMPLE")
    environment("EVAL_DELAY_MS", project.findProperty("evalDelayMs") ?: "10000")
}
