#!/usr/bin/env python3
"""Тесты для android_test_report.py."""

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("android_test_report.py")


class AndroidTestReportTest(unittest.TestCase):
    def test_script_when_gradle_exit_code_empty_then_prints_existing_report(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            results_dir = root / "app/build/outputs/androidTest-results/connected/debug"
            results_dir.mkdir(parents=True)
            (results_dir / "TEST-success.xml").write_text(
                """<?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="success" tests="1" failures="0" errors="0">
                    <testcase classname="SuccessTest" name="success"/>
                </testsuite>
                """,
                encoding="utf-8",
            )

            env = os.environ.copy()
            env["ANDROID_TEST_GRADLE_EXIT_CODE"] = ""

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH)],
                cwd=root,
                env=env,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode)
        self.assertIn("Всего тестов: 1", result.stdout)

    def test_script_when_gradle_failed_then_fails_without_reading_stale_results(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            results_dir = root / "app/build/outputs/androidTest-results/connected/debug"
            results_dir.mkdir(parents=True)
            (results_dir / "TEST-stale.xml").write_text(
                """<?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="stale" tests="1" failures="0" errors="0">
                    <testcase classname="StaleTest" name="old_success"/>
                </testsuite>
                """,
                encoding="utf-8",
            )

            env = os.environ.copy()
            env["ANDROID_TEST_GRADLE_EXIT_CODE"] = "1"

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH)],
                cwd=root,
                env=env,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(1, result.returncode)
        self.assertIn("Gradle", result.stdout)


if __name__ == "__main__":
    unittest.main()
