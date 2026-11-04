#!/usr/bin/env python3
"""Regression tests for the release preflight: production must fail closed."""
from pathlib import Path
import tempfile
import unittest

from commercial_readiness_check import gradle_block, inspect


GRADLE = """
android {
    defaultConfig { testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner' }
    buildTypes {
        debug {
            applicationIdSuffix '.preview'
        }
        release {
            minifyEnabled true
            shrinkResources true
        }
        releasePreview {
            initWith release
            signingConfig signingConfigs.debug
            applicationIdSuffix '.smooth270'
        }
    }
}
"""
MANIFEST = """<?xml version="1.0"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application android:usesCleartextTraffic="false">
    <activity android:name=".NativeWorkspaceActivity" android:exported="false"/>
  </application>
</manifest>
"""
BACKUP = """<full-backup-content>
    <exclude domain="sharedpref" path="native_license_access.xml"/>
</full-backup-content>"""
EXTRACTION = """<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="native_license_access.xml"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="native_license_access.xml"/>
    </device-transfer>
</data-extraction-rules>"""


class CommercialReadinessCheckTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        for name, value in (
            ("app/build.gradle", GRADLE),
            ("app/src/main/AndroidManifest.xml", MANIFEST),
            ("app/src/main/res/xml/backup_rules.xml", BACKUP),
            ("app/src/main/res/xml/data_extraction_rules.xml", EXTRACTION),
        ):
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(value, encoding="utf-8")

    def test_gradle_block_does_not_include_sibling_release_preview(self):
        release = gradle_block(GRADLE, "release")
        self.assertIn("minifyEnabled true", release)
        self.assertNotIn("signingConfig", release)
        self.assertIn("signingConfig signingConfigs.debug", gradle_block(GRADLE, "releasePreview"))

    def test_current_preview_invariants_are_accepted(self):
        self.assertEqual([], inspect(self.root, "preview", env={}))

    def test_production_is_rejected_without_independent_signing_and_licensing(self):
        failures = inspect(self.root, "production", env={})
        self.assertTrue(any("production signingConfig" in f for f in failures), failures)
        self.assertTrue(any("license server" in f for f in failures), failures)
        self.assertTrue(any("production APK" in f for f in failures), failures)

    def test_accidental_debug_signed_production_is_rejected(self):
        path = self.root / "app/build.gradle"
        path.write_text(GRADLE.replace("minifyEnabled true",
                         "signingConfig signingConfigs.debug\n            minifyEnabled true"), encoding="utf-8")
        failures = inspect(self.root, "preview", env={})
        self.assertTrue(any("debug signing key" in f for f in failures), failures)

    def test_licensing_preferences_must_be_excluded_from_backup(self):
        path = self.root / "app/src/main/res/xml/data_extraction_rules.xml"
        path.write_text("<data-extraction-rules/>", encoding="utf-8")
        failures = inspect(self.root, "preview", env={})
        self.assertTrue(any("license preferences" in f for f in failures), failures)

    def test_missing_apk_fails(self):
        failures = inspect(self.root, "preview", apk=self.root / "missing.apk", env={})
        self.assertTrue(any("Missing APK" in f for f in failures), failures)


if __name__ == "__main__":
    unittest.main()
