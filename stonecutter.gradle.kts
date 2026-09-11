plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        // Source is written in the newest form (see vcsVersion). Mojang renamed
        // ResourceLocation to Identifier in 1.21.11, so older targets get it
        // renamed back on the way in.
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
        }
    }
}
