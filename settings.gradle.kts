pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks the right Loom variant per version (26.1+ ships deobfuscated and needs a different one)
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Adding a Minecraft version: add it here, then add its block to stonecutter.properties.toml.
        versions(
            "1.20.1",
            "1.21.1",
            "1.21.4",
            "1.21.8",
            "1.21.11",
            "26.1",
            "26.2",
            "26.3",
        )
        vcsVersion = "26.2"
    }
}

rootProject.name = "MuteToggleFabric"
