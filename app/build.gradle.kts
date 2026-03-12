import java.util.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val testConfig = Properties()
val testConfigFile = rootProject.file("testConfig.properties")
if (testConfigFile.exists()) {
    testConfigFile.inputStream().use { testConfig.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.room)
    id("com.google.devtools.ksp") version "2.3.6"
    id("app.cash.paraphrase") version "0.4.1"
}

fun getGitCommitHash(): String {
    return try {
        val command = "git rev-parse --short HEAD"
        val process = ProcessBuilder()
            .command(command.split(" "))
            .directory(rootProject.projectDir)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        val wait = process.waitFor(60, TimeUnit.SECONDS)
        if (!wait) {
            return "unknown"
        }

        val result = process.inputStream.bufferedReader().readText()

        result.trim()
    } catch (_: Exception) {
        "unknown" // Fallback
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    compilerOptions {
        freeCompilerArgs = listOf("-Xnon-local-break-continue")
    }
}

android {
    namespace = "io.github.chrisimx.scanbridge"
    compileSdk = 36

    androidResources {
        generateLocaleConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    defaultConfig {
        applicationId = "io.github.chrisimx.scanbridge"
        minSdk = 28
        targetSdk = 36
        versionCode = 2_001_002 // format is MAJ_MIN_PAT with always 3 digits
        versionName = "2.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["escl_server_url"] =
            testConfig.getOrDefault("escl_server_url", "http://127.0.0.1:8080") as String
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "GIT_COMMIT_HASH", "\"${getGitCommitHash()}\"")
        }
        debug {
            buildConfigField("String", "GIT_COMMIT_HASH", "\"${getGitCommitHash()}\"")

            sourceSets {
                getByName("androidTest") {
                    jniLibs.srcDirs("src/androidTest/native-libs")
                    packaging {
                        jniLibs {
                            useLegacyPackaging = true
                        }
                    }
                }
            }
        }
    }
    flavorDimensions += "edition"

    productFlavors {
        create("fdroid") {
            dimension = "edition"
        }
        create("play") {
            dimension = "edition"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
    sourceSets.all {
        languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.annotations)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androix.navigation)
    implementation(libs.esclkt)
    implementation(libs.zoomable)
    implementation(libs.kotlin.reflect)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.itext7.core)
    implementation(libs.ktor.cio)
    implementation(libs.ktor.logging)
    "playImplementation"(project(":lvl_library"))
    "playImplementation"("com.squareup.retrofit2:retrofit:3.0.0")
    "playImplementation"("com.squareup.retrofit2:converter-gson:2.9.0")
    "playImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation(libs.junit)

    androidTestImplementation(libs.escl.mock.server)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.screengrab)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.rules)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.33.5"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin")
            }
        }
    }
}

afterEvaluate {
    tasks.named("clean") {
        doLast {
            delete(file("src/androidTest/native-libs"), file("../cache"))
        }
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}
