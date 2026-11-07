"""Test cases for the archive index behind feature health and the all-runs list.

Run:  python -m unittest discover -s runner/tests -v

Contract under test (runner/archive.py):
  * bucket() maps a latest result to pass / product / automation / triage / skipped / never via failureCategory.
  * health() keeps the latest *executed* result (a later phase-gate skip does not hide it), leaves out tests whose
    class is not in the generated suites (retired), lists suite tests never executed, and groups by feature.
  * refresh() parses only new or changed run folders and the parsed index survives a restart via the cache file.
  * run_list() filters by system and counts statuses per run.
"""

import json
import sys
import tempfile
import unittest
import uuid
from pathlib import Path

RUNNER_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RUNNER_DIR))

import archive  # noqa: E402

STD = "com.taf.systems.zulfi.features.standings.StandingsAcceptanceTest"
OLD = "com.taf.systems.zulfi.features.clubinfo.ClubInformationAcceptanceTest"
ZAKAA = "com.taf.systems.zakaadash.tests.AuthTest"


def result(cls, method, status, start, test_id="", feature="League Standings", category="", message=""):
    labels = [{"name": "testClass", "value": cls}, {"name": "testMethod", "value": method},
              {"name": "parentSuite", "value": feature}, {"name": "testCaseId", "value": test_id}]
    if category:
        labels.append({"name": "failureCategory", "value": category})
    return {"name": f"{test_id} — {method}", "status": status, "start": start, "stop": start + 1500, "labels": labels,
            "statusDetails": {"message": message}}


class ArchiveTestCase(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.root = Path(self.folder.name)
        self.history = self.root / "history"
        self.runs = self.root / "runs"
        self.cache = self.root / "cache.json"
        self.history.mkdir()
        self.runs.mkdir()

    def tearDown(self):
        self.folder.cleanup()

    def cli_run(self, run_id, results, report=False):
        folder = self.history / run_id / "allure-results"
        folder.mkdir(parents=True, exist_ok=True)
        for r in results:
            (folder / f"{uuid.uuid4()}-result.json").write_text(json.dumps(r, ensure_ascii=False), encoding="utf-8")
        if report:
            (self.history / run_id / "allure-html").mkdir(exist_ok=True)
            (self.history / run_id / "allure-html" / "index.html").write_text("<html></html>", encoding="utf-8")
        return folder

    def index(self):
        return archive.ArchiveIndex(self.history, self.runs, self.cache)


class BucketTest(unittest.TestCase):
    def test_categories(self):
        rec = lambda status, category="": {"status": status, "category": category}  # noqa: E731
        self.assertEqual(archive.bucket(None), "never")
        self.assertEqual(archive.bucket(rec("passed")), "pass")
        self.assertEqual(archive.bucket(rec("skipped")), "skipped")
        self.assertEqual(archive.bucket(rec("failed", "PRODUCT_DEFECT")), "product")
        self.assertEqual(archive.bucket(rec("failed", "TIMEOUT")), "automation")
        self.assertEqual(archive.bucket(rec("broken")), "automation")
        self.assertEqual(archive.bucket(rec("failed", "INCONCLUSIVE")), "triage")
        self.assertEqual(archive.bucket(rec("failed")), "triage")


class HealthTest(ArchiveTestCase):
    def test_latest_executed_result_wins_over_later_skip(self):
        self.cli_run("r1", [result(STD, "std001", "failed", 1000, "STD-001", category="PRODUCT_DEFECT", message="row 3 wrong")])
        self.cli_run("r2", [result(STD, "std001", "passed", 2000, "STD-001")])
        self.cli_run("r3", [result(STD, "std001", "skipped", 3000, "STD-001")])
        test = self.index().health("zulfi")["tests"][0]
        self.assertEqual((test["state"], test["lastRun"]), ("pass", "r2"))
        self.assertEqual(test["history"], ["failed", "passed", "skipped"])
        self.assertFalse(test["flaky"], "a single fail → pass change is a fix, not flakiness")
        self.assertEqual(test["skippedSince"], 3000)

    def test_flaky_means_flipping_back_and_forth(self):
        for n, status in enumerate(["passed", "failed", "passed"]):
            self.cli_run(f"r{n}", [result(STD, "std001", status, 1000 * (n + 1), "STD-001")])
        self.assertTrue(self.index().health("zulfi")["tests"][0]["flaky"])

    def test_catalog_excludes_retired_classes_and_lists_never_run_tests(self):
        self.cli_run("r1", [result(STD, "std001", "failed", 1000, "STD-001", category="PRODUCT_DEFECT"),
                            result(OLD, "clubInf001", "failed", 1000, "CLUB-INF-001", feature="Club Information")])
        catalog = {STD: [{"method": "std001", "id": "STD-001", "description": "", "module": "standings"},
                         {"method": "std002", "id": "STD-002", "description": "STD-002 — full table", "module": "standings"}]}
        health = self.index().health("zulfi", catalog)
        states = {t["id"]: t["state"] for t in health["tests"]}
        self.assertEqual(states, {"STD-001": "product", "STD-002": "never"})
        feature = health["features"][0]
        self.assertEqual((feature["name"], feature["tests"], feature["product"], feature["never"]), ("League Standings", 2, 1, 1))
        self.assertEqual(health["totals"]["passRate"], 0)

    def test_systems_are_separated(self):
        self.cli_run("r1", [result(STD, "std001", "passed", 1000, "STD-001"), result(ZAKAA, "auth001", "failed", 1000, "AUTH-001")])
        index = self.index()
        self.assertEqual([t["id"] for t in index.health("zulfi")["tests"]], ["STD-001"])
        self.assertEqual([t["id"] for t in index.health("zakaadash")["tests"]], ["AUTH-001"])
        self.assertEqual(index.health("zulfi")["trend"][0]["passRate"], 100)


class RefreshTest(ArchiveTestCase):
    def test_incremental_refresh_and_cache_reload(self):
        folder = self.cli_run("r1", [result(STD, "std001", "passed", 1000, "STD-001")], report=True)
        index = self.index()
        self.assertTrue(index.refresh(force=True))
        self.assertFalse(index.refresh(force=True), "unchanged folders are not parsed again")
        (folder / f"{uuid.uuid4()}-result.json").write_text(json.dumps(result(STD, "std002", "failed", 2000, "STD-002")),
                                                           encoding="utf-8")
        self.assertTrue(index.refresh(force=True))
        reloaded = self.index()
        self.assertEqual(len(reloaded.runs), 1, "cache file restores the parsed runs")
        self.assertFalse(reloaded.refresh(force=True))
        run = reloaded.run_list("zulfi")[0]
        self.assertEqual((run["counts"]["total"], run["counts"]["failed"], run["problems"]), (2, 1, 1))
        self.assertEqual(run["report"], "allure-html/index.html")

    def test_removed_folder_drops_out(self):
        self.cli_run("r1", [result(STD, "std001", "passed", 1000, "STD-001")])
        index = self.index()
        index.refresh(force=True)
        for f in (self.history / "r1" / "allure-results").iterdir():
            f.unlink()
        (self.history / "r1" / "allure-results").rmdir()
        index.refresh(force=True)
        self.assertEqual(index.run_list(), [])


if __name__ == "__main__":
    unittest.main()
