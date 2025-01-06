import java.lang.ProcessBuilder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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

        result.toString().trim()
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
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.chrisimx.scanbridge"
        minSdk = 28
        targetSdk = 35
        versionCode = 1_000_000 // format is MAJ_MIN_PAT with always 3 digits
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}


dependencies {
    implementation(libs.okhttp)
    implementation(libs.esclkt)
    implementation(libs.zoomable)
    implementation(libs.kotlin.reflect)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}