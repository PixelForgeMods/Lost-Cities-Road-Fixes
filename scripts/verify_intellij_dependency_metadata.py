#!/usr/bin/env python3
"""Ensure Gradle trusts every IDE-only artifact observed during IntelliJ sync."""

from pathlib import Path
import sys
import xml.etree.ElementTree as element_tree


REQUIRED_ARTIFACTS = frozenset({
    "annotations-24.1.0-javadoc.jar",
    "antlr4-runtime-4.13.1-javadoc.jar",
    "asm-9.8-javadoc.jar",
    "asm-analysis-9.8-javadoc.jar",
    "asm-commons-9.8-javadoc.jar",
    "asm-tree-9.8-javadoc.jar",
    "asm-util-9.8-javadoc.jar",
    "bus-8.0.5-javadoc.jar",
    "checker-qual-3.33.0-javadoc.jar",
    "core-3.8.3-javadoc.jar",
    "error_prone_annotations-2.18.0-javadoc.jar",
    "gradle-idea-ext-1.2-javadoc.jar",
    "gradle-idea-ext-1.2-sources.jar",
    "gson-2.10.1-javadoc.jar",
    "j2objc-annotations-2.8-javadoc.jar",
    "jline-reader-3.20.0-javadoc.jar",
    "jline-terminal-3.20.0-javadoc.jar",
    "jsr305-3.0.2-javadoc.jar",
    "maven-artifact-3.8.5-javadoc.jar",
    "mixinextras-neoforge-0.5.3-javadoc.jar",
    "moddev-gradle-2.0.141-javadoc.jar",
    "moddev-gradle-2.0.141-sources.jar",
    "nashorn-core-15.4-javadoc.jar",
    "noexception-1.7.1-javadoc.jar",
    "plexus-utils-3.3.0-javadoc.jar",
    "sponge-mixin-0.15.2+mixin.0.8.7-javadoc.jar",
    "terminalconsoleappender-1.3.0-javadoc.jar",
    "toml-3.8.3-javadoc.jar",
    "typetools-0.6.3-javadoc.jar",
})


def main() -> int:
    metadata = Path(__file__).resolve().parents[1] / "gradle" / "verification-metadata.xml"
    root = element_tree.parse(metadata).getroot()
    namespace = {"verification": "https://schema.gradle.org/dependency-verification"}
    present = {
        artifact.attrib["name"]
        for artifact in root.findall(".//verification:artifact", namespace)
    }
    missing = sorted(REQUIRED_ARTIFACTS - present)
    if missing:
        print("IntelliJ dependency verification metadata is missing:", file=sys.stderr)
        for artifact in missing:
            print(f"  - {artifact}", file=sys.stderr)
        return 1
    print(f"Verified {len(REQUIRED_ARTIFACTS)} IntelliJ-only dependency artifacts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
