"""Dashboard insights: success criteria, release status, defect registry, retest plans and bug-report export.

Run:  python -m unittest discover -s runner/tests -v

Contract under test (runner/server.py, spec sections 8, 9, 18):
  * quality() uses the latest executed result of every test in the period; a test fixed later is no longer open.
  * Open defects leave out quarantined tests, "not a bug" and tests that no longer exist in the sources.
  * health(): red with a Critical, yellow with other open defects or pass rate < 95 %, green otherwise.
  * Defect IDs use the system prefix (ZUL-01...); exporting a report records the date; a defect that passed and
    fails again on a later day is Reopened.
"""

import io
import sys
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from support import LOGIN, RunnerTestCase, login_methods, server  # noqa: E402


class QualityTest(RunnerTestCase):
    def setUp(self):
        super().setUp()
        m = login_methods()
        self.a, self.b, self.c = (f"{LOGIN}.{m[0]}", f"{LOGIN}.{m[1]}", f"{LOGIN}.{m[2]}")

    def test_ins001_latest_result_wins_and_fixed_tests_are_listed(self):
        now = server.now()
        day = lambda d: (now.replace(hour=10) - server.timedelta(days=d)).isoformat()  # noqa: E731
        self.add_run("r1", day(3), [(self.a, "failed", "expected [1] but found [2]"), (self.b, "passed")])
        self.add_run("r2", day(2), [(self.a, "passed"), (self.b, "failed", "boom")])
        q = server.quality("zulfi", 30)
        self.assertEqual((q["tests"], q["passed"], q["failed"], q["open"]), (2, 1, 1, 1))
        self.assertEqual(q["passRate"], 50.0)
        self.assertEqual([d["fullName"] for d in q["defects"]], [self.b])
        fixed = server.fixed_defects("zulfi", 30)
        self.assertEqual([(f["fullName"], f["failedIn"], f["fixedIn"]) for f in fixed], [(self.a, "r1", "r2")])

    def test_ins002_quarantine_not_a_bug_and_deleted_tests_are_not_open(self):
        created = server.now_iso()
        self.add_run("r1", created, [(self.a, "failed", "x"), (self.b, "failed", "y"), (self.c, "failed", "z"),
                                     (f"{LOGIN}.methodThatWasDeleted", "failed", "gone")])
        server.reliability.set_quarantine("zulfi", self.a, True, "flaky")
        server.rate_defect({"run": "r1", "test": self.b, "notBug": True})
        self.assertEqual([d["fullName"] for d in server.open_defects("zulfi", 30)], [self.c])

    def test_ins003_release_status_colours(self):
        created = server.now_iso()
        self.add_run("r1", created, [(self.a, "passed"), (self.b, "passed")])
        self.assertEqual(server.health(server.quality("zulfi"))["color"], "green")
        self.assertEqual(server.health(server.quality("zulfi"))["label"], "جاهز للإصدار")
        self.add_run("r2", server.now_iso(), [(self.a, "failed", "boom")])
        health = server.health(server.quality("zulfi"))
        self.assertEqual((health["color"], health["label"]), ("yellow", "فيه مخاطر"))
        server.rate_defect({"run": "r2", "test": self.a, "severity": "Critical", "note": "الفريق مش بيتحفظ"})
        health = server.health(server.quality("zulfi"))
        self.assertEqual((health["color"], health["label"], health["headline"]), ("red", "فيه Critical", "الفريق مش بيتحفظ"))
        self.assertEqual(server.health(server.quality("zakaadash"))["color"], "gray", "no data is not 'ready'")

    def test_ins004_defect_ids_severity_and_reopened(self):
        now = server.now().replace(hour=10)
        self.add_run("r1", (now - server.timedelta(days=5)).isoformat(), [(self.a, "failed", "expected [1] but found [2]")])
        self.add_run("r2", (now - server.timedelta(days=3)).isoformat(), [(self.a, "passed")])
        self.add_run("r3", (now - server.timedelta(days=1)).isoformat(), [(self.a, "failed", "expected [1] but found [3]")])
        entry = server.rate_defect({"run": "r3", "test": self.a, "severity": "High", "note": "note"})
        self.assertEqual((entry["id"], entry["severity"]), ("ZUL-01", "High"))
        self.assertEqual(entry["first"], (now - server.timedelta(days=5)).date().isoformat(),
                         "numbers are masked in the signature, so r1 and r3 are the same defect")
        defect = server.open_defects("zulfi", 30)[0]
        self.assertEqual((defect["status"], defect["id"]), ("Reopened", "ZUL-01"))
        self.assertEqual(server.quality("zulfi", 30)["reopenRate"], 100.0)
        with self.assertRaises(server.BadRequest):
            server.rate_defect({"run": "r3", "test": self.a, "severity": "Blocker"})
        cleared = server.rate_defect({"run": "r3", "test": self.a, "severity": None, "note": ""})
        self.assertEqual((cleared["id"], cleared["severity"], cleared["note"]), ("ZUL-01", None, ""))
        kept = server.rate_defect({"run": "r3", "test": self.a, "note": "only the note"})
        self.assertIsNone(kept["severity"], "a request without 'severity' leaves it unchanged")

    def test_ins005_full_run_rules(self):
        self.add_run("f1", "2026-11-01T10:00:00", [(self.a, "passed")], scope="full", fullRun=True)
        self.add_run("f2", "2026-11-02T10:00:00", [(self.a, "skipped")], scope="full", fullRun=True)
        self.add_run("f3", "2026-11-03T10:00:00", [(self.a, "passed")], scope="classes", fullRun=False)
        self.assertEqual(server.last_full_run("zulfi")["id"], "f1", "a run with only skipped tests is not a full run")
        self.assertFalse(server.is_complete_full_run({"status": "stopped", "fullRun": True,
                                                      "summary": {"tests": [{"status": "passed"}]}}))

    def test_ins006_trial_runs_are_left_out(self):
        self.add_run("t1", server.now_iso(), [(self.a, "failed", "boom")], metrics=False)
        self.assertEqual(server.quality("zulfi")["tests"], 0)

    def test_ins007_availability_counts_checks_and_run_time(self):
        rows = [{"t": "2026-11-07T10:00:00", "system": "zulfi", "up": True},
                {"t": "2026-11-07T10:05:00", "system": "zulfi", "up": False, "detail": "HTTP 503"},
                {"t": "2026-11-07T10:10:00", "system": "zulfi", "up": False, "detail": "HTTP 503"},
                {"t": "2026-11-07T10:15:00", "system": "zulfi", "up": True}]
        result = server.availability("zulfi", 30, rows)
        self.assertEqual(result["percent"], 50.0)
        self.assertEqual(len(result["outages"]), 1)
        self.assertEqual(result["outages"][0]["minutes"], round(2 * server.UPTIME_EVERY / 60))

    def test_ins008_dashboard_and_retest_plan(self):
        self.add_run("r1", server.now_iso(), [(self.a, "failed", "boom"), (self.b, "passed")])
        data = server.dashboard(None, 30)
        self.assertEqual({c["key"] for c in data["systems"]}, {"zulfi", "zakaadash"})
        self.assertEqual(data["openDefects"][0]["fullName"], self.a)
        self.assertEqual(data["mostFailing"][0]["failures"], 1)
        modules = {m["name"]: m for m in data["modules"]["zulfi"]}
        self.assertEqual((modules["Login"]["passed"], modules["Login"]["failed"]), (1, 1))
        plan = server.retest_plan("zulfi")
        self.assertEqual(plan["classes"][0]["failing"], [self.a.rsplit(".", 1)[1]])
        payload = server.retest_request("zulfi")
        self.assertEqual((payload["scope"], payload["tests"]), ("retest-tests", {"LoginRegressionTest": [self.a.rsplit(".", 1)[1]]}))

    def test_ins009_defect_retest_creates_a_one_test_run(self):
        self.add_run("r1", server.now_iso(), [(self.a, "failed", "boom")])
        run = server.defect_retest({"run": "r1", "test": self.a})
        self.assertEqual((run["scope"], run["tests"], run["estimated"]), ("retest-one", {"LoginRegressionTest": [self.a.rsplit(".", 1)[1]]}, 1))


class BugReportTest(RunnerTestCase):
    def setUp(self):
        super().setUp()
        self.test = f"{LOGIN}.{login_methods()[0]}"
        self.add_run("r1", server.now_iso(), [(self.test, "failed",
                                               "Step failed: Visible text — expected: Arabic only — actual: «Round of 32»",
                                               {"testId": "LOGIN-UI-001", "title": "Arabic labels"})])

    def test_ins010_suggestions_parse_expected_and_actual(self):
        bug = server.bug_suggestions("r1")["bugs"][0]
        self.assertEqual((bug["expected"], bug["actual"]), ("Arabic only", "«Round of 32»"))
        self.assertEqual((bug["severity"], bug["priority"], bug["status"]), ("Medium", "P3", "New"))
        self.assertTrue(bug["title"].startswith("LOGIN-UI-001"))

    def test_ins011_word_and_excel_exports_record_the_report_date(self):
        bugs = server.bug_suggestions("r1")["bugs"]
        name, data = server.generate_bug_report("r1", "docx", bugs)
        self.assertTrue(name.endswith(".docx"))
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            document = archive.read("word/document.xml").decode("utf-8")
        self.assertIn("ZUL-01", document)
        self.assertIn("Arabic only", document)
        name, data = server.generate_bug_report("r1", "xlsx", bugs)
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            self.assertIn("xl/worksheets/sheet1.xml", archive.namelist())
        _, entry = server.defect_of("zulfi", server.RUNS["r1"]["summary"]["tests"][0])
        self.assertEqual(entry["reported"], [server.now_iso()[:10]], "two exports on one day record the date once")
        with self.assertRaises(server.BadRequest):
            server.generate_bug_report("r1", "pdf", bugs)


if __name__ == "__main__":
    unittest.main(verbosity=2)
