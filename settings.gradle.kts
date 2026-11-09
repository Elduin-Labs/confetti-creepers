pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	}
	includeBuild("build-logic")
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.7"
	id("dev.kikugie.loom-back-compat") version "0.4.1"
}

// Fabric only. One entry per Minecraft version this mod supports.
//
// Each entry becomes a subproject under versions/, and each one downloads and
// remaps its own copy of Minecraft. Every version you add here costs real disk
// and real memory at build time — keep this list as short as the mod needs.
//
// KEEP IN SYNC with:
//   - the `minecraft` matrix in .github/workflows/release.yml
//   - the [fabric."<version>"] blocks in stonecutter.properties.toml
//   - src/main/resources/aw/<version>.accesswidener (one file per version)
stonecutter {
	create(rootProject) {
		fun fabric(version: String) =
			version("$version-fabric", version).apply { buildscript = "build.fabric.gradle.kts" }

		fabric("26.2")
		fabric("1.21.11")

		vcsVersion = "1.21.11-fabric"
	}
}
