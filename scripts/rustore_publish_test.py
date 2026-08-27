"""Smoke-test scripts/rustore_publish.sh: 4-шаговый happy-path с fake curl.

Проверяет только happy path — что скрипт проходит все 4 шага (auth →
create-draft → upload → submit) без ошибок при подменённом curl.
Реальные сетевые контракты RuStore не проверяются — для этого нужен
sandbox или test-mode у RuStore.

Подменяется ТОЛЬКО curl (через PATH). openssl/jq остаются реальными —
они нужны для подписи (openssl dgst -sign) и парсинга JSON (jq).
"""

import base64
import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SCRIPT = REPO_ROOT / "scripts" / "rustore_publish.sh"

FAKE_CURL_BODY = """\
#!/usr/bin/env python3
import json, os, sys
url = next(a for a in sys.argv[1:] if a.startswith("http"))
with open(os.environ["FAKE_CURL_LOG"], "a") as f:
    f.write(url + "\\n")
if url.endswith("/public/auth/"):
    print(json.dumps({"body": {"jwe": "fake.jwe.token"}}))
elif "/aab" in url:
    pass
elif "/commit" in url:
    pass
else:
    print(json.dumps({"body": 12345}))
"""


class RustorePublishHappyPathTest(unittest.TestCase):
    """Скрипт отрабатывает все 4 шага и завершается с exit 0."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.fake_bin = self.tmp / "bin"
        self.fake_bin.mkdir()
        self.cwd = self.tmp / "project"
        self.cwd.mkdir()
        self.log = self.tmp / "curl.log"

        # Whats-new файл — скрипт требует его наличие (line 54-58).
        version_name = "9.9.9-smoke"
        (self.cwd / "gradle.properties").write_text(f"VERSION_NAME={version_name}\n")
        whats_new_dir = (
            self.cwd / "fastlane" / "metadata" / "android" / "ru-RU" / "whats_new"
        )
        whats_new_dir.mkdir(parents=True)
        (whats_new_dir / f"{version_name}.txt").write_text("Тестовый релиз\n")

        # Fake AAB (скрипт только проверяет существование).
        self.aab = self.cwd / "app.aab"
        self.aab.write_bytes(b"fake-aab-content")

        # Реальный RSA private key (PKCS8, base64) — openssl его примет
        # при подписи. openssl отклоняет слишком короткие ключи, поэтому
        # генерируем 2048-bit в setUp. Это занимает ~0.5с.
        genpkey = subprocess.run(
            [
                "openssl",
                "genpkey",
                "-algorithm",
                "RSA",
                "-pkeyopt",
                "rsa_keygen_bits:2048",
            ],
            capture_output=True,
            check=True,
        )
        pkcs8 = subprocess.run(
            ["openssl", "pkcs8", "-topk8", "-nocrypt"],
            input=genpkey.stdout,
            capture_output=True,
            check=True,
        )
        creds = {
            "key_id": "test-key-id",
            "client_secret": base64.b64encode(pkcs8.stdout).decode(),
        }
        self.creds = self.cwd / "creds.json"
        self.creds.write_text(json.dumps(creds))

        # Fake curl через PATH — логирует URL и отвечает заготовленным JSON.
        curl = self.fake_bin / "curl"
        curl.write_text(FAKE_CURL_BODY)
        curl.chmod(0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_happy_path(self):
        env = os.environ.copy()
        env["PATH"] = f"{self.fake_bin}{os.pathsep}{env['PATH']}"
        env["FAKE_CURL_LOG"] = str(self.log)

        result = subprocess.run(
            [str(SCRIPT), str(self.creds), str(self.aab), "0"],
            cwd=self.cwd,
            env=env,
            capture_output=True,
            text=True,
        )

        self.assertEqual(
            result.returncode,
            0,
            f"exit={result.returncode}\n"
            f"stdout={result.stdout!r}\nstderr={result.stderr!r}",
        )

        urls = self.log.read_text().splitlines()
        self.assertEqual(len(urls), 4, f"expected 4 curl calls, got {urls}")
        self.assertTrue(urls[0].endswith("/public/auth/"), urls[0])
        self.assertIn("/version", urls[1])
        self.assertIn("/aab", urls[2])
        self.assertIn("/commit", urls[3])


class RustorePublishReleaseNotesMissingTest(unittest.TestCase):
    """Если release notes для VERSION_NAME отсутствует — exit != 0.

    Защита от регрессии: скрипт не должен молча слать пустой whatsNew
    или падать с непонятной ошибкой на шаге create-draft.
    """

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.cwd = self.tmp / "project"
        self.cwd.mkdir()
        self.version_name = "9.9.9-smoke"
        (self.cwd / "gradle.properties").write_text(
            f"VERSION_NAME={self.version_name}\n"
        )
        self.creds = self.cwd / "creds.json"
        self.creds.write_text(json.dumps({"key_id": "k", "client_secret": "c"}))
        self.aab = self.cwd / "app.aab"
        self.aab.write_bytes(b"fake")
        # Намеренно НЕ создаём fastlane/metadata/.../whats_new/<VERSION>.txt.

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_missing_release_notes_exits_with_error(self):
        result = subprocess.run(
            [str(SCRIPT), str(self.creds), str(self.aab)],
            cwd=self.cwd,
            capture_output=True,
            text=True,
        )
        self.assertNotEqual(
            result.returncode,
            0,
            f"ожидался exit != 0, got {result.returncode}\n"
            f"stdout={result.stdout!r}\nstderr={result.stderr!r}",
        )
        self.assertIn(
            "Файл release notes не найден",
            result.stderr,
            f"stderr должен содержать сообщение о release notes, got {result.stderr!r}",
        )
        self.assertIn(
            self.version_name,
            result.stderr,
            f"stderr должен содержать VERSION_NAME, got {result.stderr!r}",
        )


class _ModeTestBase(unittest.TestCase):
    """Общий setup для upload/commit mode тестов."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.fake_bin = self.tmp / "bin"
        self.fake_bin.mkdir()
        self.cwd = self.tmp / "project"
        self.cwd.mkdir()
        self.log = self.tmp / "curl.log"
        version_name = "9.9.9-smoke"
        (self.cwd / "gradle.properties").write_text(f"VERSION_NAME={version_name}\n")
        whats_new_dir = (
            self.cwd / "fastlane" / "metadata" / "android" / "ru-RU" / "whats_new"
        )
        whats_new_dir.mkdir(parents=True)
        (whats_new_dir / f"{version_name}.txt").write_text("Тестовый релиз\n")
        # Реальный RSA private key (PKCS8, base64) — иначе openssl в шаге 1
        # auth падает с "Could not find private key". Генерируем 2048-bit в
        # setUp (~0.5с). Тот же подход что и в happy_path тесте.
        genpkey = subprocess.run(
            [
                "openssl",
                "genpkey",
                "-algorithm",
                "RSA",
                "-pkeyopt",
                "rsa_keygen_bits:2048",
            ],
            capture_output=True,
            check=True,
        )
        pkcs8 = subprocess.run(
            ["openssl", "pkcs8", "-topk8", "-nocrypt"],
            input=genpkey.stdout,
            capture_output=True,
            check=True,
        )
        creds = {
            "key_id": "test-key-id",
            "client_secret": base64.b64encode(pkcs8.stdout).decode(),
        }
        self.creds = self.cwd / "creds.json"
        self.creds.write_text(json.dumps(creds))
        self.aab = self.cwd / "app.aab"
        self.aab.write_bytes(b"fake-aab")
        curl = self.fake_bin / "curl"
        curl.write_text(FAKE_CURL_BODY)
        curl.chmod(0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _run(self, extra_env):
        env = os.environ.copy()
        env["PATH"] = f"{self.fake_bin}{os.pathsep}{env['PATH']}"
        env["FAKE_CURL_LOG"] = str(self.log)
        env.update(extra_env)
        return subprocess.run(
            [str(SCRIPT), str(self.creds), str(self.aab), "0"],
            cwd=self.cwd,
            env=env,
            capture_output=True,
            text=True,
        )


class RustorePublishUploadModeTest(_ModeTestBase):
    """RUSTORE_MODE=upload: шаги 1-3 (auth+create-draft+upload AAB), без commit."""

    def test_upload_mode_skips_commit(self):
        result = self._run({"RUSTORE_MODE": "upload"})

        self.assertEqual(
            result.returncode,
            0,
            f"exit={result.returncode}\n"
            f"stdout={result.stdout!r}\nstderr={result.stderr!r}",
        )
        urls = self.log.read_text().splitlines()
        self.assertEqual(len(urls), 3, f"expected 3 curl calls, got {urls}")
        self.assertTrue(urls[0].endswith("/public/auth/"), urls[0])
        self.assertIn("/version", urls[1])
        self.assertIn("/aab", urls[2])
        self.assertFalse(
            any("/commit" in u for u in urls),
            f"upload mode не должен вызывать /commit, got {urls}",
        )

    def test_upload_mode_writes_vid_to_file(self):
        """После успешного create-draft VID записывается в .secrets/.last_rustore_vid.

        Makefile `rustore-draft` читает этот файл и подставляет VID в подсказку
        про `make rustore-commit VID=<vid>`. Если файл не создан — пользователь
        должен идти в Console искать VID руками.
        """
        result = self._run({"RUSTORE_MODE": "upload"})

        self.assertEqual(
            result.returncode,
            0,
            f"exit={result.returncode}\n"
            f"stdout={result.stdout!r}\nstderr={result.stderr!r}",
        )
        vid_file = self.cwd / ".secrets" / ".last_rustore_vid"
        self.assertTrue(
            vid_file.exists(),
            f"VID-файл должен быть создан после create-draft: {vid_file}",
        )
        self.assertEqual(
            vid_file.read_text(),
            "12345",
            f"VID-файл должен содержать VID из create-draft ответа",
        )


class RustorePublishCommitModeTest(_ModeTestBase):
    """RUSTORE_MODE=commit: только шаг 4 (commit), требует RUSTORE_VID."""

    def test_commit_mode_requires_vid(self):
        # Нет RUSTORE_VID — должен упасть до curl.
        result = self._run({"RUSTORE_MODE": "commit"})

        self.assertNotEqual(
            result.returncode,
            0,
            f"exit={result.returncode}\nstderr={result.stderr!r}",
        )
        self.assertIn(
            "RUSTORE_VID",
            result.stderr,
            f"stderr должен упоминать RUSTORE_VID, got {result.stderr!r}",
        )
        self.assertFalse(
            self.log.exists(),
            "без RUSTORE_VID curl не должен вызываться вообще",
        )

    def test_commit_mode_calls_only_auth_and_commit(self):
        result = self._run({"RUSTORE_MODE": "commit", "RUSTORE_VID": "12345"})

        self.assertEqual(
            result.returncode,
            0,
            f"exit={result.returncode}\n"
            f"stdout={result.stdout!r}\nstderr={result.stderr!r}",
        )
        urls = self.log.read_text().splitlines()
        self.assertEqual(len(urls), 2, f"expected 2 curl calls, got {urls}")
        self.assertTrue(urls[0].endswith("/public/auth/"), urls[0])
        self.assertIn("/commit", urls[1])
        self.assertIn("12345", urls[1], "VID должен попасть в URL commit")
        # create-draft (POST /version без /aab) НЕ должен вызываться
        create_draft_calls = [
            u
            for u in urls[1:]
            if "/version" in u and "/aab" not in u and "/commit" not in u
        ]
        self.assertEqual(
            create_draft_calls,
            [],
            f"commit mode не должен слать create-draft, got {create_draft_calls}",
        )


if __name__ == "__main__":
    unittest.main()
