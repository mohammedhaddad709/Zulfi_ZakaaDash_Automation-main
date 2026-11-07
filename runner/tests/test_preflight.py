"""Test cases for the runner's environment preflight, including the "Authentication - not verified" check.

Run:  python -m unittest discover -s runner/tests -v

Contract under test (runner/server.py):
  * preflight(run) -> {"ok", "lines", "checks"}: each URL must answer with HTTP < 500 (a 404 still means the host is up).
  * It never claims authentication was verified: that entry always has ok=None ("not verified"), because neither
    system exposes an API sign-in contract. UI login is exercised by the tests themselves.
  * A Selenium Grid run also needs a ready Grid (/status).
  * An unreachable environment ends the run as "environment down" before any test process starts.
  * Credentials typed in the UI are never persisted and never passed as -D properties (environment only).
"""

import json
import socket
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from support import RUNNER_DIR, RunnerTestCase, server  # noqa: E402

AUTH_LABEL = "Authentication"
GRID_READY = {"ready": True, "nodes": [{"id": "node-1"}]}


class StubHandler(BaseHTTPRequestHandler):
    """Answers with the status encoded in the path: /status/<code>; /status imitates Selenium Grid."""

    def do_GET(self):
        if self.path == "/status":
            body = json.dumps({"value": {"ready": GRID_READY["ready"], "nodes": GRID_READY["nodes"]}}).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        code = int(self.path.rsplit("/", 1)[-1]) if self.path.startswith("/status/") else 200
        self.send_response(code)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, *args):
        pass


def closed_port():
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def make_run(*urls):
    return {"id": "pf-test", "system": "zulfi", "urls": {f"URL {i}": u for i, u in enumerate(urls, 1)},
            "status": "building", "message": "", "progress": {}, "created": "2000-01-01T00:00:00",
            "execution": "LocalHeadless", "preflight": True, "envRetry": 99}


def auth_entry(checks):
    return next(c for c in checks if c["label"] == AUTH_LABEL)


class PreflightTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.stub = HTTPServer(("127.0.0.1", 0), StubHandler)
        cls.base = f"http://127.0.0.1:{cls.stub.server_port}"
        threading.Thread(target=cls.stub.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.stub.shutdown()
        cls.stub.server_close()

    def run_preflight(self, *urls):
        result = server.preflight(make_run(*urls))
        return result["ok"], result["checks"], result["lines"]

    def grid_run(self, port):
        run = make_run(self.base + "/status/200")
        run.update(execution="Remote", grid={"host": "127.0.0.1", "port": str(port)})
        return run

    def test_pf001_reachable_url_passes_and_reports_status(self):
        ok, checks, lines = self.run_preflight(self.base + "/status/200")
        self.assertTrue(ok)
        self.assertEqual((checks[0]["ok"], checks[0]["status"]), (True, 200))
        self.assertIn("OK: the 1 URL(s) answer", lines)

    def test_pf002_client_error_status_still_counts_as_reachable(self):
        ok, checks, _ = self.run_preflight(self.base + "/status/404")
        self.assertTrue(ok, "a 404 means the host answered; login pages often live under other paths")
        self.assertEqual(checks[0]["status"], 404)

    def test_pf003_server_error_status_fails_preflight(self):
        for code in (500, 502, 503):
            with self.subTest(code=code):
                ok, checks, lines = self.run_preflight(self.base + f"/status/{code}")
                self.assertFalse(ok)
                self.assertFalse(checks[0]["ok"])
                self.assertEqual(checks[0]["status"], code)
                self.assertTrue(any(l.startswith("DOWN") and str(code) in l for l in lines))

    def test_pf004_unreachable_host_fails_with_a_readable_reason(self):
        ok, checks, _ = self.run_preflight(f"http://127.0.0.1:{closed_port()}/")
        self.assertFalse(ok)
        self.assertFalse(checks[0]["ok"])
        self.assertIsNone(checks[0]["status"])
        self.assertTrue(checks[0]["detail"], "failure must say why (e.g. connection refused)")

    def test_pf005_authentication_entry_is_always_present_and_never_verified(self):
        outcomes = {"reachable": self.base + "/status/200", "server error": self.base + "/status/503",
                    "unreachable": f"http://127.0.0.1:{closed_port()}/"}
        for name, url in outcomes.items():
            with self.subTest(outcome=name):
                _, checks, _ = self.run_preflight(url)
                entry = auth_entry(checks)
                self.assertIsNone(entry["ok"], "authentication must never be reported as verified or failed")
                self.assertIn("Not verified", entry["detail"])
                self.assertIs(checks[-1], entry, "the authentication note is listed after the URL checks")

    def test_pf006_one_bad_url_fails_overall_but_every_url_is_reported(self):
        ok, checks, _ = self.run_preflight(self.base + "/status/200", self.base + "/status/500")
        self.assertFalse(ok)
        self.assertEqual([c["ok"] for c in checks if c["label"] != AUTH_LABEL], [True, False])

    def test_pf007_authentication_note_does_not_affect_overall_result(self):
        ok, checks, _ = self.run_preflight(self.base + "/status/200")
        self.assertTrue(ok, "the 'not verified' note is informational, not a failure")
        self.assertEqual(len(checks), 2)

    def test_pf008_ui_text_matches_the_backend_contract(self):
        page = (RUNNER_DIR / "static" / "index.html").read_text(encoding="utf-8")
        self.assertIn("not verified", page)
        self.assertIn("c.ok === true", page)
        self.assertIn("c.ok === false", page)

    def test_pf011_grid_run_passes_when_grid_is_ready(self):
        GRID_READY.update(ready=True, nodes=[{"id": "n1"}, {"id": "n2"}])
        result = server.preflight(self.grid_run(self.stub.server_port))
        grid = next(c for c in result["checks"] if c["label"] == "Selenium Grid")
        self.assertTrue(result["ok"])
        self.assertIn("2 node(s)", grid["detail"])

    def test_pf012_grid_run_fails_when_no_grid_is_listening(self):
        result = server.preflight(self.grid_run(closed_port()))
        grid = next(c for c in result["checks"] if c["label"] == "Selenium Grid")
        self.assertFalse(result["ok"], "a Grid run must not start when the Grid is down")
        self.assertIn("Start a Grid", grid["detail"])

    def test_pf013_grid_run_fails_when_grid_is_not_ready(self):
        GRID_READY.update(ready=False, nodes=[])
        try:
            result = server.preflight(self.grid_run(self.stub.server_port))
            self.assertFalse(result["ok"])
            self.assertIn("not ready", next(c for c in result["checks"] if c["label"] == "Selenium Grid")["detail"])
        finally:
            GRID_READY.update(ready=True, nodes=[{"id": "node-1"}])

    def test_pf014_local_runs_do_not_check_the_grid(self):
        result = server.preflight(make_run(self.base + "/status/200"))
        self.assertNotIn("Selenium Grid", [c["label"] for c in result["checks"]])


class RunLifecycleTests(RunnerTestCase):
    def test_pf009_unreachable_environment_ends_run_as_environment_down_before_any_test_starts(self):
        original = server.build_snapshot
        server.build_snapshot = lambda reason="run", force=False: "fake-snapshot"
        try:
            run = make_run(f"http://127.0.0.1:{closed_port()}/")
            run["id"] = "pf009"
            server.RUNS[run["id"]] = run
            server.execute(run)
        finally:
            server.build_snapshot = original
        self.assertEqual(run["status"], "environment down")
        self.assertIn("preflight", run["error"].lower())
        self.assertNotIn("process", server.TRANSIENT.get(run["id"], {}))
        self.assertFalse((server.RUNS_DIR / run["id"] / "console.log").exists(), "no test process may have started")
        self.assertIsNone(auth_entry(run["preflightResult"]["checks"])["ok"])
        saved = json.loads((server.RUNS_DIR / run["id"] / "run.json").read_text(encoding="utf-8"))
        self.assertEqual(saved["status"], "environment down")

    def test_pf010_credentials_from_the_form_are_never_written_or_passed_as_properties(self):
        secret_user, secret_password = "preflight.user@example.test", "Sup3r-S3cret-Pass!"
        run = server.create_run({"system": "zulfi", "env": "current", "scope": "classes",
                                 "classes": ["LoginRegressionTest"], "execution": "LocalHeadless", "pace": "fast",
                                 "logins": {"ZULFI_USER": secret_user, "ZULFI_PASSWORD": secret_password}})
        self.assertEqual(run["credentialOverrides"], ["ZULFI_PASSWORD", "ZULFI_USER"])
        for path in (server.RUNS_DIR / run["id"]).rglob("*"):
            if path.is_file():
                text = path.read_text(encoding="utf-8", errors="replace")
                self.assertNotIn(secret_password, text, f"password leaked into {path.name}")
                self.assertNotIn(secret_user, text, f"user leaked into {path.name}")
        self.assertEqual(server.TRANSIENT[run["id"]]["secrets"]["ZULFI_PASSWORD"], secret_password,
                         "kept in memory for the child environment only")
        props = server.jvm_properties(run, server.RUNS_DIR / run["id"] / "output")
        self.assertNotIn(secret_password, " ".join(f"{k}={v}" for k, v in props.items()))
        self.assertEqual(server.masked(f"x {secret_password} y", [secret_password]), "x **** y")


if __name__ == "__main__":
    unittest.main(verbosity=2)
