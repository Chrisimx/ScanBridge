import java.security.MessageDigest
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
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

    defaultConfig {
        applicationId = "io.github.chrisimx.scanbridge"
        minSdk = 28
        targetSdk = 36
        versionCode = 1_006_002 // format is MAJ_MIN_PAT with always 3 digits
        versionName = "1.6.2"

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
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.screengrab)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun calculateChecksum(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
    val actualChecksum = calculateChecksum(file)
    return actualChecksum == expectedChecksum
}

val checksums = mapOf(
    "x86_64" to "a7c6cf413b8963017212a3c59530ac7d0a48379c7d3dd7128babf2937f6c0a97",
    "armeabi-v7a" to "8de29fd1aff02d24c1adff34a0f83e686fae6024cfdb25b5d6647eb0916399d5",
    "arm64-v8a" to "10e843da28ff9b1089a9130ee60ba45d57b6c3bc218e5fa044c8e6d86b88991b"
)

fun downloadESCLMockServer(archName: String, archPath: String, client: OkHttpClient) {
    val request = Request.Builder()
        .url("https://chrisimx.github.io/escl-mock-server/$archPath/escl-mock-server").get().build()
    client.newCall(request).execute().use { response ->
        File("./cache/escl-mock-server/$archName").mkdirs()

        val f = File("./cache/escl-mock-server/$archName/escl-mock-server")

        f.createNewFile()

        f.outputStream().use {
            val stream = response.body!!.byteStream()
            stream.copyTo(it)
        }

        if (!verifyChecksum(f, checksums[archName]!!)) {
            throw IllegalStateException("Checksum verification failed for $archName")
        }
    }
}

tasks.register("downloadEsclMockServer") {
    description = "Build the eSCL dummy server"

    outputs.dirs("../cache/escl-mock-server")

    doLast {
        val client = OkHttpClient()

        downloadESCLMockServer("x86_64", "android-x86-64", client)
        downloadESCLMockServer("armeabi-v7a", "android-armv7", client)
        downloadESCLMockServer("arm64-v8a", "android-arm64-v8a", client)
    }
}

val architectures = listOf("arm64-v8a", "armeabi-v7a", "x86_64")

architectures.forEach { arch ->
    tasks.register<Copy>("copyEsclMockServer$arch") {
        description =
            "Copy the eSCL dummy server to the androidTest native-libs folder for the $arch ABI"

        dependsOn("downloadEsclMockServer") // Ensure the files are downloaded before copying

        duplicatesStrategy = DuplicatesStrategy.FAIL

        from("../cache/escl-mock-server/$arch/escl-mock-server") {
            rename { "lib_escl_mock.so" }
        }

        into("src/androidTest/native-libs/$arch/")

        outputs.files(
            file("src/androidTest/native-libs/$arch/lib_escl_mock.so")
        )
    }
}

val taskNamesCopyESCL = architectures.map { "copyEsclMockServer$it" }

tasks.register("copyEsclMockServerAll") {
    description =
        "Copy the eSCL dummy server for all ABIs/archs to the androidTest native-libs folder"

    dependsOn(taskNamesCopyESCL)
}

afterEvaluate {

    tasks.named("connectedFdroidDebugAndroidTest") {
        dependsOn("copyEsclMockServerAll")
    }

    tasks.named("mergeFdroidDebugAndroidTestJniLibFolders") {
        dependsOn("copyEsclMockServerAll")
    }

    tasks.named("connectedPlayDebugAndroidTest") {
        dependsOn("copyEsclMockServerAll")
    }

    tasks.named("mergePlayDebugAndroidTestJniLibFolders") {
        dependsOn("copyEsclMockServerAll")
    }

    tasks.named("clean") {
        doLast {
            delete(file("src/androidTest/native-libs"), file("../cache"))
        }
    }
}
