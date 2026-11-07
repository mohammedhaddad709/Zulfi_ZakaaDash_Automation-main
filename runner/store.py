"""Results persistence: FileStore (JSON files, default) or MySqlStore (RUNNER_DB_HOST set).

Results = runs + per-test results, defect registry, schedules, report-file index, system configuration.
Reports (Allure HTML, screenshots, console log) are files and are handled by reports.py; deleting them never
touches results kept here.

FileStore layout:
    runner/runs/<id>/run.json        one run, test results inside summary.tests
    runner/runs/<id>/files.json      report files uploaded to Spaces
    runner/bugs.json                 defect registry: system -> signature -> entry
    runner/schedules.json            [schedule]
    runner/config-store.json         Systems-page edits: {"configs", "modules", "testData"} (seeds stay untouched)
"""

import copy
import hashlib
import json
import os
import ssl
import threading
from pathlib import Path

TRANSIENT = {"process", "secrets", "args", "props"}


def clean_run(run):
    """Copy of a run without transient (in-memory only) fields."""
    return {k: v for k, v in run.items() if k not in TRANSIENT and not k.startswith("_")}


def sha256(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp")
    tmp.write_text(json.dumps(value, indent=1, ensure_ascii=False), encoding="utf-8")
    os.replace(tmp, path)


def _read_json(path, default):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return default


class FileStore:
    kind = "files"

    def __init__(self, runner_dir):
        self.dir = Path(runner_dir)
        self.runs_dir = self.dir / "runs"
        self.lock = threading.RLock()

    def describe(self):
        return {"kind": self.kind, "detail": "JSON files in " + str(self.dir)}

    # ---------------------------------------------------------------- runs
    def save_run(self, run):
        folder = self.runs_dir / run["id"]
        with self.lock:
            folder.mkdir(parents=True, exist_ok=True)
            _write_json(folder / "run.json", clean_run(run))

    def load_runs(self):
        runs = []
        if not self.runs_dir.is_dir():
            return runs
        for path in sorted(self.runs_dir.glob("*/run.json")):
            run = _read_json(path, None)
            if isinstance(run, dict) and run.get("id"):
                runs.append(run)
        return runs

    def delete_run(self, run_id):
        with self.lock:
            for name in ("run.json", "files.json"):
                try:
                    (self.runs_dir / run_id / name).unlink()
                except OSError:
                    pass

    # ---------------------------------------------------------------- defects
    def load_defects(self):
        data = _read_json(self.dir / "bugs.json", {})
        return data if isinstance(data, dict) else {}

    def save_defect(self, system, signature, entry):
        with self.lock:
            data = self.load_defects()
            data.setdefault(system, {})[signature] = entry
            _write_json(self.dir / "bugs.json", data)

    # ---------------------------------------------------------------- schedules
    def load_schedules(self):
        data = _read_json(self.dir / "schedules.json", [])
        return data if isinstance(data, list) else []

    def save_schedule(self, schedule):
        with self.lock:
            items = [s for s in self.load_schedules() if s.get("id") != schedule["id"]] + [schedule]
            _write_json(self.dir / "schedules.json", items)

    def delete_schedule(self, schedule_id):
        with self.lock:
            _write_json(self.dir / "schedules.json", [s for s in self.load_schedules() if s.get("id") != schedule_id])

    # ---------------------------------------------------------------- report files (Spaces index)
    def load_run_files(self, run_id):
        data = _read_json(self.runs_dir / run_id / "files.json", [])
        return data if isinstance(data, list) else []

    def save_run_files(self, run_id, files):
        with self.lock:
            _write_json(self.runs_dir / run_id / "files.json", files)

    def delete_run_files(self, run_id):
        with self.lock:
            try:
                (self.runs_dir / run_id / "files.json").unlink()
            except OSError:
                pass

    # ---------------------------------------------------------------- system configuration
    def _config_doc(self):
        data = _read_json(self.dir / "config-store.json", {})
        return data if isinstance(data, dict) else {}

    def _config_put(self, section, key, value, sub=None):
        with self.lock:
            data = self._config_doc()
            bucket = data.setdefault(section, {})
            if sub is None:
                bucket[key] = value
            else:
                bucket.setdefault(key, {})[sub] = value
            _write_json(self.dir / "config-store.json", data)

    def load_configs(self):
        return self._config_doc().get("configs", {})

    def save_config(self, system, data):
        self._config_put("configs", system, data)

    def load_modules(self):
        return self._config_doc().get("modules", {})

    def save_modules(self, system, data):
        self._config_put("modules", system, data)

    def load_test_data(self):
        return self._config_doc().get("testData", {})

    def save_test_data(self, system, file_name, data):
        self._config_put("testData", system, data, sub=file_name)


SCHEMA = [
    """CREATE TABLE IF NOT EXISTS runs (
        id VARCHAR(40) PRIMARY KEY,
        system_key VARCHAR(40) NOT NULL,
        status VARCHAR(40),
        created VARCHAR(25),
        full_run BOOLEAN NOT NULL DEFAULT FALSE,
        data JSON NOT NULL,
        INDEX runs_system_created (system_key, created)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS test_results (
        run_id VARCHAR(40) NOT NULL,
        seq INT NOT NULL,
        full_name VARCHAR(500),
        status VARCHAR(16),
        soft BOOLEAN NOT NULL DEFAULT FALSE,
        flaky BOOLEAN NOT NULL DEFAULT FALSE,
        duration DOUBLE,
        data JSON NOT NULL,
        PRIMARY KEY (run_id, seq),
        INDEX test_results_name (full_name(191)),
        FOREIGN KEY (run_id) REFERENCES runs (id) ON DELETE CASCADE
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS defects (
        system_key VARCHAR(40) NOT NULL,
        signature_hash CHAR(64) NOT NULL,
        signature TEXT NOT NULL,
        bug_id VARCHAR(40),
        severity VARCHAR(16),
        data JSON NOT NULL,
        PRIMARY KEY (system_key, signature_hash)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS schedules (
        id VARCHAR(16) PRIMARY KEY,
        data JSON NOT NULL
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS run_files (
        run_id VARCHAR(40) NOT NULL,
        path_hash CHAR(64) NOT NULL,
        path TEXT NOT NULL,
        space_key TEXT NOT NULL,
        content_type VARCHAR(100),
        size BIGINT,
        uploaded VARCHAR(25),
        PRIMARY KEY (run_id, path_hash),
        FOREIGN KEY (run_id) REFERENCES runs (id) ON DELETE CASCADE
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS system_configs (
        system_key VARCHAR(40) PRIMARY KEY,
        data JSON NOT NULL
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS system_modules (
        system_key VARCHAR(40) PRIMARY KEY,
        data JSON NOT NULL
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS test_data (
        system_key VARCHAR(40) NOT NULL,
        file_name VARCHAR(80) NOT NULL,
        data JSON NOT NULL,
        PRIMARY KEY (system_key, file_name)
    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci""",
]


def _dumps(value):
    return json.dumps(value, ensure_ascii=False)


class MySqlStore:
    kind = "mysql"

    def __init__(self, settings, connect=None):
        """settings: host, port, user, password, database, ssl_ca, ssl_required. connect: injectable for tests."""
        self.settings = settings
        self._connect_fn = connect
        self.lock = threading.RLock()
        self.conn = None
        self._tests_hash = {}  # run id -> hash of the stored test results (skip rewriting unchanged results)
        self._connect(create_database=True)
        with self.lock:
            for statement in SCHEMA:
                self._execute(statement)

    def describe(self):
        s = self.settings
        return {"kind": self.kind, "detail": f"MySQL {s['user']}@{s['host']}:{s['port']}/{s['database']}"}

    def _ssl(self):
        if self.settings.get("ssl_ca"):
            return ssl.create_default_context(cafile=self.settings["ssl_ca"])
        if self.settings.get("ssl_required"):
            context = ssl.create_default_context()
            context.check_hostname = False
            context.verify_mode = ssl.CERT_NONE
            return context
        return None

    def _connect(self, create_database=False):
        if self._connect_fn:
            self.conn = self._connect_fn(self.settings)
            return
        import pymysql
        s = self.settings
        options = dict(host=s["host"], port=int(s["port"]), user=s["user"], password=s["password"],
                       charset="utf8mb4", autocommit=True, connect_timeout=10)
        context = self._ssl()
        if context is not None:
            options["ssl"] = context
        if create_database:
            bootstrap = pymysql.connect(**options)
            try:
                with bootstrap.cursor() as cursor:
                    cursor.execute("CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                                   % s["database"].replace("`", ""))
            finally:
                bootstrap.close()
        self.conn = pymysql.connect(database=s["database"], **options)

    def _cursor(self):
        try:
            self.conn.ping(reconnect=True)
        except Exception:
            self._connect()
        return self.conn.cursor()

    def _execute(self, sql, params=None, many=False):
        with self.lock, self._cursor() as cursor:
            if many:
                cursor.executemany(sql, params)
            else:
                cursor.execute(sql, params)
            return cursor.fetchall() if cursor.description else []

    # ---------------------------------------------------------------- runs
    def save_run(self, run):
        data = clean_run(run)
        summary = copy.deepcopy(data.get("summary") or {})
        tests = summary.pop("tests", None) or []
        if data.get("summary") is not None:
            data["summary"] = summary
        tests_hash = sha256(_dumps(tests))
        with self.lock:
            self._execute(
                "INSERT INTO runs (id, system_key, status, created, full_run, data) VALUES (%s, %s, %s, %s, %s, %s) "
                "ON DUPLICATE KEY UPDATE system_key=VALUES(system_key), status=VALUES(status), created=VALUES(created), "
                "full_run=VALUES(full_run), data=VALUES(data)",
                (run["id"], run.get("system", ""), run.get("status"), run.get("created"), bool(run.get("fullRun")),
                 _dumps(data)))
            if self._tests_hash.get(run["id"]) == tests_hash:
                return
            self._execute("DELETE FROM test_results WHERE run_id=%s", (run["id"],))
            if tests:
                self._execute(
                    "INSERT INTO test_results (run_id, seq, full_name, status, soft, flaky, duration, data) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
                    [(run["id"], index, (t.get("fullName") or "")[:500], t.get("status"), bool(t.get("soft")),
                      bool(t.get("flaky")), t.get("duration"), _dumps(t)) for index, t in enumerate(tests)], many=True)
            self._tests_hash[run["id"]] = tests_hash

    def load_runs(self):
        runs = {}
        for run_id, data in self._execute("SELECT id, data FROM runs ORDER BY created"):
            run = json.loads(data) if isinstance(data, (str, bytes)) else data
            runs[run_id] = run
        tests = {}
        for run_id, data in self._execute("SELECT run_id, data FROM test_results ORDER BY run_id, seq"):
            tests.setdefault(run_id, []).append(json.loads(data) if isinstance(data, (str, bytes)) else data)
        for run_id, run in runs.items():
            if run_id in tests:
                run.setdefault("summary", {})["tests"] = tests[run_id]
                self._tests_hash[run_id] = sha256(_dumps(tests[run_id]))
            elif isinstance(run.get("summary"), dict):
                run["summary"].setdefault("tests", [])
        return list(runs.values())

    def delete_run(self, run_id):
        self._execute("DELETE FROM runs WHERE id=%s", (run_id,))
        self._tests_hash.pop(run_id, None)

    # ---------------------------------------------------------------- defects
    def load_defects(self):
        result = {}
        for system, signature, data in self._execute("SELECT system_key, signature, data FROM defects"):
            result.setdefault(system, {})[signature] = json.loads(data) if isinstance(data, (str, bytes)) else data
        return result

    def save_defect(self, system, signature, entry):
        self._execute(
            "INSERT INTO defects (system_key, signature_hash, signature, bug_id, severity, data) VALUES (%s,%s,%s,%s,%s,%s) "
            "ON DUPLICATE KEY UPDATE signature=VALUES(signature), bug_id=VALUES(bug_id), severity=VALUES(severity), "
            "data=VALUES(data)",
            (system, sha256(signature), signature, entry.get("id"), entry.get("severity"), _dumps(entry)))

    # ---------------------------------------------------------------- schedules
    def load_schedules(self):
        return [json.loads(d) if isinstance(d, (str, bytes)) else d for (d,) in self._execute("SELECT data FROM schedules")]

    def save_schedule(self, schedule):
        self._execute("INSERT INTO schedules (id, data) VALUES (%s, %s) ON DUPLICATE KEY UPDATE data=VALUES(data)",
                      (schedule["id"], _dumps(schedule)))

    def delete_schedule(self, schedule_id):
        self._execute("DELETE FROM schedules WHERE id=%s", (schedule_id,))

    # ---------------------------------------------------------------- report files
    def load_run_files(self, run_id):
        rows = self._execute("SELECT path, space_key, content_type, size, uploaded FROM run_files WHERE run_id=%s",
                             (run_id,))
        return [{"path": p, "key": k, "contentType": c, "size": s, "uploaded": u} for p, k, c, s, u in rows]

    def save_run_files(self, run_id, files):
        with self.lock:
            self._execute("DELETE FROM run_files WHERE run_id=%s", (run_id,))
            if files:
                self._execute(
                    "INSERT INTO run_files (run_id, path_hash, path, space_key, content_type, size, uploaded) "
                    "VALUES (%s,%s,%s,%s,%s,%s,%s)",
                    [(run_id, sha256(f["path"]), f["path"], f["key"], f.get("contentType"), f.get("size"),
                      f.get("uploaded")) for f in files], many=True)

    def delete_run_files(self, run_id):
        self._execute("DELETE FROM run_files WHERE run_id=%s", (run_id,))

    # ---------------------------------------------------------------- system configuration
    def _load_keyed(self, table):
        return {key: json.loads(d) if isinstance(d, (str, bytes)) else d
                for key, d in self._execute(f"SELECT system_key, data FROM {table}")}

    def load_configs(self):
        return self._load_keyed("system_configs")

    def save_config(self, system, data):
        self._execute("INSERT INTO system_configs (system_key, data) VALUES (%s,%s) ON DUPLICATE KEY UPDATE data=VALUES(data)",
                      (system, _dumps(data)))

    def load_modules(self):
        return self._load_keyed("system_modules")

    def save_modules(self, system, data):
        self._execute("INSERT INTO system_modules (system_key, data) VALUES (%s,%s) ON DUPLICATE KEY UPDATE data=VALUES(data)",
                      (system, _dumps(data)))

    def load_test_data(self):
        result = {}
        for system, name, data in self._execute("SELECT system_key, file_name, data FROM test_data"):
            result.setdefault(system, {})[name] = json.loads(data) if isinstance(data, (str, bytes)) else data
        return result

    def save_test_data(self, system, file_name, data):
        self._execute("INSERT INTO test_data (system_key, file_name, data) VALUES (%s,%s,%s) "
                      "ON DUPLICATE KEY UPDATE data=VALUES(data)", (system, file_name, _dumps(data)))

    # ---------------------------------------------------------------- first start
    def is_empty(self):
        return not self._execute("SELECT id FROM runs LIMIT 1")

    def import_files(self, files):
        """Copy a FileStore's results (runs, defects, schedules, report index) into this database."""
        counts = {"runs": 0, "defects": 0, "schedules": 0}
        for run in files.load_runs():
            self.save_run(run)
            entries = files.load_run_files(run["id"])
            if entries:
                self.save_run_files(run["id"], entries)
            counts["runs"] += 1
        for system, entries in files.load_defects().items():
            for signature, entry in entries.items():
                self.save_defect(system, signature, entry)
                counts["defects"] += 1
        for schedule in files.load_schedules():
            self.save_schedule(schedule)
            counts["schedules"] += 1
        return counts


def mysql_settings():
    host = os.environ.get("RUNNER_DB_HOST", "").strip()
    if not host:
        return None
    return {
        "host": host,
        "port": int(os.environ.get("RUNNER_DB_PORT", "3306") or 3306),
        "user": os.environ.get("RUNNER_DB_USER", "runner"),
        "password": os.environ.get("RUNNER_DB_PASSWORD", ""),
        "database": os.environ.get("RUNNER_DB_NAME", "automation_runner"),
        "ssl_ca": os.environ.get("RUNNER_DB_SSL_CA", "").strip() or None,
        "ssl_required": os.environ.get("RUNNER_DB_SSL", "").strip().lower() == "required",
    }


def open_store(runner_dir, log=print):
    """MySQL when RUNNER_DB_HOST is set (first start imports the JSON files), otherwise FileStore."""
    files = FileStore(runner_dir)
    settings = mysql_settings()
    if not settings:
        return files
    database = MySqlStore(settings)
    if database.is_empty():
        counts = database.import_files(files)
        if any(counts.values()):
            log("MySQL first start: imported %(runs)d runs, %(defects)d defects, %(schedules)d schedules" % counts)
    return database
