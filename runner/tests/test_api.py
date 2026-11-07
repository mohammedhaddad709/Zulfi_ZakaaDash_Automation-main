"""REST API over a real HTTP server on a random port (spec section 7).

Run:  python -m unittest discover -s runner/tests -v

Contract under test: JSON with Cache-Control no-store; writes need the X-Runner header; foreign Host headers are
refused; Basic auth when RUNNER_PASSWORD is set; run/config/schedule/defect routes; run.json is never served.
"""

import base64
import json
import sys
import threading
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from support import LOGIN, RunnerTestCase, login_methods, server  # noqa: E402


class ApiTest(RunnerTestCase):
    def setUp(self):
        super().setUp()
        self.http = server.ExclusiveServer(("127.0.0.1", 0), server.Handler)
        self.http.allowed_hosts = {"127.0.0.1", "localhost"}
        self.base = f"http://127.0.0.1:{self.http.server_port}"
        threading.Thread(target=self.http.serve_forever, daemon=True).start()
        self._auth = server.AUTH_PASSWORD

    def tearDown(self):
        server.AUTH_PASSWORD = self._auth
        self.http.shutdown()
        self.http.server_close()
        super().tearDown()

    def call(self, path, body=None, headers=None, write_header=True):
        request = urllib.request.Request(self.base + path, method="POST" if body is not None else "GET")
        if body is not None:
            request.data = json.dumps(body).encode("utf-8")
            request.add_header("Content-Type", "application/json")
            if write_header:
                request.add_header("X-Runner", "1")
        for key, value in (headers or {}).items():
            request.add_header(key, value)
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                return response.status, response.headers, response.read()
        except urllib.error.HTTPError as error:
            return error.code, error.headers, error.read()

    def json(self, path, body=None, **kwargs):
        status, headers, raw = self.call(path, body, **kwargs)
        return status, json.loads(raw.decode("utf-8")) if raw else None

    def test_api001_page_catalog_and_json_headers(self):
        status, _, page = self.call("/")
        self.assertEqual(status, 200)
        self.assertIn(b"Automation Control Center", page)
        status, headers, raw = self.call("/api/catalog")
        self.assertEqual(status, 200)
        self.assertEqual(headers["Cache-Control"], "no-store")
        self.assertIn("application/json", headers["Content-Type"])
        systems = {s["key"]: s for s in json.loads(raw)["systems"]}
        self.assertEqual(set(systems), {"zulfi", "zakaadash"})
        self.assertEqual(systems["zakaadash"]["maxThreads"], 1)
        self.assertTrue(all(not l["default"] for l in systems["zulfi"]["logins"] if l["secret"]))

    def test_api002_write_header_host_check_and_basic_auth(self):
        self.assertEqual(self.json("/api/runs", {}, write_header=False)[0], 403)
        self.assertEqual(self.json("/api/catalog", headers={"Host": "evil.example"})[0], 403)
        server.AUTH_PASSWORD = "s3cret"
        status, headers, _ = self.call("/api/catalog")
        self.assertEqual(status, 401)
        self.assertIn("Basic", headers["WWW-Authenticate"])
        token = base64.b64encode(f"{server.AUTH_USER}:s3cret".encode()).decode()
        self.assertEqual(self.call("/api/catalog", headers={"Authorization": "Basic " + token})[0], 200)
        bad = base64.b64encode(f"{server.AUTH_USER}:nope".encode()).decode()
        self.assertEqual(self.call("/api/catalog", headers={"Authorization": "Basic " + bad})[0], 401)

    def test_api003_run_routes(self):
        status, run = self.json("/api/runs", {"system": "zulfi", "scope": "classes", "classes": ["LoginRegressionTest"]})
        self.assertEqual((status, run["status"]), (201, "queued"))
        status, detail = self.json(f"/api/runs/{run['id']}")
        self.assertEqual((status, detail["estimated"]), (200, len(login_methods())))
        self.assertIn("log", detail)
        status, listing = self.json("/api/runs")
        self.assertEqual(listing["runs"][0]["id"], run["id"])
        self.assertEqual(listing["queued"], 1)
        self.assertEqual(self.json("/api/runs/nope")[0], 404)
        status, error = self.json("/api/runs", {"system": "zulfi", "scope": "classes"})
        self.assertEqual(status, 400)
        self.assertIn("Select at least one test class", error["error"])
        status, stopped = self.json(f"/api/runs/{run['id']}/stop", {})
        self.assertEqual(stopped["status"], "cancelled")
        self.assertEqual(self.json(f"/api/runs/{run['id']}/metrics", {"count": False})[1]["metrics"], False)
        self.assertEqual(self.json("/api/runs/delete", {"ids": [run["id"]], "mode": "runs"})[1]["deleted"], [run["id"]])

    def test_api004_private_files_are_never_served(self):
        self.add_run("r1", server.now_iso(), [(f"{LOGIN}.{login_methods()[0]}", "passed")])
        folder = server.RUNS_DIR / "r1"
        folder.mkdir(exist_ok=True)
        (folder / "run.json").write_text("{}", encoding="utf-8")
        (folder / "console.log").write_text("hello", encoding="utf-8")
        self.assertEqual(self.call("/runs/r1/console.log")[2], b"hello")
        for path in ("/runs/r1/run.json", "/runs/r1/../../server.py", "/runs/r1/%2e%2e/%2e%2e/server.py"):
            with self.subTest(path=path):
                self.assertEqual(self.call(path)[0], 404)

    def test_api005_defects_bug_report_and_dashboard(self):
        test = f"{LOGIN}.{login_methods()[0]}"
        self.add_run("r1", server.now_iso(), [(test, "failed", "expected [a] but found [b]")])
        status, entry = self.json("/api/bugs/rate", {"run": "r1", "test": test, "severity": "High", "note": "جملة"})
        self.assertEqual((status, entry["id"]), (200, "ZUL-01"))
        status, bugs = self.json("/api/runs/r1/bugs")
        self.assertEqual(bugs["bugs"][0]["id"], "ZUL-01")
        status, headers, data = self.call("/api/runs/r1/bugreport", {"format": "docx", "bugs": bugs["bugs"]})
        self.assertEqual(status, 200)
        self.assertIn("attachment", headers["Content-Disposition"])
        self.assertEqual(data[:2], b"PK")
        status, dash = self.json("/api/dashboard?days=30&system=zulfi")
        self.assertEqual(dash["openDefects"][0]["id"], "ZUL-01")
        self.assertEqual(dash["systems"][0]["health"]["color"], "yellow")
        status, retest = self.json("/api/defects/retest", {"run": "r1", "test": test})
        self.assertEqual((status, retest["scope"]), (201, "retest-one"))
        for path in ("/api/reliability?system=zulfi", "/api/storage", "/api/jobs", "/api/agent", "/api/schedules",
                     "/api/build", "/api/retest/zulfi", "/api/config"):
            with self.subTest(path=path):
                self.assertEqual(self.json(path)[0], 200)

    def test_api006_config_and_schedules(self):
        status, view = self.json("/api/config")
        self.assertTrue(view["editable"])
        spec = next(s for s in view["systems"] if s["key"] == "zulfi")["spec"]
        status, error = self.json("/api/config/zulfi", {**spec, "maxThreads": 9})
        self.assertEqual(status, 400)
        status, view = self.json("/api/config/zulfi", {**spec, "parallelNote": "via API"})
        self.assertEqual(next(s for s in view["systems"] if s["key"] == "zulfi")["spec"]["parallelNote"], "via API")
        status, schedule = self.json("/api/schedules", {"name": "Nightly", "trigger": {"time": "22:00", "days": [0]},
                                                         "run": {"system": "zulfi", "scope": "e2e-only"}})
        self.assertEqual(status, 200)
        self.assertFalse(self.json(f"/api/schedules/{schedule['id']}/toggle", {})[1]["enabled"])
        status, run = self.json(f"/api/schedules/{schedule['id']}/run", {})
        self.assertEqual((status, run["schedule"]["name"]), (201, "Nightly"))
        self.assertEqual(self.json(f"/api/schedules/{schedule['id']}/delete", {})[1], {"deleted": schedule["id"]})


if __name__ == "__main__":
    import unittest
    unittest.main(verbosity=2)
