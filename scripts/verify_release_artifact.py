#!/usr/bin/env python3
"""Verify the universal mod JAR and optional release bundle."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path, PurePosixPath
import sys
import tomllib
from zipfile import BadZipFile, ZipFile, ZipInfo


MOD_ID = "lostcitiesroadfixes"
MOD_NAME = "Lost Cities: Road Fixes"
MOD_VERSION = "1.0.0+mc1.21.1"
MOD_OWNER = "Austizz_tds"
MINECRAFT_VERSION = "1.21.1"
NEOFORGE_RANGE = "[21.1.235,)"
LOST_CITIES_RANGE = "[1.21-8.3.10]"
NORMALIZED_ZIP_TIME = (1980, 2, 1, 0, 0, 0)
LICENSE_ENTRY = f"META-INF/LICENSE_{MOD_ID}"
REQUIRED_ENTRIES = frozenset({
    "META-INF/MANIFEST.MF",
    "META-INF/neoforge.mods.toml",
    LICENSE_ENTRY,
    "lostcitiesroadfixes.mixins.json",
    "net/austizz/lostcitiesroadfixes/LostCitiesRoadFixes.class",
    "net/austizz/lostcitiesroadfixes/compat/LostCitiesMixinPlugin.class",
    "net/austizz/lostcitiesroadfixes/mixin/HighwaysMixin.class",
    "net/austizz/lostcitiesroadfixes/mixin/LostCityTerrainFeatureMixin.class",
})
FORBIDDEN_PARTS = frozenset({
    ".gradle", ".idea", "__pycache__", "build", "run", "src",
})
FORBIDDEN_SUFFIXES = (".bbmodel", ".java", ".pyc", ".pyo")


class VerificationError(ValueError):
    """Raised when a release artifact violates its contract."""


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_manifest(data: bytes) -> dict[str, str]:
    unfolded: list[str] = []
    for line in data.decode("utf-8").replace("\r\n", "\n").split("\n"):
        if line.startswith(" ") and unfolded:
            unfolded[-1] += line[1:]
        elif line:
            unfolded.append(line)
    result: dict[str, str] = {}
    for line in unfolded:
        key, separator, value = line.partition(": ")
        if not separator:
            raise VerificationError(f"invalid manifest line: {line!r}")
        result[key] = value
    return result


def verify_zip_entries(entries: list[ZipInfo], artifact_name: str) -> set[str]:
    names = [entry.filename for entry in entries]
    if len(names) != len(set(names)):
        raise VerificationError(f"{artifact_name} contains duplicate entries")
    for entry in entries:
        path = PurePosixPath(entry.filename)
        if path.is_absolute() or ".." in path.parts:
            raise VerificationError(f"unsafe archive path: {entry.filename}")
        if entry.date_time != NORMALIZED_ZIP_TIME:
            raise VerificationError(
                f"non-reproducible timestamp on {entry.filename}: {entry.date_time}")
    return set(names)


def require_dependency(
        dependencies: list[dict[str, object]],
        mod_id: str,
        version_range: str,
        ordering: str) -> None:
    dependency = next(
        (candidate for candidate in dependencies if candidate.get("modId") == mod_id),
        None)
    if dependency is None:
        raise VerificationError(f"missing required dependency {mod_id}")
    expected = {
        "type": "required",
        "versionRange": version_range,
        "ordering": ordering,
        "side": "BOTH",
    }
    for key, value in expected.items():
        if dependency.get(key) != value:
            raise VerificationError(
                f"dependency {mod_id} has {key}={dependency.get(key)!r}, expected {value!r}")


def verify_jar(jar_path: Path, project_root: Path) -> tuple[str, int]:
    expected_name = f"{MOD_ID}-{MOD_VERSION}.jar"
    if jar_path.name != expected_name:
        raise VerificationError(
            f"universal JAR must be named {expected_name}, found {jar_path.name}")
    jar_bytes = jar_path.read_bytes()
    with ZipFile(jar_path) as archive:
        names = verify_zip_entries(archive.infolist(), jar_path.name)
        missing = sorted(REQUIRED_ENTRIES - names)
        if missing:
            raise VerificationError(f"universal JAR is missing: {', '.join(missing)}")
        for name in names:
            path = PurePosixPath(name)
            if FORBIDDEN_PARTS.intersection(path.parts) or name.endswith(FORBIDDEN_SUFFIXES):
                raise VerificationError(f"development-only JAR entry: {name}")

        expected_license = (project_root / "LICENSE").read_bytes()
        if archive.read(LICENSE_ENTRY) != expected_license:
            raise VerificationError("embedded MIT license differs from repository LICENSE")

        metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode("utf-8"))
        if metadata.get("modLoader") != "javafml" or metadata.get("license") != "MIT":
            raise VerificationError("NeoForge loader/license metadata is incorrect")
        mods = metadata.get("mods", [])
        if len(mods) != 1:
            raise VerificationError("release must declare exactly one mod")
        expected_mod = {
            "modId": MOD_ID,
            "version": MOD_VERSION,
            "displayName": MOD_NAME,
            "authors": MOD_OWNER,
        }
        for key, value in expected_mod.items():
            if mods[0].get(key) != value:
                raise VerificationError(
                    f"mod metadata has {key}={mods[0].get(key)!r}, expected {value!r}")
        mixins = metadata.get("mixins", [])
        if {entry.get("config") for entry in mixins} != {"lostcitiesroadfixes.mixins.json"}:
            raise VerificationError("required mixin configuration is not declared exactly once")
        dependencies = metadata.get("dependencies", {}).get(MOD_ID, [])
        require_dependency(dependencies, "minecraft", f"[{MINECRAFT_VERSION}]", "NONE")
        require_dependency(dependencies, "neoforge", NEOFORGE_RANGE, "NONE")
        require_dependency(dependencies, "lostcities", LOST_CITIES_RANGE, "AFTER")

        manifest = read_manifest(archive.read("META-INF/MANIFEST.MF"))
        expected_manifest = {
            "Manifest-Version": "1.0",
            "Implementation-Title": MOD_NAME,
            "Implementation-Version": MOD_VERSION,
            "Implementation-Vendor": MOD_OWNER,
            "Built-For-Minecraft": MINECRAFT_VERSION,
        }
        for key, value in expected_manifest.items():
            if manifest.get(key) != value:
                raise VerificationError(
                    f"manifest has {key}={manifest.get(key)!r}, expected {value!r}")
    return sha256(jar_bytes), len(jar_bytes)


def verify_bundle(bundle_path: Path, jar_path: Path, project_root: Path) -> str:
    expected_names = {
        jar_path.name, "README.md", "LICENSE", "CHANGELOG.md", "SHA256SUMS",
    }
    bundle_bytes = bundle_path.read_bytes()
    with ZipFile(bundle_path) as archive:
        names = verify_zip_entries(archive.infolist(), bundle_path.name)
        if names != expected_names:
            raise VerificationError(
                f"release bundle entries differ: {sorted(names ^ expected_names)}")
        for document in ("README.md", "LICENSE", "CHANGELOG.md"):
            if archive.read(document) != (project_root / document).read_bytes():
                raise VerificationError(f"bundle {document} differs from repository copy")
        if archive.read(jar_path.name) != jar_path.read_bytes():
            raise VerificationError("bundled universal JAR differs from build output")

        checksum_lines = archive.read("SHA256SUMS").decode("ascii").splitlines()
        checksums: dict[str, str] = {}
        for line in checksum_lines:
            digest, separator, name = line.partition("  ")
            if not separator or len(digest) != 64:
                raise VerificationError(f"invalid checksum line: {line!r}")
            if name in checksums:
                raise VerificationError(f"duplicate checksum entry: {name}")
            checksums[name] = digest
        expected_checksum_names = expected_names - {"SHA256SUMS"}
        if set(checksums) != expected_checksum_names:
            raise VerificationError("SHA256SUMS does not cover every release payload")
        for name, digest in checksums.items():
            if sha256(archive.read(name)) != digest:
                raise VerificationError(f"checksum mismatch for {name}")
    return sha256(bundle_bytes)


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, help="universal mod JAR")
    parser.add_argument("--bundle", type=Path, help="optional release ZIP")
    parser.add_argument(
        "--project-root", type=Path,
        default=Path(__file__).resolve().parents[1],
        help="repository root (default: inferred from this script)")
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    try:
        jar_digest, jar_size = verify_jar(
            arguments.jar.resolve(), arguments.project_root.resolve())
        print(f"verified universal JAR: sha256={jar_digest} bytes={jar_size}")
        if arguments.bundle:
            bundle_digest = verify_bundle(
                arguments.bundle.resolve(), arguments.jar.resolve(),
                arguments.project_root.resolve())
            print(f"verified release bundle: sha256={bundle_digest}")
    except (BadZipFile, OSError, VerificationError, tomllib.TOMLDecodeError) as exception:
        print(f"Release verification failed: {exception}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
