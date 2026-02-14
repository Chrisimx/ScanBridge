import java.net.URI

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Fireamp Snapshots"
            url = uri("https://repo.fireamp.eu/repository/maven-snapshots/")

            content {
                includeGroup("io.github.chrisimx")
            }
        }
    }
}

rootProject.name = "ScanBridge"
include(":app")
include(":lvl_library")
project(":lvl_library").projectDir = File(rootDir, "libraries/play-licensing/lvl_library/")
