pluginManagement {
    repositories {
        maven ("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    create(rootProject) {
        versions("1.20", "1.20.5", "1.21.2", "1.21.6", "1.21.11", "26.1.2")
        vcsVersion = "1.21.11"
    }
}
