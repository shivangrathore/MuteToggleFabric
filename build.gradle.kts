plugins {
    // Applies the correct Loom variant for this version (26.1+ ships deobfuscated)
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ... here; Stonecutter needs the default.
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_1_8
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang mappings stay stable across versions where Yarn does not, which is
    // what keeps the version-specific code in this mod down to a single block.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    // Only the two Fabric API modules this mod actually uses.
    val fapi: String = sc.properties["deps.fabric_api"]
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fapi))
    modImplementation(fabricApi.module(sc.properties["deps.keybind_module"], fapi))
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
    runConfigs.remove(runConfigs.findByName("server"))
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            register("keybind_module", "deps.keybind_module")
        }

        filesMatching("fabric.mod.json") { expand(props) }
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from(rootProject.file("LICENSE")) { rename { "$it-$name" } }
    }

    // Drops every version's jars into build/libs/<mod version>/ at the repo root.
    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod and copies the jars to build/libs/{mod version}/"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
