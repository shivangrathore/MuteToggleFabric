# Mute Toggle

A Fabric client mod that adds one rebindable key (default <kbd>M</kbd>) which toggles
Minecraft's master volume between muted and whatever it was before. Rebind it in
**Options → Controls → Key Binds → Mute Toggle**.

Nothing else. No config screen, no dependencies beyond Fabric API.

## Supported Minecraft versions

| Build target | Covers |
|---|---|
| `1.20.1`  | 1.20 – 1.20.1 |
| `1.21.1`  | 1.21 – 1.21.1 |
| `1.21.4`  | 1.21.2 – 1.21.4 |
| `1.21.8`  | 1.21.5 – 1.21.8 |
| `1.21.11` | 1.21.9 – 1.21.11 |
| `26.1`    | 26.1.x |
| `26.2`    | 26.2.x |

One jar per row. A single jar cannot cover all of them: Minecraft's classes are
remapped every release, so a jar compiled for 26.1 throws `NoSuchMethodError` on
1.20.1. What *is* shared is the source — there is one copy of the mod, and
[Stonecutter](https://stonecutter.kikugie.dev/) compiles it against each version.

## How it stays version-agnostic

Two deliberate choices keep the version-specific code down to a single block:

1. **Mojang mappings, not Yarn.** Yarn renamed effectively every class in 1.21.9
   (`MinecraftClient` → `Minecraft`, `SoundCategory` → `SoundSource`, …). Mojang's
   own names did not change, so the mod compiles unmodified across that boundary.
2. **APIs that have not moved.** `Options.getSoundSourceVolume`,
   `getSoundSourceOptionInstance`, `KeyMapping.consumeClick` and
   `setOverlayMessage` have identical signatures from 1.20.1 through 26.2. Using
   the action bar instead of a toast avoids the toast accessor, which moved twice
   over the same span (`getToasts` → `getToastManager` → `Gui.toastManager()`).

What is left — the whole of it — is in `MuteToggleClient#registerMuteKey`:

| Minecraft | Change |
|---|---|
| ≥ 1.21.9  | `KeyMapping`'s category argument became a `KeyMapping.Category` instead of a `String` |
| ≥ 1.21.11 | Mojang renamed `ResourceLocation` to `Identifier` (handled by a Stonecutter replacement rule, no source change) |
| ≥ 26.1    | Fabric API renamed `fabric-key-binding-api-v1`/`KeyBindingHelper` to `fabric-key-mapping-api-v1`/`KeyMappingHelper` |
| ≥ 26.2    | The HUD was split out of `Gui` into its own `Hud` class, so the call is `client.gui.hud.setOverlayMessage` |

Verified against Mojang's published mappings for every target, and the built jars
were checked at the bytecode level: the 1.21.8 jar constructs `KeyMapping` with a
`String` category, the 1.21.11 jar with a `Category` object.

## Working on the code

`src/main/java` is stored in the **newest** supported version's form — that is
what `vcsVersion` in `settings.gradle.kts` means, and `stonecutter active` in
`stonecutter.gradle.kts` must match it. Stonecutter rewrites the `//?` blocks for
every other version into `versions/<version>/build/generated/`.

Switch the version you are working against with Stonecutter's own task rather
than editing `stonecutter active` by hand; editing it by hand leaves the source
in one version's form while the build expects another, and the active version is
the one compiled straight from `src/`.

## Building

```bash
./gradlew ":26.1:build"          # one version
./gradlew build                  # the active version (see stonecutter.gradle.kts)
```

Jars land in `versions/<version>/build/libs/`.

To run the game: `./gradlew ":26.1:runClient"`.

## Adding a new Minecraft version

Usually: nothing. `new-version.yml` checks daily for Minecraft releases, and when
Fabric API supports one it opens a PR adding it. The build workflow runs on that
PR, so the PR tells you which case you are in:

- **Check passes** — merge it. The mod compiles against the new version unchanged
  and releases start shipping a jar for it.
- **Check fails** — Mojang moved an API this mod uses. Wrap the affected lines in
  a `//? if` block in `MuteToggleClient` and push to the PR branch.

By hand it is two edits, which is exactly what that script automates:

1. Add the version string to `versions(...)` in `settings.gradle.kts`.
2. Add a matching `["<version>"]` block to `stonecutter.properties.toml` with its
   Fabric API build and the correct `deps.keybind_module`.

The build matrix is read out of `settings.gradle.kts` at runtime, so no workflow
file ever needs editing.

You can also trigger it for a specific version from the Actions tab, or run it
locally:

```bash
python3 scripts/add_mc_version.py 1.21.12
```

## CI

- **`build.yml`** — every push and PR builds all supported versions in parallel
  and uploads each jar as a workflow artifact.
- **`release.yml`** — pushing a `v*` tag builds all versions and attaches every
  jar to a GitHub Release.
- **`new-version.yml`** — daily check for new Minecraft releases; opens a PR
  adding any it finds.

## License

MIT. Inspired by [EasyMute](https://github.com/bilektugrul/EasyMute); no code shared.
