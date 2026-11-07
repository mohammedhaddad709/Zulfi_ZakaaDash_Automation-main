"""Read-only index of every archived Allure result: Maven/CLI runs (test-execution-reports/history/<id>/) and
dashboard runs (runner/runs/<id>/output/). Feeds feature health, the all-runs list and archive run details.

* Incremental: a run folder is parsed again only when its allure-results signature (file count + folder mtime)
  changes; parsed runs are cached in runner/.archive-cache.json so a restart does not re-read ~2k JSON files.
* Never writes or deletes anything under test-execution-reports (historical reports are evidence).
"""

import json
import os
import re
import threading
import time
from pathlib import Path

CACHE_VERSION = 1
SCAN_INTERVAL_S = 10
SYSTEM_OF_CLASS = re.compile(r"^com\.taf\.systems\.(\w+)\.")
# failureCategory labels written by the framework (reporting contract), grouped for the dashboard
AUTOMATION_CATEGORIES = {"AUTOMATION_DEFECT", "CONFIGURATION_ERROR", "ENVIRONMENT_ERROR", "TEST_DATA_ERROR", "TIMEOUT"}
EXECUTED = {"passed", "failed", "broken"}
HISTORY_DOTS = 12


def bucket(record):
    """pass | product | automation | triage | skipped | never — what the latest result means for the team."""
    if record is None:
        return "never"
    status, category = record["status"], record["category"]
    if status == "passed":
        return "pass"
    if status == "skipped":
        return "skipped"
    if category == "PRODUCT_DEFECT":
        return "product"
    if category in AUTOMATION_CATEGORIES or (status == "broken" and not category):
        return "automation"
    return "triage"  # INCONCLUSIVE, or an assertion failure nobody classified yet


def parse_result(path):
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None
    labels = {}
    for label in data.get("labels", []):
        labels.setdefault(label.get("name"), label.get("value"))
    cls, method = labels.get("testClass") or "", labels.get("testMethod") or ""
    match = SYSTEM_OF_CLASS.match(cls)
    if not (cls and method and match):
        return None
    start, stop = int(data.get("start") or 0), int(data.get("stop") or 0)
    message = ((data.get("statusDetails") or {}).get("message") or "").strip()
    return {
        "key": f"{cls}#{method}", "class": cls, "method": method, "system": match.group(1),
        "id": labels.get("testCaseId") or "", "title": labels.get("testTitle") or data.get("name") or method,
        "feature": labels.get("parentSuite") or labels.get("suite") or "",
        "status": data.get("status") or "unknown", "category": labels.get("failureCategory") or "",
        "message": message[:600], "start": start, "durationMs": max(0, stop - start),
    }


def parse_run(run_id, source, results_dir, report_rel):
    tests = [r for r in (parse_result(p) for p in results_dir.glob("*-result.json")) if r]
    tests.sort(key=lambda r: r["start"])
    counts = {"passed": 0, "failed": 0, "broken": 0, "skipped": 0}
    for test in tests:
        if test["status"] in counts:
            counts[test["status"]] += 1
    return {
        "id": run_id, "source": source, "tests": tests,
        "started": tests[0]["start"] if tests else 0,
        "finished": max((t["start"] + t["durationMs"] for t in tests), default=0),
        "counts": {**counts, "total": len(tests)},
        "systems": sorted({t["system"] for t in tests}),
        "features": sorted({t["feature"] for t in tests if t["feature"]}),
        "report": report_rel,
    }


class ArchiveIndex:
    def __init__(self, history_dir, runner_runs_dir, cache_file):
        self.history_dir, self.runner_runs_dir, self.cache_file = Path(history_dir), Path(runner_runs_dir), Path(cache_file)
        self.lock = threading.RLock()
        self.runs = {}          # folder path -> {"sig": [...], "run": {...}}
        self.version = 0        # bumps when any run is added/changed/removed (cache key for derived views)
        self.scanned_at = 0.0
        self._derived = {}
        self._load_cache()

    # ------------------------------------------------------------------ scanning
    def _sources(self):
        if self.history_dir.is_dir():
            for folder in self.history_dir.iterdir():
                results = folder / "allure-results"
                if results.is_dir():
                    report = "allure-html/index.html" if (folder / "allure-html" / "index.html").is_file() else None
                    yield folder.name, "cli", folder, results, report
        if self.runner_runs_dir.is_dir():
            for folder in self.runner_runs_dir.iterdir():
                results = folder / "output" / "allure-results"
                if results.is_dir():
                    report = "allure-report/index.html" if (folder / "allure-report" / "index.html").is_file() else None
                    yield folder.name, "dashboard", folder, results, report

    @staticmethod
    def _signature(results_dir, report):
        try:
            count = sum(1 for e in os.scandir(results_dir) if e.name.endswith("-result.json"))
            return [count, results_dir.stat().st_mtime_ns, bool(report)]
        except OSError:
            return None

    def refresh(self, force=False):
        with self.lock:
            if not force and time.monotonic() - self.scanned_at < SCAN_INTERVAL_S:
                return False
            seen, changed = set(), False
            for run_id, source, folder, results, report in self._sources():
                key = str(folder)
                seen.add(key)
                sig = self._signature(results, report)
                cached = self.runs.get(key)
                if sig is None or (cached and cached["sig"] == sig):
                    continue
                self.runs[key] = {"sig": sig, "run": parse_run(run_id, source, results, report)}
                changed = True
            for key in [k for k in self.runs if k not in seen]:
                del self.runs[key]
                changed = True
            self.scanned_at = time.monotonic()
            if changed:
                self.version += 1
                self._derived.clear()
                self._save_cache()
            return changed

    def _load_cache(self):
        try:
            data = json.loads(self.cache_file.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            return
        if data.get("version") == CACHE_VERSION:
            self.runs = data.get("runs", {})

    def _save_cache(self):
        tmp = self.cache_file.with_suffix(".tmp")
        try:
            tmp.write_text(json.dumps({"version": CACHE_VERSION, "runs": self.runs}, ensure_ascii=False), encoding="utf-8")
            os.replace(tmp, self.cache_file)
        except OSError:
            pass  # the cache only speeds up the next start

    # ------------------------------------------------------------------ queries
    def all_runs(self):
        self.refresh()
        with self.lock:
            return [entry["run"] for entry in self.runs.values()]

    def folder_of(self, source, run_id):
        with self.lock:
            for key, entry in self.runs.items():
                if entry["run"]["id"] == run_id and entry["run"]["source"] == source:
                    return Path(key)
        return None

    def run(self, source, run_id):
        return next((r for r in self.all_runs() if r["id"] == run_id and r["source"] == source), None)

    def run_list(self, system=None, limit=200):
        rows = []
        for run in self.all_runs():
            tests = [t for t in run["tests"] if not system or t["system"] == system]
            if not tests:
                continue
            counts = {s: sum(1 for t in tests if t["status"] == s) for s in ("passed", "failed", "broken", "skipped")}
            rows.append({key: run[key] for key in ("id", "source", "started", "finished", "report", "systems")}
                        | {"counts": {**counts, "total": len(tests)},
                           "features": sorted({t["feature"] for t in tests if t["feature"]}),
                           "problems": sum(1 for t in tests if bucket(t) in ("product", "automation", "triage"))})
        rows.sort(key=lambda r: r["started"], reverse=True)
        return rows[:limit]

    def health(self, system, catalog_classes=None):
        """Latest meaningful result per test of `system`, grouped by feature.

        `catalog_classes` maps every class in the system's generated suites to its methods
        ([{method, id, description, module}]): tests outside it (retired, deleted) are left out and tests
        never executed are listed as `never`. The latest *executed* result wins over later skips, because a
        phase-a smoke run skips phase-b/c tests without saying anything about them.
        """
        self.refresh()
        cache_key = (system, self.version, json.dumps(catalog_classes, sort_keys=True) if catalog_classes else None)
        with self.lock:
            if cache_key in self._derived:
                return self._derived[cache_key]
            per_test, runs = {}, []
            for entry in self.runs.values():
                run = entry["run"]
                tests = [t for t in run["tests"] if t["system"] == system]
                if not tests:
                    continue
                executed = [t for t in tests if t["status"] in EXECUTED]
                if executed:
                    passed = sum(1 for t in executed if t["status"] == "passed")
                    runs.append({"id": run["id"], "source": run["source"], "started": run["started"],
                                 "passed": passed, "failed": len(executed) - passed,
                                 "skipped": len(tests) - len(executed), "passRate": round(100 * passed / len(executed))})
                for test in tests:
                    per_test.setdefault(test["key"], []).append({**test, "runId": run["id"], "source": run["source"]})
        runs.sort(key=lambda r: r["started"])

        known = {}
        if catalog_classes:
            for cls, methods in catalog_classes.items():
                for m in methods:
                    known[f"{cls}#{m['method']}"] = {"class": cls, **m}
        keys = list(known) if catalog_classes else list(per_test)
        class_feature = {}
        for records in per_test.values():
            for r in records:
                if r["feature"]:
                    class_feature[r["class"]] = r["feature"]

        tests = []
        for key in keys:
            records = sorted(per_test.get(key, []), key=lambda r: r["start"])
            executed = [r for r in records if r["status"] in EXECUTED]
            current = executed[-1] if executed else (records[-1] if records else None)
            meta = known.get(key, {})
            cls = meta.get("class") or (current or {}).get("class") or key.split("#")[0]
            # flaky = flips back and forth; one change (a fix, or a new regression) is not flakiness
            recent = [r["status"] == "passed" for r in executed[-10:]]
            flips = sum(1 for a, b in zip(recent, recent[1:]) if a != b)
            tests.append({
                "key": key, "class": cls, "method": key.split("#", 1)[1],
                "id": (current or {}).get("id") or meta.get("id") or "",
                "title": (current or {}).get("title") or meta.get("description") or meta.get("method") or key,
                "feature": class_feature.get(cls) or meta.get("module") or cls.rsplit(".", 1)[-1],
                "state": bucket(current), "status": (current or {}).get("status", "never"),
                "category": (current or {}).get("category", ""), "message": (current or {}).get("message", ""),
                "lastRun": (current or {}).get("runId"), "lastSource": (current or {}).get("source"),
                "lastAt": (current or {}).get("start", 0),
                "skippedSince": records[-1]["start"] if records and records[-1]["status"] == "skipped" and executed else 0,
                "history": [r["status"] for r in records[-HISTORY_DOTS:]],
                "flaky": flips >= 2,
                "runs": len(executed),
            })

        features = {}
        for t in tests:
            f = features.setdefault(t["feature"], {"name": t["feature"], "tests": 0, "pass": 0, "product": 0, "automation": 0,
                                                    "triage": 0, "skipped": 0, "never": 0, "flaky": 0, "lastAt": 0,
                                                    "classes": set()})
            f["tests"] += 1
            f[t["state"]] += 1
            f["flaky"] += t["flaky"]
            f["lastAt"] = max(f["lastAt"], t["lastAt"])
            f["classes"].add(t["class"])
        feature_rows = []
        for f in features.values():
            executed = f["pass"] + f["product"] + f["automation"] + f["triage"]
            feature_rows.append({**f, "classes": sorted(f["classes"]),
                                 "passRate": round(100 * f["pass"] / executed) if executed else None})
        feature_rows.sort(key=lambda f: (-(f["product"] + f["automation"] + f["triage"]), f["name"]))

        totals = {k: sum(f[k] for f in feature_rows) for k in ("tests", "pass", "product", "automation", "triage", "skipped", "never", "flaky")}
        executed = totals["pass"] + totals["product"] + totals["automation"] + totals["triage"]
        result = {"system": system, "indexedRuns": len(runs), "trend": runs[-40:], "features": feature_rows,
                  "tests": tests, "totals": {**totals, "passRate": round(100 * totals["pass"] / executed) if executed else None},
                  "lastAt": max((t["lastAt"] for t in tests), default=0)}
        with self.lock:
            self._derived[cache_key] = result
        return result
