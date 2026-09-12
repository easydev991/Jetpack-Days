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
            results_dir = (
                root / "app/build/outputs/androidTest-results/connected/rustoreDebug"
            )
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
            results_dir = (
                root / "app/build/outputs/androidTest-results/connected/rustoreDebug"
            )
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

    def test_script_when_variant_arg_supplied_then_reads_matching_dir(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            results_dir = (
                root / "app/build/outputs/androidTest-results/connected/githubDebug"
            )
            results_dir.mkdir(parents=True)
            (results_dir / "TEST-success.xml").write_text(
                """<?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="success" tests="2" failures="0" errors="0">
                    <testcase classname="GithubTest" name="a"/>
                    <testcase classname="GithubTest" name="b"/>
                </testsuite>
                """,
                encoding="utf-8",
            )

            env = os.environ.copy()
            env["ANDROID_TEST_GRADLE_EXIT_CODE"] = ""

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "githubDebug"],
                cwd=root,
                env=env,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode)
        self.assertIn("Всего тестов: 2", result.stdout)

    def test_script_when_variant_arg_unknown_then_fails(self):
        # Без whitelist variant теперь просто ищет несуществующую директорию —
        # и падает на «Директория с результатами... не найдена». Это та же защита
        # от мусорного variant, просто с другим сообщением.
        env = os.environ.copy()
        env["ANDROID_TEST_GRADLE_EXIT_CODE"] = ""

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "playstore"],
            env=env,
            check=False,
            capture_output=True,
            text=True,
        )

        self.assertEqual(1, result.returncode)
        self.assertIn("Директория с результатами", result.stdout)

    def test_script_when_assumption_violated_then_counts_as_skipped_not_failed(self):
        # AndroidJUnitRunner пишет скип через Assume (flavor-гейт) как <failure>
        # с AssumptionViolatedException — прогон при этом должен быть зелёным.
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            results_dir = (
                root / "app/build/outputs/androidTest-results/connected/githubDebug"
            )
            results_dir.mkdir(parents=True)
            (results_dir / "TEST-assume.xml").write_text(
                """<?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="assume" tests="2" failures="0" errors="0">
                    <testcase classname="GateTest" name="a"/>
                    <testcase classname="GateTest" name="b">
                        <failure>org.junit.AssumptionViolatedException: got: &lt;false&gt;, expected: is &lt;true&gt;
	at org.junit.Assume.assumeTrue(Assume.java:50)
	at com.dayscounter.GateTest.b(GateTest.kt:10)
                        </failure>
                    </testcase>
                </testsuite>
                """,
                encoding="utf-8",
            )

            env = os.environ.copy()
            env["ANDROID_TEST_GRADLE_EXIT_CODE"] = ""

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "githubDebug"],
                cwd=root,
                env=env,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(0, result.returncode)
        self.assertIn("Всего тестов: 2", result.stdout)
        self.assertIn("Успешные: 1", result.stdout)
        self.assertIn("Пропущенные (Assume/flavor-гейт): 1", result.stdout)
        self.assertNotIn("ТЕСТЫ ПРОВАЛИЛИСЬ", result.stdout)

    def test_script_when_real_failure_then_fails(self):
        # Обычный failure (не Assume) — прогон красный, как и раньше.
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            results_dir = (
                root / "app/build/outputs/androidTest-results/connected/githubDebug"
            )
            results_dir.mkdir(parents=True)
            (results_dir / "TEST-fail.xml").write_text(
                """<?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="fail" tests="1" failures="1" errors="0">
                    <testcase classname="FailTest" name="a">
                        <failure>java.lang.AssertionError: expected:&lt;1&gt; but was:&lt;2&gt;</failure>
                    </testcase>
                </testsuite>
                """,
                encoding="utf-8",
            )

            env = os.environ.copy()
            env["ANDROID_TEST_GRADLE_EXIT_CODE"] = ""

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "githubDebug"],
                cwd=root,
                env=env,
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(1, result.returncode)
        self.assertIn("ТЕСТЫ ПРОВАЛИЛИСЬ", result.stdout)


if __name__ == "__main__":
    unittest.main()
