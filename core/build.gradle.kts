import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    android {
        namespace = "io.github.chrisimx.scanbridge"
        compileSdk = 36
        minSdk = 23

        /*compilerOptions.configure {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            )
        }*/
    }


    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {

        }

        commonTest.dependencies {

        }

        androidMain.dependencies {
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
