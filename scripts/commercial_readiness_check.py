#!/usr/bin/env python3
"""Non-invasive commercial release preflight for CeilingMeasure Native.

Preview mode validates safety invariants, not commercial fitness.
Production mode intentionally fails until signing and licensing are configured.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile

ANDROID = "{http://schemas.android.com/apk/res/android}"


def gradle_block(source: str, name: str) -> str:
    """Return the body of a named Groovy block, without subsequent sibling blocks."""
    match = re.search(r"(?m)^\s*" + re.escape(name) + r"\s*\{", source)
    if match is None:
        return ""
    depth = 0
    start = match.end()
    for i in range(start - 1, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[start:i]
    return ""


def inspect(root: Path, mode: str, apk: Path | None = None,
            env: dict[str, str] | None = None) -> list[str]:
    """Return failures, never silently convert a commercial failure to preview pass."""
    errors: list[str] = []
    env = os.environ if env is None else env

    def load(path: str) -> str:
        try:
            return (root / path).read_text(encoding="utf-8")
        except OSError as exc:
            errors.append(f"Missing or unreadable {path}: {exc}")
            return ""

    gradle = load("app/build.gradle")
    release = gradle_block(gradle, "release")
    preview = gradle_block(gradle, "releasePreview")
    debug = gradle_block(gradle, "debug")

    for name, ok in (
        ("R8 optimization in release", bool(re.search(r"\bminifyEnabled\s+true\b", release))),
        ("Resource shrinking in release", bool(re.search(r"\bshrinkResources\s+true\b", release))),
        ("Separate debug application ID", bool(re.search(r"applicationIdSuffix\s+['\"]\.preview['\"]", debug))),
        ("Isolated release preview application ID", bool(re.search(r"applicationIdSuffix\s+['\"]\.smooth\d+['\"]", preview))),
        ("Instrumentation test runner", "androidx.test.runner.AndroidJUnitRunner" in gradle),
    ):
        if not ok:
            errors.append(f"Missing invariant: {name}")

    if re.search(r"signingConfig\s+signingConfigs\.debug\b", release):
        errors.append("Production release is configured with the Android debug signing key")
    if not re.search(r"signingConfig\s+signingConfigs\.debug\b", preview):
        errors.append("Preview should be isolated and signed explicitly as a test build")

    manifest_text = load("app/src/main/AndroidManifest.xml")
    if manifest_text:
        try:
            manifest = ET.fromstring(manifest_text)
            application = manifest.find("application")
            if application is None:
                errors.append("AndroidManifest has no application element")
            else:
                if application.attrib.get(ANDROID + "usesCleartextTraffic") != "false":
                    errors.append("Cleartext traffic must be disabled")
                workspace = next((a for a in application.findall("activity")
                                  if a.attrib.get(ANDROID + "name") == ".NativeWorkspaceActivity"), None)
                if workspace is None or workspace.attrib.get(ANDROID + "exported") != "false":
                    errors.append("Workspace activity must remain non-exported")
        except ET.ParseError as exc:
            errors.append(f"Invalid AndroidManifest XML: {exc}")

    for path in ("app/src/main/res/xml/backup_rules.xml",
                 "app/src/main/res/xml/data_extraction_rules.xml"):
        xml = load(path)
        if xml:
            try:
                doc = ET.fromstring(xml)
                excluded = any(e.attrib.get("domain") == "sharedpref" and
                               e.attrib.get("path") == "native_license_access.xml"
                               for e in doc.iter("exclude"))
                if not excluded:
                    errors.append(f"Sensitive license preferences are not excluded from {path}")
            except ET.ParseError as exc:
                errors.append(f"Invalid backup XML {path}: {exc}")

    if apk is not None:
        if not apk.is_file() or apk.stat().st_size == 0:
            errors.append(f"Missing APK: {apk}")
        else:
            try:
                with zipfile.ZipFile(apk) as archive:
                    names = set(archive.namelist())
                    if "AndroidManifest.xml" not in names or "classes.dex" not in names:
                        errors.append("APK is missing its manifest or primary classes.dex")
                    bad = archive.testzip()
                    if bad is not None:
                        errors.append(f"Corrupted APK archive member: {bad}")
            except (OSError, zipfile.BadZipFile) as exc:
                errors.append(f"Invalid APK: {exc}")

    if mode == "production":
        if not re.search(r"signingConfig\s+signingConfigs\.release\b", release):
            errors.append("BLOCKER: dedicated production signingConfig is not configured")
        url = env.get("CEILING_LICENSE_URL", "").strip()
        key = env.get("CEILING_LICENSE_PUBLIC_KEY_B64", "").strip()
        if not url.startswith("https://") or not key:
            errors.append("BLOCKER: HTTPS license server and verification key are not configured")
        if apk is None or "releasePreview" in str(apk):
            errors.append("BLOCKER: provide an actual signed production APK, never a preview")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--mode", choices=("preview", "production"), required=True)
    parser.add_argument("--apk", type=Path)
    args = parser.parse_args()
    errors = inspect(args.root, args.mode, args.apk)
    if errors:
        for error in errors:
            print("FAIL:", error, file=sys.stderr)
        return 1
    if args.mode == "preview":
        print("PASS: preview safety invariants only; commercial release is NOT certified")
    else:
        print("PASS: preliminary static checks only; real-device and signing verification still required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
