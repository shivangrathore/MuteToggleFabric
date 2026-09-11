#!/usr/bin/env python3
"""Adds a new Minecraft version as a build target.

Figures out the Fabric API build and the correct key-binding module for the
version, then edits settings.gradle.kts and stonecutter.properties.toml in
place. Run by .github/workflows/new-version.yml, but works standalone:

    python3 scripts/add_mc_version.py            # latest stable, if missing
    python3 scripts/add_mc_version.py 1.21.12    # a specific one
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETTINGS = ROOT / "settings.gradle.kts"
PROPS = ROOT / "stonecutter.properties.toml"

FABRIC_META = "https://meta.fabricmc.net/v2/versions/game"
MODRINTH = "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=[%22{}%22]"
POM = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{v}/fabric-api-{v}.pom"


def get(url: str) -> str:
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read().decode()


def configured_versions() -> list[str]:
    block = re.search(r"versions\((.*?)\)", SETTINGS.read_text(), re.S).group(1)
    return re.findall(r'"([^"]+)"', block)


def latest_stable() -> str:
    return next(v["version"] for v in json.loads(get(FABRIC_META)) if v["stable"])


def fabric_api_build(mc: str) -> str | None:
    releases = json.loads(get(MODRINTH.format(mc)))
    # Modrinth returns newest first; prefer a full release over alpha/beta.
    for wanted in ("release", None):
        for r in releases:
            if wanted is None or r["version_type"] == wanted:
                return r["version_number"]
    return None


def keybind_module(api_build: str) -> str:
    """Fabric API renamed this module in 26.1; ask the POM which one exists."""
    quoted = api_build.replace("+", "%2B")
    pom = get(POM.format(v=quoted).replace(f"fabric-api-{quoted}", f"fabric-api-{api_build}"))
    for name in ("fabric-key-mapping-api-v1", "fabric-key-binding-api-v1"):
        if f"<artifactId>{name}</artifactId>" in pom:
            return name
    raise SystemExit(f"neither key module found in fabric-api {api_build}")


def add(mc: str, api_build: str, module: str) -> None:
    settings = SETTINGS.read_text()
    existing = configured_versions()
    last = existing[-1]
    settings = settings.replace(f'"{last}",\n', f'"{last}",\n            "{mc}",\n', 1)
    SETTINGS.write_text(settings)

    PROPS.write_text(
        PROPS.read_text().rstrip("\n")
        + f'\n\n["{mc}"]\n'
        + f'mod.mc_compat = "{mc}"\n'
        + f'mod.mc_releases = ["{mc}"]\n'
        + f'deps.fabric_api = "{api_build}"\n'
        + f'deps.keybind_module = "{module}"\n'
    )


def main() -> int:
    mc = sys.argv[1] if len(sys.argv) > 1 else latest_stable()
    if mc in configured_versions():
        print(f"{mc} already configured")
        return 0

    api_build = fabric_api_build(mc)
    if not api_build:
        print(f"no Fabric API build for {mc} yet")
        return 0

    module = keybind_module(api_build)
    add(mc, api_build, module)
    print(f"added {mc} (fabric-api {api_build}, {module})")

    # Consumed by the workflow to build the PR body.
    out = ROOT / "new-version.json"
    out.write_text(json.dumps({"mc": mc, "api": api_build, "module": module}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
