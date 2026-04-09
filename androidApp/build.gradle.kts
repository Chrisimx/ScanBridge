import com.android.utils.TraceUtils.simpleId
import java.util.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val testConfig = Properties()
val testConfigFile = rootProject.file("testConfig.properties")
if (testConfigFile.exists()) {
    testConfigFile.inputStream().use { testConfig.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paraphrase)
}

val gitHashProvider = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }

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
        versionCode = 2_001_004 // format is MAJ_MIN_PAT with always 3 digits
        versionName = "2.1.4"

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
            buildConfigField("String", "GIT_COMMIT_HASH", "\"${gitHashProvider.get()}\"")
        }
        debug {
            buildConfigField("String", "GIT_COMMIT_HASH", "\"${gitHashProvider.get()}\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.annotations)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androix.navigation)
    implementation(libs.zoomable)
    implementation(libs.kotlin.reflect)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core"))
    implementation(project(":composeUI"))
    implementation(libs.androidx.concurrent.futures)

    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)

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
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.logging)
    "playImplementation"(project(":lvl_library"))
    "playImplementation"(libs.retrofit)
    "playImplementation"(libs.retrofit.converter.gson)
    "playImplementation"(libs.kotlinx.coroutines.android)

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
        val protoc = libs.protoc.get()

        artifact = "${protoc.module}:${protoc.version}"
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

koinCompiler {
    compileSafety = true
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
