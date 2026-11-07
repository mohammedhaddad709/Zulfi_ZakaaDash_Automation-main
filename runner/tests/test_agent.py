"""Claude Code integration and the QA orchestrator, driven by a fake Claude CLI (tests/fake_claude.py).

Run:  python -m unittest discover -s runner/tests -v

Contract under test (runner/agent.py, runner/orchestrator.py; spec sections 11 and 12.5):
  * Requests run in a private copy of a project; credentials files and secret env vars never reach Claude.
  * Events, session, cost and changed files are captured; the diff is exact; merge has a conflict check and a backup,
    undo-merge restores the files; read-only requests discard their copy.
  * Jobs run their steps in order, wait for approval where required, merge on approve, redo with a note, skip,
    cancel, retry a failed step, triage up to two rounds, produce reports and e-mail a summary.
"""

import json
import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

TESTS = Path(__file__).resolve().parent
sys.path.insert(0, str(TESTS))
from support import wait_for  # noqa: E402,F401  (sets sys.path for the runner modules)

import agent  # noqa: E402
import orchestrator  # noqa: E402
import schedules  # noqa: E402

ORIGINAL_DEMO = "class DemoTest {}\n"


class AgentSandbox(unittest.TestCase):
    """A throw-away project + Claude work folder; agent module pointed at them and at the fake CLI."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="agent-test-"))
        self.project = self.tmp / "project"
        demo = self.project / "src" / "test" / "java" / "com" / "demo"
        demo.mkdir(parents=True)
        (demo / "DemoTest.java").write_text(ORIGINAL_DEMO, encoding="utf-8")
        (self.project / "pom.xml").write_text("<project/>", encoding="utf-8")
        (self.project / "AGENTS.md").write_text("rules", encoding="utf-8")
        (self.project / "config").mkdir()
        (self.project / "config" / "credentials.local.properties").write_text("ZULFI_PASSWORD=real-secret", encoding="utf-8")
        self.saved = {k: getattr(agent, k) for k in ("ROOT", "WORK", "STATE_DIR", "CLAUDE")}
        agent.ROOT, agent.WORK, agent.STATE_DIR = self.project, self.tmp / "work", self.tmp / "state"
        agent.CLAUDE = str(TESTS / "fake_claude.py")
        agent._requests.clear()
        agent._events.clear()
        self.calls = self.tmp / "calls.jsonl"
        os.environ["FAKE_CLAUDE_LOG"] = str(self.calls)
        os.environ["ZULFI_PASSWORD"] = "must-not-reach-claude"
        os.environ.pop("FAKE_CLAUDE_FAIL", None)

    def tearDown(self):
        for key, value in self.saved.items():
            setattr(agent, key, value)
        agent._requests.clear()
        agent._events.clear()
        for key in ("FAKE_CLAUDE_LOG", "ZULFI_PASSWORD", "FAKE_CLAUDE_FAIL"):
            os.environ.pop(key, None)
        shutil.rmtree(self.tmp, ignore_errors=True)

    def finished(self, request_id):
        return wait_for(lambda: (agent.get(request_id) or {}).get("status") in agent.TERMINAL and agent.get(request_id))

    def logged_calls(self):
        return [json.loads(line) for line in self.calls.read_text(encoding="utf-8").splitlines()] if self.calls.exists() else []

    @property
    def demo(self):
        return self.project / "src" / "test" / "java" / "com" / "demo" / "DemoTest.java"


class AgentTest(AgentSandbox):
    def test_agt001_code_request_captures_events_session_cost_and_changes(self):
        request = self.finished(agent.create("zulfi", "أصلح الاختبار", mode="code", system_title="Zulfi Portal")["id"])
        self.assertEqual(request["status"], "done")
        self.assertTrue(request["sessionId"])
        self.assertEqual((request["costUsd"], request["turns"]), (0.0125, 3))
        self.assertIn("تم التعديل", request["result"])
        self.assertEqual(sorted((c["status"], Path(c["path"]).name) for c in request["changed"]),
                         [("added", "NewFlowTest.java"), ("modified", "DemoTest.java")])
        kinds = {e["type"] for e in agent.detail(request["id"])["eventsAfter"]}
        self.assertTrue({"user", "system", "text", "tool", "result", "end"} <= kinds)
        call = self.logged_calls()[0]
        self.assertFalse(call["secretVisible"], "credentials are removed from Claude's environment")
        self.assertIn("acceptEdits", call["args"])
        self.assertIn("--append-system-prompt", call["args"])
        self.assertFalse((Path(request["worktree"]) / "config" / "credentials.local.properties").exists())

    def test_agt002_diff_merge_and_undo(self):
        request = self.finished(agent.create("zulfi", "edit", mode="code")["id"])
        diff = agent.diff(request["id"])
        self.assertIn("+// edited by fake claude", diff["diff"])
        self.assertIn("NewFlowTest.java", diff["diff"])
        result = agent.merge(request["id"])
        self.assertTrue(result["ok"])
        self.assertIn("edited by fake claude", self.demo.read_text(encoding="utf-8"))
        self.assertTrue((self.demo.parent / "NewFlowTest.java").exists())
        with self.assertRaises(agent.AgentError):
            agent.merge(request["id"])
        agent.undo_merge(request["id"])
        self.assertEqual(self.demo.read_text(encoding="utf-8"), ORIGINAL_DEMO)
        self.assertFalse((self.demo.parent / "NewFlowTest.java").exists())

    def test_agt003_conflict_is_reported_unless_forced(self):
        request = self.finished(agent.create("zulfi", "edit", mode="code")["id"])
        self.demo.write_text("class DemoTest { /* changed meanwhile */ }\n", encoding="utf-8")
        result = agent.merge(request["id"])
        self.assertFalse(result["ok"])
        self.assertEqual([Path(p).name for p in result["conflicts"]], ["DemoTest.java"])
        self.assertTrue(agent.merge(request["id"], force=True)["forced"])
        agent.undo_merge(request["id"])
        self.assertIn("changed meanwhile", self.demo.read_text(encoding="utf-8"), "undo restores the pre-merge file")

    def test_agt004_read_only_requests_discard_their_copy(self):
        request = self.finished(agent.create("zulfi", "analyse", mode="read")["id"])
        self.assertEqual((request["status"], request["changed"]), ("done", []))
        self.assertFalse(Path(request["worktree"]).exists())
        args = self.logged_calls()[0]["args"]
        self.assertIn("dontAsk", args)
        self.assertIn("Edit,Write,NotebookEdit,Bash", args)

    def test_agt005_failure_reply_resume_and_safety(self):
        failed = self.finished(agent.create("zulfi", "FAIL_PLEASE", mode="code")["id"])
        self.assertEqual(failed["status"], "failed")
        self.assertIn("error_during_execution", failed["error"])
        done = self.finished(agent.create("zulfi", "edit", mode="code")["id"])
        agent.reply(done["id"], "أضف تعليقًا")
        wait_for(lambda: len(self.logged_calls()) == 3)
        resumed = self.finished(done["id"])
        self.assertEqual(resumed["status"], "done")
        self.assertEqual(self.logged_calls()[-1]["args"][self.logged_calls()[-1]["args"].index("--resume") + 1], done["sessionId"])
        for path in ("config/credentials.local.properties", "runner/server.py", "../outside.txt"):
            with self.subTest(path=path), self.assertRaises(agent.AgentError):
                agent._safe_target(path)

    def test_agt006_requests_left_running_are_interrupted_on_load(self):
        folder = agent.STATE_DIR / "old-1"
        folder.mkdir(parents=True)
        (folder / "request.json").write_text(json.dumps({"id": "old-1", "status": "running", "created": "x"}), encoding="utf-8")
        agent.load()
        self.assertEqual(agent.get("old-1")["status"], "interrupted")


class FakeServices:
    def __init__(self, failures=True):
        self.runs, self.mails, self.created, self.failures = {}, [], [], failures

    def system_exists(self, system):
        return system == "zulfi"

    def system_title(self, system):
        return "Zulfi Portal"

    def clean_emails(self, emails):
        return schedules.clean_emails(emails)

    def refresh_catalog(self):
        pass

    def class_exists(self, system, name):
        return name in ("DemoTest", "NewFlowTest")

    def classes_request(self, system, classes, env=None):
        return {"scope": "classes", "classes": classes}

    def scope_request(self, system, scope, env=None):
        return {"scope": scope}

    def retest_request(self, system, env=None):
        return {"scope": "retest-tests"}

    def rerun_failed_request(self, run_id):
        return {"scope": "retest-tests", "from": run_id}

    def create_run(self, payload):
        run_id = f"run{len(self.created) + 1}"
        self.created.append(payload)
        tests = [{"fullName": "com.demo.DemoTest.check", "name": "check", "status": "failed", "message": "boom"}] if self.failures else []
        self.runs[run_id] = {"id": run_id, "status": "finished with failures" if self.failures else "passed",
                             "summary": {"counts": {}, "tests": tests}}
        return {"id": run_id}

    def run_state(self, run_id):
        return self.runs.get(run_id)

    def stop_run(self, run_id):
        pass

    def run_markdown(self, run, label):
        return f"{label}: {run['status']}"

    def run_failures(self, run_id):
        return [dict(t, kind="assertion") for t in self.runs[run_id]["summary"]["tests"]]

    def bug_report(self, run_id, fmt):
        return f"bug-report-{run_id}.{fmt}", b"report"

    def release(self, system):
        return {"emoji": "🟡", "label": "فيه مخاطر", "headline": "boom", "passRate": 90.0, "open": 1, "critical": 0}

    def email_ready(self):
        return True

    def send_email(self, emails, subject, html_body, text_body):
        self.mails.append((emails, subject, text_body))
        return len(emails)

    def public_url(self):
        return "http://runner.test"


class OrchestratorTest(AgentSandbox):
    def setUp(self):
        super().setUp()
        self.saved_jobs = orchestrator.JOBS_DIR
        orchestrator.JOBS_DIR = self.tmp / "jobs"
        orchestrator._jobs.clear()
        orchestrator._signals.clear()
        orchestrator._threads.clear()
        self.services = FakeServices()
        orchestrator.load(self.services)

    def tearDown(self):
        for job_id in list(orchestrator._jobs):
            job = orchestrator._jobs[job_id]
            if job["status"] in ("running", "waiting"):
                try:
                    orchestrator.decide(job_id, "cancel")
                except orchestrator.JobError:
                    pass
        wait_for(lambda: all(not t.is_alive() for t in orchestrator._threads.values()), timeout=20)
        orchestrator.JOBS_DIR = self.saved_jobs
        orchestrator._jobs.clear()
        super().tearDown()

    def job(self, job_id):
        return orchestrator.get(job_id)

    def wait_state(self, job_id, status, step=None):
        def ready():
            job = self.job(job_id)
            return job["status"] == status and (step is None or job["current"] == step) and job
        return wait_for(ready, timeout=30)

    def test_orc001_workflows_and_default_prompts(self):
        flows = orchestrator.workflows()
        self.assertEqual([s["key"] for s in flows["sprint"]["steps"]],
                         ["analyze", "design", "model", "coverage", "automate", "run", "triage", "report", "release", "notify"])
        self.assertEqual([s["kind"] for s in flows["bug"]["steps"]],
                         ["agent-read:bugcheck", "agent-code:bugautomate", "run-new", "report", "notify"])
        self.assertEqual(flows["regression"]["steps"][0]["kind"], "run-scope")
        self.assertEqual(flows["retest"]["steps"][0]["kind"], "run-retest")
        self.assertIn("@Issue", orchestrator.default_prompt("bug", "automate"))
        with self.assertRaises(orchestrator.JobError):
            orchestrator.default_prompt("bug", "design")

    def test_orc002_bug_workflow_approvals_merge_run_report_and_mail(self):
        job_id = orchestrator.create({"type": "bug", "system": "zulfi", "text": "الحفظ لا يعمل", "emails": ["QA@gmail.com"]})["id"]
        job = self.wait_state(job_id, "waiting", 0)
        self.assertIn("تم التعديل", job["steps"][0]["output"])
        self.assertIn("الحفظ لا يعمل", job["steps"][0]["sentPrompt"])
        orchestrator.decide(job_id, "approve")
        job = self.wait_state(job_id, "waiting", 1)
        self.assertIn("DemoTest.java", job["steps"][1]["output"])
        self.assertEqual(self.demo.read_text(encoding="utf-8"), ORIGINAL_DEMO, "nothing merged before approval")
        orchestrator.decide(job_id, "approve")
        job = self.wait_state(job_id, "done")
        self.assertIn("edited by fake claude", self.demo.read_text(encoding="utf-8"))
        self.assertEqual(sorted(self.services.created[0]["classes"]), ["DemoTest", "NewFlowTest"])
        report = next(s for s in job["steps"] if s["key"] == "report")
        self.assertEqual(report["files"], ["bug-report-run1.docx", "bug-report-run1.xlsx"])
        self.assertTrue(orchestrator.job_file(job_id, "bug-report-run1.docx").is_file())
        self.assertEqual(self.services.mails[0][0], ["qa@gmail.com"])
        self.assertIn("Add the bug's test: done", self.services.mails[0][2])
        name, data = orchestrator.step_report_docx(job_id, 0)
        self.assertTrue(name.endswith(".docx") and data[:2] == b"PK")

    def test_orc003_redo_with_note_skip_and_cancel(self):
        job_id = orchestrator.create({"type": "bug", "system": "zulfi", "text": "bug"})["id"]
        self.wait_state(job_id, "waiting", 0)
        orchestrator.decide(job_id, "redo", "ركز على صفحة الأندية")
        wait_for(lambda: any("ركز على صفحة الأندية" in c["prompt"] for c in self.logged_calls()))
        self.wait_state(job_id, "waiting", 0)
        orchestrator.decide(job_id, "skip")
        self.wait_state(job_id, "waiting", 1)
        self.assertEqual(self.job(job_id)["steps"][0]["status"], "skipped")
        orchestrator.decide(job_id, "cancel")
        job = self.wait_state(job_id, "cancelled")
        self.assertEqual(self.demo.read_text(encoding="utf-8"), ORIGINAL_DEMO)
        with self.assertRaises(orchestrator.JobError):
            orchestrator.decide(job_id, "approve")
        self.assertEqual(job["status"], "cancelled")

    def test_orc004_failed_step_can_be_retried(self):
        os.environ["FAKE_CLAUDE_FAIL"] = "1"
        job_id = orchestrator.create({"type": "bug", "system": "zulfi", "text": "bug"})["id"]
        job = self.wait_state(job_id, "failed")
        self.assertEqual(job["steps"][0]["status"], "failed")
        os.environ.pop("FAKE_CLAUDE_FAIL")
        orchestrator.retry(job_id)
        self.wait_state(job_id, "waiting", 0)

    def test_orc005_regression_triage_merges_and_reruns_up_to_two_rounds(self):
        job_id = orchestrator.create({"type": "regression", "system": "zulfi", "scope": "e2e"})["id"]
        self.wait_state(job_id, "waiting", 1)
        self.assertEqual(self.services.created[0], {"scope": "e2e"})
        orchestrator.decide(job_id, "approve")
        wait_for(lambda: self.job(job_id)["steps"][1]["rounds"] == 2 and self.job(job_id)["status"] == "waiting")
        orchestrator.decide(job_id, "approve")
        job = self.wait_state(job_id, "done")
        triage = job["steps"][1]
        self.assertEqual(len(triage["runIds"]), 2, "one rerun after each merged round")
        self.assertEqual(self.services.created[1], {"scope": "retest-tests", "from": "run1"})
        self.assertIn("Round 2", triage["output"])
        release = next(s for s in job["steps"] if s["key"] == "release")
        self.assertIn("فيه مخاطر", release["output"])

    def test_orc006_creation_is_validated(self):
        for payload in ({"type": "nope", "system": "zulfi"}, {"type": "bug", "system": "zulfi", "text": ""},
                        {"type": "regression", "system": "zulfi", "scope": "classes"},
                        {"type": "retest", "system": "unknown"},
                        {"type": "retest", "system": "zulfi", "emails": ["x@yahoo.com"]}):
            with self.subTest(payload=payload), self.assertRaises((orchestrator.JobError, schedules.ScheduleError)):
                orchestrator.create(payload)


if __name__ == "__main__":
    unittest.main(verbosity=2)
