"""Run requests, scopes, TestNG suite generation, scheduling rules, progress parsing and the Allure summary.

Run:  python -m unittest discover -s runner/tests -v
"""

import json
import sys
import tempfile
import unittest
from datetime import timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from support import LOGIN, RunnerTestCase, allure_result, login_methods, server  # noqa: E402


class PresetsTest(unittest.TestCase):
    def setUp(self):
        self.original = server.PRESETS_FILE
        self.folder = tempfile.TemporaryDirectory()
        server.PRESETS_FILE = Path(self.folder.name) / "presets.json"
        server.PRESETS_FILE.write_text(json.dumps({"zulfi": {
            "phaseKeys": [{"key": "standings.phase", "max": "c"}],
            "presets": [{"id": "mixed", "classes": ["zulfi.features.A", "zulfi.features.Missing"]},
                        {"id": "empty", "classes": ["zulfi.features.Missing"]}]}}), encoding="utf-8")

    def tearDown(self):
        server.PRESETS_FILE = self.original
        self.folder.cleanup()

    def test_run001_unknown_classes_are_dropped_and_empty_presets_hidden(self):
        result = server.presets_for("zulfi", {"A"})
        self.assertEqual([p["id"] for p in result["presets"]], ["mixed"])
        self.assertEqual(result["presets"][0]["classes"], ["A"])
        self.assertEqual(result["phaseKeys"], [{"key": "standings.phase", "max": "c"}])

    def test_run002_missing_or_broken_file_yields_no_presets(self):
        server.PRESETS_FILE.write_text("{not json", encoding="utf-8")
        self.assertEqual(server.presets_for("zulfi", {"A"}), {"presets": [], "phaseKeys": []})


class RequestTest(RunnerTestCase):
    def test_run003_defaults_and_simple_or_full_class_names(self):
        request, secrets = server.clean_request({"system": "zulfi", "scope": "classes", "classes": [LOGIN, "HomeOverviewAcceptanceTest"]})
        self.assertEqual(request["classes"], ["LoginRegressionTest", "HomeOverviewAcceptanceTest"])
        self.assertEqual((request["env"], request["execution"], request["threads"]), ("current", "LocalHeadless", 1))
        self.assertTrue(request["preflight"] and request["retry"] and request["metrics"])
        self.assertFalse(request["fullRun"], "only a full scope counts as a full run by default")
        self.assertEqual(secrets, {})

    def test_run004_invalid_requests_are_rejected(self):
        cases = {"unknown system": {"system": "nope"}, "unknown env": {"system": "zulfi", "env": "prod"},
                 "http on https field": {"system": "zulfi", "urls": {"Portal URL": "http://zulfi.example.test"}},
                 "no classes": {"system": "zulfi", "scope": "classes"},
                 "unknown class": {"system": "zulfi", "scope": "classes", "classes": ["Nope"]},
                 "reserved property": {"system": "zulfi", "extraProperties": "output.dir=/tmp"},
                 "bad property line": {"system": "zulfi", "extraProperties": "no equals sign"},
                 "unknown test type": {"system": "zakaadash", "group": "nope"},
                 "unknown module": {"system": "zulfi", "scope": "modules", "modules": ["Nope"]},
                 "empty retest": {"system": "zulfi", "scope": "retest-tests", "tests": {"LoginRegressionTest": ["nope"]}},
                 "bad grid": {"system": "zulfi", "execution": "Remote", "gridHost": "bad host!"}}
        for name, payload in cases.items():
            with self.subTest(case=name):
                with self.assertRaises(server.BadRequest):
                    server.clean_request(payload)

    def test_run005_threads_are_capped_by_the_system(self):
        zakaa, _ = server.clean_request({"system": "zakaadash", "threads": 4})
        zulfi, _ = server.clean_request({"system": "zulfi", "threads": 9})
        self.assertEqual(zakaa["threads"], 1, "ZakaaDash stays serial (AGENTS.md)")
        self.assertEqual(zulfi["threads"], 4)

    def test_run006_an_open_submit_gate_switches_retries_off(self):
        request, _ = server.clean_request({"system": "zulfi", "extraProperties": "analysis.create.enabled=true"})
        self.assertFalse(request["retry"])
        request, _ = server.clean_request({"system": "zulfi", "extraProperties": "analysis.create.enabled=false"})
        self.assertTrue(request["retry"])

    def test_run007_legacy_execution_names_are_accepted(self):
        self.assertEqual(server.clean_request({"system": "zulfi", "execution": "headless"})[0]["execution"], "LocalHeadless")
        self.assertEqual(server.clean_request({"system": "zulfi", "execution": "local"})[0]["execution"], "Local")


class SuiteTest(RunnerTestCase):
    def request(self, **payload):
        request, _ = server.clean_request({"system": "zulfi", **payload})
        request["id"] = "suite-test"
        return request

    def suite(self, **payload):
        run = self.request(**payload)
        path = server.suite_file(run, self.tmp)
        return run, path, path.read_text(encoding="utf-8")

    def test_run008_plain_full_run_uses_the_generated_suite(self):
        _, path, _ = self.suite(scope="full")
        self.assertEqual(path, server.SUITES_DIR / "zulfi.xml")

    def test_run009_full_run_skips_excluded_classes_and_counts_tests(self):
        run = self.request(scope="full")
        classes = [c for _, block in server.wanted_blocks(run) for c in block]
        self.assertNotIn("com.taf.systems.zulfi.features.clubinfo.ClubInformationAcceptanceTest", classes)
        self.assertEqual(server.estimate(run), server.catalog()["zulfi"]["testCount"])

    def test_run010_methods_are_included_for_retest_scopes(self):
        methods = login_methods()[:2]
        run, _, xml = self.suite(scope="retest-tests", tests={"LoginRegressionTest": methods})
        for method in methods:
            self.assertIn(f'<include name="{method}"/>', xml)
        self.assertIn(f'<class name="{LOGIN}">', xml)
        self.assertEqual(server.estimate(run), 2)

    def test_run011_parallel_runs_get_one_test_per_class_and_serial_groups_stay_together(self):
        _, _, xml = self.suite(scope="modules", modules=["Login", "Create analysis (official fixture)"], threads=3)
        self.assertIn('parallel="tests" thread-count="3"', xml)
        self.assertIn('<test name="LoginRegressionTest"', xml)
        group = xml.split('<test name="Zulfi - analysis fixtures"')[1].split("</test>")[0]
        self.assertEqual(group.count("<class "), 4, "the analysis classes share one thread, in order")
        self.assertLess(group.index("OfficialFixtureDiscoveryTest"), group.index("OfficialFixtureSelectionTest"))
        props = server.jvm_properties(self.request(threads=3), self.tmp)
        self.assertEqual((props["DEFAULT_WAIT"], props["parallel.classes"]), ("25", "3"))

    def test_run012_test_type_filters_classes_and_adds_group_include(self):
        request, _ = server.clean_request({"system": "zakaadash", "scope": "full", "group": "smoke"})
        request["id"] = "g"
        xml = server.suite_file(request, self.tmp).read_text(encoding="utf-8")
        self.assertIn('<include name="smoke"/>', xml)
        self.assertNotIn("DataUploadJourneyE2ETest", xml, "E2E classes have no smoke test")
        self.assertEqual(server.estimate(request), server.catalog()["zakaadash"]["groupCounts"]["smoke"])

    def test_run013_urls_and_properties_reach_the_jvm(self):
        run = self.request(urls={"Portal URL": "https://zulfi.example.test/p/"}, extraProperties="home.overview.phase=b")
        props = server.jvm_properties(run, self.tmp / "output")
        self.assertEqual(props["zulfi.baseUrl"], "https://zulfi.example.test/p/")
        self.assertEqual(props["home.overview.phase"], "b")
        self.assertEqual(props["active.system"], "zulfi")
        self.assertTrue(props["allure.results.directory"].endswith("allure-results"))


class SchedulerTest(RunnerTestCase):
    def queued(self, run_id, system="zulfi", url="https://zulfi.example.test/", **extra):
        run = {"id": run_id, "system": system, "status": "queued", "created": "2026-11-07T10:00:00",
               "urls": {"URL": url}, **extra}
        server.RUNS[run_id] = run
        return run

    def test_run014_same_system_and_same_server_never_overlap(self):
        active = [self.queued("a", status="running")]
        self.assertFalse(server.can_start(self.queued("b"), active), "same system")
        other = self.queued("c", system="zakaadash", url="https://zulfi.example.test/other")
        self.assertFalse(server.can_start(other, active), "same server (host)")
        self.assertTrue(server.can_start(self.queued("d", system="zakaadash", url="https://zd.example.test/"), active))
        self.assertTrue(server.can_start(self.queued("e", parallelStart=True), active), "parallelStart overrides")

    def test_run015_capacity_batch_after_and_not_before(self):
        active = [self.queued(f"x{i}", system=f"s{i}", url=f"https://h{i}.test/", status="running")
                  for i in range(server.MAX_PARALLEL)]
        self.assertFalse(server.can_start(self.queued("cap", system="zakaadash", url="https://free.test/"), active))
        active = [self.queued("b1", system="zakaadash", url="https://zd.test/", status="running", batch="B", batchParallel=1)]
        self.assertFalse(server.can_start(self.queued("b2", batch="B", batchParallel=1), active))
        self.queued("first", system="zakaadash", url="https://zd2.test/", status="running")
        self.assertFalse(server.can_start(self.queued("after", after=["first"]), []))
        server.RUNS["first"]["status"] = "passed"
        self.assertTrue(server.can_start(server.RUNS["after"], []))
        later = (server.now() + timedelta(minutes=5)).isoformat()
        self.assertFalse(server.can_start(self.queued("nb", notBefore=later), []))

    def test_run016_schedule_once_starts_runs_in_order(self):
        started = []
        original = server.execute
        server.execute = lambda run: started.append(run["id"])
        try:
            self.queued("q1", created="2026-11-07T10:00:00")
            self.queued("q2", created="2026-11-07T10:00:01")
            self.queued("q3", system="zakaadash", url="https://zd.test/", created="2026-11-07T10:00:02")
            server.schedule_once()
        finally:
            server.execute = original
        self.assertEqual(server.RUNS["q1"]["status"], "building")
        self.assertEqual(server.RUNS["q2"]["status"], "queued", "same system waits")
        self.assertEqual(server.RUNS["q3"]["status"], "building")

    def test_run017_env_down_runs_are_queued_again_until_the_limit(self):
        run = self.queued("env", status="environment down", envRetry=0, finished="x", error="down")
        self.assertTrue(server.env_retry_due(run))
        self.assertEqual((run["status"], run["envRetry"]), ("queued", 1))
        self.assertIsNotNone(run["notBefore"])
        run.update(status="environment down", envRetry=server.reliability.ENV_RETRIES)
        self.assertFalse(server.env_retry_due(run))

    def test_run018_stopping_a_queued_run_cancels_it(self):
        self.queued("stopme")
        self.assertEqual(server.stop_run("stopme")["status"], "cancelled")
        with self.assertRaises(server.BadRequest):
            server.stop_run("stopme")


class ProgressTest(unittest.TestCase):
    def test_run019_progress_lines_and_retries(self):
        run = {"progress": {"passed": 0, "failed": 0, "skipped": 0, "done": 0, "retried": 0, "current": None}}
        pending = set()
        for line in ["[TAF-PROGRESS] START com.x.LoginTest#a", "[TAF-PROGRESS] PASS com.x.LoginTest#a",
                     "[TAF-RETRY] com.x.LoginTest#b timing failure, retry 1/1", "[TAF-PROGRESS] SKIP com.x.LoginTest#b",
                     "[TAF-PROGRESS] PASS com.x.LoginTest#b", "Test Case createTeam (failed)", "noise"]:
            server.track_progress(run, line, pending)
        p = run["progress"]
        self.assertEqual((p["passed"], p["failed"], p["skipped"], p["done"], p["retried"]), (2, 1, 0, 3, 1))
        self.assertEqual(p["current"], "createTeam")


class SummaryTest(RunnerTestCase):
    def test_run020_retries_flaky_soft_checks_screenshots_and_setup_failures(self):
        results = self.tmp / "r1" / "output" / "allure-results"
        allure_result(results, LOGIN, login_methods()[0], "failed", 1000, history="h1", message="TimeoutException: slow")
        allure_result(results, LOGIN, login_methods()[0], "passed", 5000, history="h1")
        allure_result(results, LOGIN, login_methods()[1], "failed", 2000, message="expected [true] but found [false]",
                      links=[{"type": "issue", "name": "BUG-7"}],
                      attachments=[{"name": "x_FAIL", "type": "image/png", "source": "abc-attachment.png"}],
                      labels=[{"name": "testCaseId", "value": "LOGIN-UI-002"}, {"name": "failureCategory", "value": "PRODUCT_DEFECT"}])
        allure_result(results, LOGIN, "createFreshBrowser", "broken", 900, message="UncheckedIOException: loopback")
        allure_result(results, LOGIN, login_methods()[2], "skipped", 3000)
        allure_result(results, LOGIN, "openLoginInFreshBrowser", "skipped", 950)
        summary = server.summarize(self.tmp / "r1", server.is_test_method)
        by_name = {t["name"]: t for t in summary["tests"]}
        retried = by_name[login_methods()[0]]
        self.assertEqual((retried["status"], retried["retried"], retried["flaky"]), ("passed", 1, True))
        self.assertIn("TimeoutException", retried["firstAttempt"])
        soft = by_name[login_methods()[1]]
        self.assertTrue(soft["soft"])
        self.assertEqual((soft["shot"], soft["testId"], soft["category"]), ("abc-attachment.png", "LOGIN-UI-002", "PRODUCT_DEFECT"))
        self.assertNotIn("createFreshBrowser", by_name, "a fixture is not a test")
        self.assertEqual(summary["setup"][0]["name"], "createFreshBrowser")
        self.assertEqual(summary["counts"], {"passed": 1, "failed": 1, "broken": 0, "skipped": 1, "bugChecks": 1,
                                             "flaky": 1, "setupFailures": 1})

    def test_run021_no_results_folder_means_no_summary(self):
        self.assertIsNone(server.summarize(self.tmp / "missing"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
