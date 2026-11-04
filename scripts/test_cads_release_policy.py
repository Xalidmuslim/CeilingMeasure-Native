#!/usr/bin/env python3
"""Fail-closed policy tests for the active v270 preview release workflow.

Static policy guard, not a replacement for running Android tests.
"""
import re
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent
WORKFLOWS = ROOT / ".github" / "workflows"


class CADSReleasePolicyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.release = (WORKFLOWS / "ceiling-v270-auto-ceiling.yml").read_text(encoding="utf-8")
        cls.quality = (WORKFLOWS / "commercial-quality-gate.yml").read_text(encoding="utf-8")

    def test_release_runs_on_pull_requests_for_active_branch(self):
        self.assertRegex(self.release, r"(?m)^  pull_request:\s*$")

    def test_full_instrumentation_is_run_before_publishing(self):
        # A selected '-e class' list can mask other failing tests.
        self.assertIn("adb shell am instrument -w -r", self.release)
        self.assertNotIn("-e class", self.release)
        self.assertIn("grep -Eq '^OK", self.release)
        self.assertIn("needs: test-build-preview", self.release)
        self.assertNotIn("continue-on-error: true", self.release)

    def test_release_job_never_publishes_on_pull_requests(self):
        self.assertRegex(
            self.release,
            r"(?ms)^  publish-v270:\s*\n(?:(?!^  [A-Za-z0-9_-]+:).)*?if:\s*github\.event_name != 'pull_request'"
        )
        self.assertIn("prerelease: true", self.release)

    def test_failure_logs_are_retained(self):
        self.assertIn("Retain full test evidence", self.release)
        self.assertIn("if: always()", self.release)

    def test_quality_workflow_requires_full_android_suite(self):
        self.assertIn("Full native Android regression suite", self.quality)
        self.assertIn("script: bash scripts/run_android_smoke.sh", self.quality)

    def test_security_and_dependency_scanners_are_configured(self):
        codeql = (WORKFLOWS / "cads-codeql.yml").read_text(encoding="utf-8")
        dependabot = (ROOT / ".github" / "dependabot.yml").read_text(encoding="utf-8")
        self.assertIn("github/codeql-action/init@v4", codeql)
        self.assertIn("github/codeql-action/analyze@v4", codeql)
        self.assertIn("java-kotlin", codeql)
        self.assertIn("security-events: write", codeql)
        self.assertIn("package-ecosystem: gradle", dependabot)
        self.assertIn("package-ecosystem: github-actions", dependabot)


if __name__ == "__main__":
    unittest.main()
