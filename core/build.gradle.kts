import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.koin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xnon-local-break-continue")
    }
}

kotlin {
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    android {
        namespace = "io.github.chrisimx.scanbridge"
        compileSdk = 36
        minSdk = 23

        compilerOptions {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            )
        }

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
        commonMain.dependencies {
            api("com.diamondedge:logging:2.1.0")
            api(libs.koin.core)
            api(libs.koin.annotations)
            api(libs.kotlinx.coroutines)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.core)
            api(libs.ktor.logging)
            api(libs.esclkt)

            // Room deps
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            api("com.rickclephas.kmp:kmp-observableviewmodel-core:1.0.3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.test:core-ktx:1.7.0")
                implementation("androidx.test.ext:junit-ktx:1.2.1")
                implementation("androidx.test:runner:1.7.0")
            }
        }

        jvmMain.dependencies {
        }
    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "ScanBridgeCore"
                    isStatic = true
                }
            }
        }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}
