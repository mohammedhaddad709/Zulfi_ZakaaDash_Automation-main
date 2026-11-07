"""Shared fixtures for the runner tests: an isolated runner state (temp store, runs folder, quarantine, uptime) and
small builders for runs and Allure results. Never touches runner/runs, bugs.json or the project sources."""

import json
import shutil
import sys
import tempfile
import time
import unittest
import uuid
from pathlib import Path

RUNNER_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RUNNER_DIR))

import config  # noqa: E402
import reliability  # noqa: E402
import schedules  # noqa: E402
import server  # noqa: E402
import store  # noqa: E402

LOGIN = "com.taf.systems.zulfi.features.login.LoginRegressionTest"
HOME = "com.taf.systems.zulfi.features.home.HomeOverviewAcceptanceTest"


def login_methods():
    cls = server.class_entry("zulfi", "LoginRegressionTest")
    return [m["name"] for m in cls["methods"]]


class RunnerTestCase(unittest.TestCase):
    """Each test gets its own store, runs folder, defect registry, quarantine and schedules."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="runner-test-"))
        self._saved = {name: getattr(server, name) for name in ("RUNS_DIR", "STORE", "UPTIME_FILE", "after_run")}
        self._runs = dict(server.RUNS)
        self._transient = dict(server.TRANSIENT)
        server.RUNS.clear()
        server.TRANSIENT.clear()
        server.RUNS_DIR = self.tmp / "runs"
        server.RUNS_DIR.mkdir()
        server.STORE = store.FileStore(self.tmp)
        server.UPTIME_FILE = self.tmp / "uptime.jsonl"
        server.after_run = lambda run: None  # background hooks are tested on their own
        config.load(server.STORE, set(server.SYSTEMS))
        reliability.load(self.tmp)
        schedules.load(server.STORE)
        server.DEFECTS.clear()

    def tearDown(self):
        for name, value in self._saved.items():
            setattr(server, name, value)
        server.RUNS.clear()
        server.RUNS.update(self._runs)
        server.TRANSIENT.clear()
        server.TRANSIENT.update(self._transient)
        server.DEFECTS.clear()
        shutil.rmtree(self.tmp, ignore_errors=True)

    # ------------------------------------------------------------------ builders
    def add_run(self, run_id, created, tests, system="zulfi", status=None, **extra):
        """A finished run with summary tests [(fullName, status, message, extra dict)]."""
        rows = []
        for item in tests:
            full_name, state = item[0], item[1]
            message = item[2] if len(item) > 2 else ""
            more = item[3] if len(item) > 3 else {}
            rows.append({"name": full_name.rsplit(".", 1)[-1], "fullName": full_name, "status": state,
                         "soft": False, "message": message, "duration": 1.0, "shot": None, "start": 0,
                         "retried": 0, "flaky": False, "testId": "", "title": full_name.rsplit(".", 1)[-1], **more})
        counts = {s: sum(1 for t in rows if t["status"] == s) for s in ("passed", "failed", "broken", "skipped")}
        counts.update(bugChecks=0, flaky=sum(1 for t in rows if t["flaky"]), setupFailures=0)
        if status is None:
            status = "finished with failures" if counts["failed"] + counts["broken"] else "passed"
        run = {"id": run_id, "system": system, "env": "current", "status": status, "created": created,
               "started": created, "finished": created, "scope": "classes", "metrics": True,
               "urls": {"Portal URL": "https://zulfi.example.test/"}, "execution": "LocalHeadless",
               "summary": {"counts": counts, "total": len(rows), "tests": rows}, **extra}
        server.RUNS[run_id] = run
        return run


def allure_result(folder, cls, method, status, start, history=None, message="", links=(), labels=(), attachments=()):
    data = {"uuid": str(uuid.uuid4()), "historyId": history or f"{cls}.{method}", "fullName": f"{cls}.{method}",
            "name": method, "status": status, "start": start, "stop": start + 2000,
            "statusDetails": {"message": message, "trace": (message.split(":")[0] + ": boom") if message else ""},
            "labels": [{"name": "testClass", "value": cls}, {"name": "testMethod", "value": method}, *labels],
            "links": list(links), "attachments": list(attachments), "steps": []}
    folder.mkdir(parents=True, exist_ok=True)
    (folder / f"{data['uuid']}-result.json").write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    return data


def wait_for(condition, timeout=20.0, step=0.05):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        value = condition()
        if value:
            return value
        time.sleep(step)
    raise AssertionError("condition not reached in time")
