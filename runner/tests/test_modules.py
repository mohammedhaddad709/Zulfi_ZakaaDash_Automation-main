"""Vault, stores, system configuration, reliability rules and schedules (spec sections 4, 10, 12, 13).

Run:  python -m unittest discover -s runner/tests -v
"""

import json
import sys
import unittest
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from support import LOGIN, RunnerTestCase, login_methods, server  # noqa: E402

import config  # noqa: E402
import reliability  # noqa: E402
import schedules  # noqa: E402
import store  # noqa: E402
import vault  # noqa: E402

JAVA_VECTOR = ("FHt69O9hGJI2BD+aUrW2Bwxoqis7Xp6DNebfRY+LHwI=",
               "enc:v1:FHFF6UICKsFOfLr8Xop4A9ymOECfcIAE8TMqZKL1Jv0fZXqq0NVfdDxOIL5VXg==", "Pässw0rd-زلفي")


class VaultTest(unittest.TestCase):
    def test_mod001_round_trip_and_format(self):
        key = vault.new_key()
        value = vault.encrypt("سر-123", key)
        self.assertTrue(value.startswith("enc:v1:"))
        self.assertEqual(vault.decrypt(value, key), "سر-123")
        self.assertNotEqual(vault.encrypt("سر-123", key), value, "a fresh nonce per encryption")

    def test_mod002_same_format_as_the_java_secret_vault_test_vector(self):
        key, value, plain = JAVA_VECTOR
        self.assertEqual(vault.decrypt(value, key), plain)

    def test_mod003_wrong_key_bad_key_and_plain_values(self):
        value = vault.encrypt("x", vault.new_key())
        with self.assertRaises(vault.VaultError):
            vault.decrypt(value, vault.new_key())
        with self.assertRaises(vault.VaultError):
            vault.encrypt("x", "c2hvcnQ=")
        self.assertEqual(vault.decrypt("plain", vault.new_key()), "plain")


class FakeCursor:
    def __init__(self, db):
        self.db, self.description, self.rows = db, None, []

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False

    def execute(self, sql, params=None):
        normalised = " ".join(sql.split())
        self.db.log.append((normalised, params))
        self.rows = next((rows for prefix, rows in self.db.answers.items() if normalised.startswith(prefix)), [])
        self.description = [("c",)] if sql.lstrip().upper().startswith("SELECT") else None

    def executemany(self, sql, rows):
        self.db.log.append((" ".join(sql.split()), list(rows)))
        self.description = None

    def fetchall(self):
        return self.rows


class FakeConnection:
    def __init__(self):
        self.log, self.answers = [], {}

    def cursor(self):
        return FakeCursor(self)

    def ping(self, reconnect=True):
        return True


class StoreTest(RunnerTestCase):
    def test_mod004_file_store_round_trip_without_transient_fields(self):
        files = store.FileStore(self.tmp)
        files.save_run({"id": "r1", "system": "zulfi", "status": "passed", "process": object(), "secrets": {"P": "x"}})
        self.assertEqual(files.load_runs(), [{"id": "r1", "system": "zulfi", "status": "passed"}])
        files.save_defect("zulfi", "sig", {"id": "ZUL-01"})
        files.save_schedule({"id": "s1", "name": "a"})
        files.save_schedule({"id": "s1", "name": "b"})
        files.save_config("zulfi", {"x": 1})
        files.save_test_data("zulfi", "login-data", {"ZULFI_USER": "u"})
        self.assertEqual(files.load_defects(), {"zulfi": {"sig": {"id": "ZUL-01"}}})
        self.assertEqual(files.load_schedules(), [{"id": "s1", "name": "b"}])
        self.assertEqual((files.load_configs(), files.load_test_data()),
                         ({"zulfi": {"x": 1}}, {"zulfi": {"login-data": {"ZULFI_USER": "u"}}}))
        files.delete_run("r1")
        self.assertEqual(files.load_runs(), [])

    def test_mod005_mysql_store_schema_and_test_results_split(self):
        db = FakeConnection()
        mysql = store.MySqlStore({"host": "h", "port": 3306, "user": "u", "password": "p", "database": "d"},
                                 connect=lambda settings: db)
        schema = [sql for sql, _ in db.log]
        self.assertEqual(sum("CREATE TABLE IF NOT EXISTS" in s for s in schema), 8)
        self.assertTrue(all("utf8mb4_unicode_ci" in s for s in schema))
        run = {"id": "r1", "system": "zulfi", "status": "passed", "created": "c", "fullRun": True, "process": 1,
               "summary": {"counts": {"passed": 1}, "tests": [{"fullName": "a.b", "status": "passed", "duration": 1.5}]}}
        db.log.clear()
        mysql.save_run(run)
        inserted_run = next(p for s, p in db.log if s.startswith("INSERT INTO runs"))
        self.assertNotIn('"tests"', inserted_run[5], "test results live in test_results, not in runs.data")
        self.assertNotIn("process", json.loads(inserted_run[5]))
        rows = next(p for s, p in db.log if s.startswith("INSERT INTO test_results"))
        self.assertEqual(rows[0][:7], ("r1", 0, "a.b", "passed", False, False, 1.5))
        db.log.clear()
        mysql.save_run(run)
        self.assertFalse(any(s.startswith("INSERT INTO test_results") for s, _ in db.log), "unchanged results are not rewritten")
        db.answers = {"SELECT id, data FROM runs ORDER BY created": [("r1", json.dumps({"id": "r1", "summary": {"counts": {}}}))],
                      "SELECT run_id, data FROM test_results ORDER": [("r1", json.dumps({"fullName": "a.b"}))]}
        self.assertEqual(mysql.load_runs()[0]["summary"]["tests"], [{"fullName": "a.b"}])

    def test_mod006_first_mysql_start_imports_the_json_files(self):
        files = store.FileStore(self.tmp)
        files.save_run({"id": "r1", "system": "zulfi", "created": "c"})
        files.save_defect("zulfi", "s", {"id": "ZUL-01"})
        db = FakeConnection()
        mysql = store.MySqlStore({"host": "h", "port": 1, "user": "u", "password": "p", "database": "d"},
                                 connect=lambda settings: db)
        counts = mysql.import_files(files)
        self.assertEqual(counts, {"runs": 1, "defects": 1, "schedules": 0})


class ConfigTest(RunnerTestCase):
    def payload(self, system="zulfi", **changes):
        value = config.spec(system)
        value["modules"] = config.modules(system)
        value.update(changes)
        return value

    def test_mod007_valid_edit_is_stored_and_seed_files_stay_untouched(self):
        seed = (config.SEED_ENVIRONMENTS.read_text(encoding="utf-8"), config.SEED_MODULES.read_text(encoding="utf-8"))
        payload = self.payload(parallelNote="edited")
        payload["logins"][0]["default"] = "qa.user"
        payload["logins"][1]["default"] = "must-not-be-saved"
        config.save("zulfi", payload, [c["name"] for c in server.catalog()["zulfi"]["classes"]])
        self.assertEqual(config.spec("zulfi")["parallelNote"], "edited")
        self.assertEqual(config.spec("zulfi")["logins"][1]["default"], "", "secret defaults are never kept")
        stored = json.loads((self.tmp / "config-store.json").read_text(encoding="utf-8"))
        self.assertNotIn("must-not-be-saved", json.dumps(stored))
        self.assertEqual(stored["testData"]["zulfi"]["login-data"], {"ZULFI_USER": "qa.user"})
        self.assertEqual(seed, (config.SEED_ENVIRONMENTS.read_text(encoding="utf-8"), config.SEED_MODULES.read_text(encoding="utf-8")))
        config.reset("zulfi")
        self.assertNotEqual(config.spec("zulfi")["parallelNote"], "edited")

    def test_mod008_invalid_edits_are_rejected(self):
        broken = {
            "http on https field": lambda p: p["envs"]["current"].update({"Portal URL": "http://x.test"}),
            "reserved property": lambda p: p["urls"][0].update(targets=["output.dir"]),
            "duplicate env var": lambda p: p["logins"][1].update(env="ZULFI_USER"),
            "unknown module class": lambda p: p["modules"][0]["classes"].append("NoSuchTest"),
            "bad module status": lambda p: p["modules"][0].update(status="done"),
            "bad prefix": lambda p: p.update(bugPrefix="zu l"),
        }
        known = [c["name"] for c in server.catalog()["zulfi"]["classes"]]
        for name, change in broken.items():
            with self.subTest(case=name):
                payload = self.payload()
                change(payload)
                with self.assertRaises(config.ConfigError):
                    config.validate("zulfi", payload, known)
        with self.assertRaises(config.ConfigError):
            config.validate("zakaadash", self.payload("zakaadash", maxThreads=2))


class ReliabilityTest(RunnerTestCase):
    def test_mod009_failure_classification(self):
        cases = {"environment": "net::ERR_CONNECTION_REFUSED at https://x", "locator": "no such element: Unable to locate element",
                 "timeout": "Expected condition failed: waiting for x (tried for 25 second(s))",
                 "data": "Name already exists", "assertion": "expected [true] but found [false]", "other": "weird"}
        for kind, message in cases.items():
            with self.subTest(kind=kind):
                self.assertEqual(reliability.classify(message), kind)
        self.assertEqual(reliability.classify("no such element", soft=True), "assertion", "bug checks are assertions")
        self.assertEqual(reliability.classify("", category="ENVIRONMENT_ERROR"), "environment")
        self.assertEqual(reliability.classify("Step failed: rows — expected: No duplicate number — actual: #47 twice",
                                              category="PRODUCT_DEFECT"), "assertion",
                         "the framework's classification beats a word in the expectation text")
        self.assertEqual(reliability.classify("no such element", category="AUTOMATION_DEFECT"), "locator")

    def test_mod010_flaky_score_auto_quarantine_and_release(self):
        test = f"{LOGIN}.{login_methods()[0]}"
        pattern = ["passed", "failed", "passed", "failed", "passed"]
        for i, state in enumerate(pattern):
            self.add_run(f"r{i}", f"2026-11-0{i + 1}T10:00:00", [(test, state, "boom" if state == "failed" else "")])
        runs = list(server.RUNS.values())
        flaky = reliability.flaky(runs, "zulfi")[0]
        self.assertEqual((flaky["flips"], flaky["sequence"]), (4, "PFPFP"))
        self.assertEqual(flaky["score"], round(4 / (reliability.WINDOW - 1), 2))
        changes = reliability.auto_quarantine(runs, "zulfi")
        self.assertEqual([c["on"] for c in changes], [True])
        for i in range(reliability.STABLE):
            self.add_run(f"s{i}", f"2026-11-1{i}T10:00:00", [(test, "passed")])
        changes = reliability.auto_quarantine(list(server.RUNS.values()), "zulfi")
        self.assertEqual([c["on"] for c in changes], [False], "released after STABLE consecutive passes")

    def test_mod011_user_release_is_respected_by_auto_quarantine(self):
        test = f"{LOGIN}.{login_methods()[1]}"
        for i, state in enumerate(["passed", "failed", "passed", "failed"]):
            self.add_run(f"r{i}", f"2026-11-0{i + 1}T10:00:00", [(test, state)])
        reliability.set_quarantine("zulfi", test, False, by="user")
        self.assertEqual(reliability.auto_quarantine(list(server.RUNS.values()), "zulfi"), [])

    def test_mod012_retry_passes_count_as_flaky_and_cut_off_runs(self):
        test = f"{LOGIN}.{login_methods()[2]}"
        self.add_run("r1", "2026-11-01T10:00:00", [(test, "passed", "", {"flaky": True, "retried": 1})])
        self.add_run("r2", "2026-11-02T10:00:00", [], status="stopped", estimated=10)
        self.add_run("r3", "2026-11-03T10:00:00", [(test, "passed")], estimated=10)
        runs = list(server.RUNS.values())
        self.assertEqual(reliability.flaky(runs, "zulfi")[0]["retries"], 1)
        self.assertEqual([c["id"] for c in reliability.cut_off_runs(runs, "zulfi")], ["r3", "r2"])
        candidates = reliability.heal_candidates([{"status": "failed", "message": "no such element"},
                                                  {"status": "failed", "message": "expected [a] but found [b]"},
                                                  {"status": "failed", "soft": True, "message": "no such element"}])
        self.assertEqual(len(candidates), 1)


class FakeSMTP:
    sent = []

    def __init__(self, host, port, timeout=None):
        self.calls = [("connect", host, port)]

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False

    def starttls(self, context=None):
        self.calls.append(("starttls",))

    def login(self, user, password):
        self.calls.append(("login", user))

    def send_message(self, message):
        FakeSMTP.sent.append((self.calls + [("send",)], message))


class ScheduleTest(RunnerTestCase):
    def setUp(self):
        super().setUp()
        self._mail = (schedules.GMAIL_USER, schedules.GMAIL_APP_PASSWORD, schedules._smtp["factory"])
        schedules.GMAIL_USER, schedules.GMAIL_APP_PASSWORD = "runner.bot@gmail.com", "abcdabcdabcdabcd"
        schedules._smtp["factory"] = FakeSMTP
        FakeSMTP.sent = []

    def tearDown(self):
        schedules.GMAIL_USER, schedules.GMAIL_APP_PASSWORD, schedules._smtp["factory"] = self._mail
        super().tearDown()

    def schedule(self, **changes):
        payload = {"name": "Daily Zulfi", "trigger": {"time": "08:00", "days": [0, 1, 2, 3, 6]},
                   "run": {"system": "zulfi", "scope": "classes", "classes": ["LoginRegressionTest"],
                           "logins": {"ZULFI_PASSWORD": "secret!"}},
                   "notify": {"emails": ["QA.Lead@gmail.com", "qa.lead@gmail.com"]}}
        payload.update(changes)
        return schedules.save(payload, server.schedule_request)

    def test_mod013_validation_and_no_stored_logins(self):
        saved = self.schedule()
        self.assertEqual(saved["notify"]["emails"], ["qa.lead@gmail.com"])
        self.assertNotIn("logins", saved["run"])
        self.assertNotIn("secret!", (self.tmp / "schedules.json").read_text(encoding="utf-8"))
        bad = {"name": {"name": ""}, "time": {"trigger": {"time": "25:00", "days": [1]}},
               "days": {"trigger": {"time": "08:00", "days": []}},
               "not gmail": {"notify": {"emails": ["boss@company.com"]}},
               "too many": {"notify": {"emails": [f"u{i}@gmail.com" for i in range(21)]}},
               "bad run": {"run": {"system": "zulfi", "scope": "classes"}}}
        for name, change in bad.items():
            with self.subTest(case=name):
                with self.assertRaises((schedules.ScheduleError, server.BadRequest)):
                    self.schedule(**change)

    def test_mod014_next_occurrence_respects_weekdays(self):
        trigger = {"time": "08:00", "days": [0]}  # Mondays
        self.assertEqual(schedules.next_at(trigger, datetime(2026, 11, 7, 9, 0)), datetime(2026, 11, 9, 8, 0))
        self.assertEqual(schedules.next_at(trigger, datetime(2026, 11, 9, 7, 59)), datetime(2026, 11, 9, 8, 0))
        self.assertEqual(schedules.next_at(trigger, datetime(2026, 11, 9, 8, 0)), datetime(2026, 11, 16, 8, 0))

    def test_mod015_timer_fires_due_schedules_and_skips_long_missed_ones(self):
        saved = self.schedule()
        created = []
        due = datetime.fromisoformat(saved["nextAt"])
        fired = schedules.tick(lambda request: created.append(request) or {"id": "run-1"}, at=due)
        self.assertEqual(fired, [saved["id"]])
        self.assertEqual(created[0]["schedule"], {"id": saved["id"], "name": "Daily Zulfi"})
        live = schedules.get(saved["id"])
        self.assertEqual((live["lastRunId"], live["lastReason"]), ("run-1", "timer"))
        self.assertGreater(datetime.fromisoformat(live["nextAt"]), due)
        late = datetime.fromisoformat(live["nextAt"]) + schedules.MISSED_GRACE + schedules.timedelta(minutes=1)
        self.assertEqual(schedules.tick(lambda request: {"id": "x"}, at=late), [])
        self.assertIn("missed", schedules.get(saved["id"])["lastReason"])

    def test_mod016_notification_mail_and_only_failures(self):
        saved = self.schedule()
        run = {"id": "r1", "system": "zulfi", "env": "current", "status": "finished with failures", "scope": "classes",
               "started": "2026-11-07T08:00:00", "finished": "2026-11-07T08:03:30", "schedule": {"id": saved["id"]},
               "summary": {"counts": {"passed": 3, "failed": 1, "broken": 0, "skipped": 0, "bugChecks": 0},
                           "tests": [{"status": "failed", "name": "t", "testId": "LOGIN-UI-001", "message": "boom <b>"}]}}
        self.assertEqual(schedules.notify(run, "Zulfi Portal"), "sent to 1 Gmail address(es)")
        calls, message = FakeSMTP.sent[0]
        self.assertEqual([c[0] for c in calls], ["connect", "starttls", "login", "send"])
        self.assertEqual(calls[0][1:], ("smtp.gmail.com", 587))
        html = message.get_body(("html",)).get_content()
        self.assertIn("LOGIN-UI-001", html)
        self.assertIn("boom &lt;b&gt;", html, "content is escaped")
        self.assertIn("#run=r1", html)
        schedules.save({**saved, "notify": {"emails": saved["notify"]["emails"], "onlyFailures": True}}, server.schedule_request)
        self.assertEqual(schedules.notify({**run, "status": "passed"}), "skipped (passed; failures only)")


if __name__ == "__main__":
    unittest.main(verbosity=2)
