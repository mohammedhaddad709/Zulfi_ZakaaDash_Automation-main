"""Automation Control Center - embedded runner for the Zulfi + ZakaaDash TAF (Python 3.10+ standard library;
optional packages in requirements.txt).

    python runner/server.py --port 8080      ->  http://localhost:8080
    python runner/server.py --stop           stop the runner that is running

Principles (see docs/runner/RUNNER_REBUILD_PLAN.md):
* Zero project-file mutation: URLs typed in the UI become -D properties of one run; logins become environment
  variables of that run's JVM only (AGENTS.md) and are never written anywhere.
* Snapshot isolation: sources are compiled into runner/.build/<id>; every run keeps its snapshot.
* Same-system exclusion: runs of one system (or of systems on the same server) never overlap.
* Results != reports: results live in the store (JSON files or MySQL); reports are files (optionally in Spaces).
"""

import os
import sys
from pathlib import Path

RUNNER_DIR = Path(__file__).resolve().parent
ROOT = RUNNER_DIR.parent


def load_env_file(path):
    """KEY=VALUE lines of runner/.env (git-ignored); variables already in the environment win."""
    try:
        lines = Path(path).read_text(encoding="utf-8").splitlines()
    except OSError:
        return 0
    count = 0
    for line in lines:
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key, value = key.strip(), value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        if key and key not in os.environ:
            os.environ[key] = value
            count += 1
    return count


load_env_file(RUNNER_DIR / ".env")
sys.path.insert(0, str(ROOT / "tools"))
sys.path.insert(0, str(RUNNER_DIR))

import argparse  # noqa: E402
import base64  # noqa: E402
import hmac  # noqa: E402
import io  # noqa: E402
import json  # noqa: E402
import re  # noqa: E402
import secrets  # noqa: E402
import shutil  # noqa: E402
import signal  # noqa: E402
import socket  # noqa: E402
import statistics  # noqa: E402
import subprocess  # noqa: E402
import tempfile  # noqa: E402
import threading  # noqa: E402
import time  # noqa: E402
import urllib.error  # noqa: E402
import urllib.request  # noqa: E402
import zipfile  # noqa: E402
from datetime import datetime, timedelta  # noqa: E402
from http import HTTPStatus  # noqa: E402
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer  # noqa: E402
from urllib.parse import parse_qs, quote, unquote, urlparse  # noqa: E402
from xml.sax.saxutils import quoteattr  # noqa: E402

import agent  # noqa: E402
import bugreport  # noqa: E402
import config  # noqa: E402
import orchestrator  # noqa: E402
import reliability  # noqa: E402
import reports  # noqa: E402
import schedules  # noqa: E402
import store as store_module  # noqa: E402
from archive import ArchiveIndex, bucket  # noqa: E402
from gen_suites import plan  # noqa: E402
from regression import regression_classes  # noqa: E402
from systems import SYSTEMS  # noqa: E402
from testsource import discover, with_dependencies  # noqa: E402

DATA_DIR = Path(os.environ.get("RUNNER_DATA_DIR") or RUNNER_DIR)   # results/state (runs, registry, schedules...)
RUNS_DIR = DATA_DIR / "runs"
BUILD_DIR = RUNNER_DIR / ".build"
STATIC_DIR = RUNNER_DIR / "static"
PID_FILE = RUNNER_DIR / "runner.pid"
LEGACY_LOCK = RUNNER_DIR / ".runner.lock"
UPTIME_FILE = DATA_DIR / "uptime.jsonl"
PRESETS_FILE = RUNNER_DIR / "presets.json"
SUITES_DIR = ROOT / "suites"
TMP_DIR = RUNNER_DIR / ".tmp"


def _int_env(name, default):
    try:
        return int(os.environ.get(name, default))
    except ValueError:
        return default


MAX_PARALLEL = max(1, _int_env("RUNNER_MAX_PARALLEL", 4))
DEFAULT_EXECUTION = os.environ.get("RUNNER_EXECUTION", "LocalHeadless")
UPTIME_ENABLED = os.environ.get("RUNNER_UPTIME", "1") != "0"
UPTIME_EVERY = max(30, _int_env("RUNNER_UPTIME_EVERY", 300))
GITHUB_REPO = os.environ.get("RUNNER_GITHUB_REPO", "").strip()
GITHUB_BRANCH = os.environ.get("RUNNER_GITHUB_BRANCH", "main").strip() or "main"
GITHUB_TOKEN = os.environ.get("RUNNER_GITHUB_TOKEN", "").strip()
ALLOW_UPDATE = os.environ.get("RUNNER_ALLOW_UPDATE", "") == "1"
AUTH_USER = os.environ.get("RUNNER_USER", "admin")
AUTH_PASSWORD = os.environ.get("RUNNER_PASSWORD", "")
KEEP_SNAPSHOTS = 3
RUN_LIST_LIMIT = 60
LOG_TAIL = 30_000
SOURCE_CACHE_SECONDS = 10

TERMINAL = {"passed", "finished with failures", "failed to start", "environment down", "stopped", "cancelled",
            "error", "interrupted"}
ACTIVE = {"building", "running", "stopping"}
COMPLETED = {"passed", "finished with failures"}
EXECUTED = {"passed", "failed", "broken"}
FAILED = {"failed", "broken"}
SCOPES = ("full", "e2e", "e2e-only", "classes", "modules", "retest", "retest-tests", "retest-one")
EXECUTIONS = {"LocalHeadless": "LocalHeadless", "Local": "Local", "Remote": "Remote",
              "headless": "LocalHeadless", "local": "Local", "grid": "Remote"}
BROWSERS = ("Chrome", "Edge", "Firefox")
PACES = ("fast", "normal", "demo")
SEVERITY_RANK = {"Critical": 0, "High": 1, "Medium": 2, "Low": 3, None: 4}
PROPERTY_KEY = re.compile(r"^[A-Za-z0-9_.\-]{1,120}$")
RESERVED_KEYS = {"output.dir", "allure.results.directory", "reports.dir", "active.system", "build.dir",
                 "parallel.classes"}
SUBMIT_GATE = re.compile(r"\.(create|submit|lifecycle|creation)\.enabled$", re.I)
PROGRESS_TAF = re.compile(r"\[TAF-PROGRESS\] (START|PASS|FAIL|SKIP) (\S+)#(\S+)")
PROGRESS_SPEC = re.compile(r"Test Case (\S+) \((passed|failed|skipped)\)")
RETRY_LINE = re.compile(r"\[TAF-RETRY\] (\S+)#(\S+)")
RUN_ID = re.compile(r"^[\w\-]{1,60}$")

lock = threading.RLock()            # runs dict and run state
build_lock = threading.Lock()       # one Maven build at a time
registry_lock = threading.RLock()   # defect registry writes
uptime_lock = threading.Lock()      # uptime.jsonl writes
wake = threading.Event()            # wakes the scheduler

RUNS = {}         # id -> run (persisted without transient fields)
TRANSIENT = {}    # id -> {"process", "secrets", "args", "props"} never persisted
STORE = None
BUILD = {"status": "none", "id": None, "built": None, "fingerprint": None, "message": "", "reason": "",
         "started": None, "seconds": None}
SNAPSHOTS = {}    # fingerprint -> snapshot id
UPDATE = {"status": "idle", "message": "", "at": None}
_source_cache = {"at": 0.0, "value": None}
_catalog_cache = {"fingerprint": None, "value": None, "discovered": None}
_storage_cache = {"at": 0.0, "value": None}
ARCHIVE = ArchiveIndex(ROOT / "test-execution-reports" / "history", RUNS_DIR, RUNNER_DIR / ".archive-cache.json")


class BadRequest(Exception):
    pass


class NotFound(Exception):
    pass


def now():
    return datetime.now().replace(microsecond=0)


def now_iso():
    return now().isoformat(timespec="seconds")


def parse_time(value):
    try:
        return datetime.fromisoformat(value) if value else None
    except (TypeError, ValueError):
        return None


def log(*parts):
    print(datetime.now().strftime("%H:%M:%S"), *parts, flush=True)


# ============================================================================================= tools
def maven_command():
    configured = os.environ.get("MVN")
    executable = configured or shutil.which("mvn.cmd" if os.name == "nt" else "mvn") or shutil.which("mvn")
    if not executable:
        raise RuntimeError("Maven not found on PATH; set MVN to the mvn executable")
    return [executable]


def java_command():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")
        if candidate.is_file():
            return str(candidate)
    found = shutil.which("java")
    if not found:
        raise RuntimeError("java not found; install Java 21 or set JAVA_HOME")
    return found


def allure_cli():
    configured = os.environ.get("ALLURE_CLI")
    if configured and Path(configured).is_file():
        return configured
    name = "allure.bat" if os.name == "nt" else "allure"
    for bundled in sorted((ROOT / "tools").glob("allure-2.*/bin/" + name), reverse=True):
        return str(bundled)
    return shutil.which("allure")


def process_flags():
    return {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP} if os.name == "nt" else {"start_new_session": True}


def kill_tree(proc):
    if proc is None or proc.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"], capture_output=True)
    else:
        try:
            os.killpg(proc.pid, signal.SIGTERM)
        except OSError:
            proc.terminate()


def kill_pid(pid):
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"], capture_output=True)
    else:
        try:
            os.killpg(os.getpgid(pid), signal.SIGTERM)
        except OSError:
            try:
                os.kill(pid, signal.SIGTERM)
            except OSError:
                pass


def process_alive(pid):
    if pid <= 0:
        return False
    if os.name == "nt":
        import ctypes
        handle = ctypes.windll.kernel32.OpenProcess(0x1000, False, pid)
        if not handle:
            return False
        code = ctypes.c_ulong()
        ctypes.windll.kernel32.GetExitCodeProcess(handle, ctypes.byref(code))
        ctypes.windll.kernel32.CloseHandle(handle)
        return code.value == 259
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def masked(text, secret_values):
    for value in sorted((v for v in secret_values if v), key=len, reverse=True):
        text = text.replace(value, "****")
    return text


# ============================================================================================= catalogue
def sources_fingerprint():
    """Fingerprint of pom.xml + src/ (paths, sizes, mtimes), cached for 10 s."""
    if _source_cache["value"] and time.monotonic() - _source_cache["at"] < SOURCE_CACHE_SECONDS:
        return _source_cache["value"]
    import hashlib
    digest = hashlib.sha1()
    newest = 0
    files = [ROOT / "pom.xml"] + sorted(p for p in (ROOT / "src").rglob("*") if p.is_file())
    for path in files:
        stat = path.stat()
        newest = max(newest, stat.st_mtime)
        digest.update(f"{path.relative_to(ROOT).as_posix()}|{stat.st_size}|{stat.st_mtime_ns}\n".encode())
    value = {"fingerprint": digest.hexdigest()[:12], "mtime": datetime.fromtimestamp(newest).isoformat(timespec="seconds")}
    _source_cache.update(at=time.monotonic(), value=value)
    return value


def refresh_catalog():
    _source_cache["at"] = 0.0
    _catalog_cache["fingerprint"] = None


def discovered():
    catalog()
    return _catalog_cache["discovered"]


def local_credential_names():
    names = set()
    try:
        for line in (ROOT / "config" / "credentials.local.properties").read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                if value.strip():
                    names.add(key.strip())
    except OSError:
        pass
    return names


def public_logins(spec):
    local = local_credential_names()
    rows = []
    for login in spec.get("logins", []):
        env_name = login["env"]
        source = "env" if os.environ.get(env_name) else ("local file" if env_name in local else "")
        rows.append({"label": login["label"], "env": env_name, "secret": bool(login.get("secret")),
                     "default": "" if login.get("secret") else login.get("default", ""),
                     "inherited": bool(source), "source": source})
    return rows


def presets_for(system, known_classes):
    try:
        data = json.loads(PRESETS_FILE.read_text(encoding="utf-8")).get(system, {})
    except (OSError, ValueError):
        data = {}
    presets = []
    for preset in data.get("presets", []):
        classes = [c.rsplit(".", 1)[-1] for c in preset.get("classes", []) if c.rsplit(".", 1)[-1] in known_classes]
        if classes:
            presets.append({**preset, "classes": classes})
    return {"presets": presets, "phaseKeys": data.get("phaseKeys", [])}


def catalog():
    """Systems, their classes/tests/modules/test types, rebuilt when the sources change."""
    fingerprint = sources_fingerprint()["fingerprint"]
    if _catalog_cache["fingerprint"] == fingerprint and _catalog_cache["value"]:
        return _catalog_cache["value"]
    found = discover()
    value = {}
    for key, entries in found.items():
        blocks = plan(key, entries)
        full = [c for _, classes in blocks for c in classes]
        e2e = next((classes for name, classes in blocks if name == "E2E"), [])
        regression = regression_classes(key, entries)
        excluded = {"com.taf.systems." + n for n in SYSTEMS[key].get("exclude", [])}
        order = {c: i for i, c in enumerate(full)}
        classes = []
        for entry in sorted(entries, key=lambda e: (order.get(e["class"], 10_000), e["class"])):
            methods = [{"name": m["name"], "id": m["id"], "description": m["description"], "groups": m["groups"],
                        "enabled": m["enabled"], "dependsOn": m.get("dependsOn", []),
                        "invocations": m.get("invocations", 1)} for m in entry["methods"]]
            classes.append({"name": entry["simple"], "class": entry["class"], "module": entry["module"],
                            "tests": sum(m["invocations"] for m in methods if m["enabled"]),
                            "groups": sorted({g for m in methods for g in m["groups"]}),
                            "e2e": entry["class"] in e2e, "regression": entry["class"] in regression,
                            "excluded": entry["class"] in excluded, "inFull": entry["class"] in order,
                            "methods": methods})
        group_counts = {}
        for cls in classes:
            if not cls["inFull"]:
                continue
            for method in cls["methods"]:
                if method["enabled"]:
                    for group in method["groups"]:
                        group_counts[group] = group_counts.get(group, 0) + method["invocations"]
        value[key] = {"classes": classes, "full": [c.rsplit(".", 1)[-1] for c in full],
                      "e2e": [c.rsplit(".", 1)[-1] for c in e2e], "regression": [c.rsplit(".", 1)[-1] for c in regression],
                      "groupCounts": group_counts,
                      "testCount": sum(c["tests"] for c in classes if c["inFull"])}
    _catalog_cache.update(fingerprint=fingerprint, value=value, discovered=found)
    return value


def class_entry(system, name):
    for cls in catalog().get(system, {}).get("classes", []):
        if name in (cls["name"], cls["class"]):
            return cls
    return None


def class_exists(system, name):
    return class_entry(system, name) is not None


def test_exists(full_name):
    """A failure only counts while its test method still exists in the sources."""
    if not full_name or "." not in full_name:
        return False
    cls_name, method = full_name.rsplit(".", 1)
    for system in catalog():
        cls = class_entry(system, cls_name)
        if cls:
            return any(m["name"] == method for m in cls["methods"])
    return False


def module_rows(system):
    """Configured module map, with test counts from the catalogue."""
    known = {c["name"]: c for c in catalog().get(system, {}).get("classes", [])}
    rows = []
    for module in config.modules(system):
        classes = [c for c in module.get("classes", []) if c in known]
        rows.append({**module, "classes": classes, "missing": [c for c in module.get("classes", []) if c not in known],
                     "tests": sum(known[c]["tests"] for c in classes)})
    return rows


def catalog_view():
    cat = catalog()
    systems = []
    for key, spec in config.systems().items():
        if key not in cat or key not in SYSTEMS:
            continue
        entry = cat[key]
        known = {c["name"] for c in entry["classes"]}
        systems.append({
            "key": key, "title": SYSTEMS[key]["title"], "activeSystem": spec["activeSystem"],
            "urls": spec["urls"], "envs": spec["envs"], "defaultEnv": next(iter(spec["envs"]), None),
            "logins": public_logins(spec), "classes": entry["classes"], "full": entry["full"], "e2e": entry["e2e"],
            "regression": entry["regression"], "testCount": entry["testCount"], "modules": module_rows(key),
            "moduleChoices": [m["name"] for m in config.modules(key)], "testTypes": spec.get("testTypes", []),
            "groupCounts": entry["groupCounts"], "maxThreads": spec.get("maxThreads", 1),
            "parallelNote": spec.get("parallelNote", ""), "serialGroups": spec.get("serialGroups", []),
            "bugPrefix": spec.get("bugPrefix", key[:3].upper()), "secondsPerTest": seconds_per_test(key),
            **presets_for(key, known)})
    return {"systems": systems, "defaultExecution": EXECUTIONS.get(DEFAULT_EXECUTION, "LocalHeadless"),
            "maxParallel": MAX_PARALLEL, "store": STORE.describe() if STORE else None}


# ============================================================================================= snapshots
def load_snapshots():
    if not BUILD_DIR.is_dir():
        return
    ready = []
    for directory in BUILD_DIR.iterdir():
        marker, fingerprint = directory / "READY", directory / "FINGERPRINT"
        if marker.is_file() and fingerprint.is_file():
            ready.append((marker.stat().st_mtime, fingerprint.read_text().strip(), directory.name,
                          marker.read_text().strip()))
    for _, fingerprint, snapshot, built in sorted(ready):
        SNAPSHOTS[fingerprint] = snapshot
        BUILD.update(status="ready", id=snapshot, fingerprint=fingerprint, built=built)


def build_info():
    current = sources_fingerprint()
    with lock:
        info = dict(BUILD)
    info["sourcesChanged"] = current["mtime"]
    info["current"] = current["fingerprint"]
    info["stale"] = info["status"] != "building" and current["fingerprint"] not in SNAPSHOTS
    info["update"] = {**UPDATE, "configured": bool(GITHUB_REPO), "repo": GITHUB_REPO, "branch": GITHUB_BRANCH,
                      "allowed": update_allowed()[0], "reason": update_allowed()[1]}
    return info


def build_snapshot(reason="run", force=False):
    """Snapshot id for the current sources: reused when fresh, otherwise compiled (one build at a time)."""
    with build_lock:
        refresh_catalog()
        fingerprint = sources_fingerprint()["fingerprint"]
        snapshot = SNAPSHOTS.get(fingerprint)
        if snapshot and not force and (BUILD_DIR / snapshot / "READY").is_file():
            return snapshot
        build_id = datetime.now().strftime("%Y%m%d-%H%M%S")
        directory = BUILD_DIR / build_id
        shutil.rmtree(directory, ignore_errors=True)
        directory.mkdir(parents=True)
        started = time.monotonic()
        with lock:
            BUILD.update(status="building", id=build_id, fingerprint=fingerprint, message="", reason=reason,
                         started=now_iso(), seconds=None)
        command = maven_command() + ["-B", "-q"] + os.environ.get("RUNNER_MVN_ARGS", "").split() + [
            "test-compile", "dependency:build-classpath", "-Dmdep.outputFile=" + str(directory / "classpath.txt"),
            "-Dbuild.dir=" + str(directory / "target")]
        with open(directory / "build.log", "w", encoding="utf-8", errors="replace") as handle:
            handle.write("$ " + " ".join(command) + "\n")
            handle.flush()
            code = subprocess.run(command, cwd=ROOT, stdout=handle, stderr=subprocess.STDOUT).returncode
        ok = code == 0 and (directory / "classpath.txt").is_file() and (directory / "target" / "test-classes").is_dir()
        seconds = round(time.monotonic() - started, 1)
        if not ok:
            tail = (directory / "build.log").read_text(encoding="utf-8", errors="replace")[-4000:]
            with lock:
                BUILD.update(status="failed", message=tail, seconds=seconds)
            raise RuntimeError("Build failed (mvn test-compile): see the build log")
        (directory / "FINGERPRINT").write_text(fingerprint)
        (directory / "READY").write_text(now_iso())
        with lock:
            SNAPSHOTS[fingerprint] = build_id
            BUILD.update(status="ready", built=now_iso(), message="", seconds=seconds)
        prune_snapshots()
        return build_id


def prune_snapshots():
    with lock:
        pinned = {r.get("snapshot") for r in RUNS.values() if r.get("status") in ACTIVE | {"queued"}}
        current = SNAPSHOTS.get(sources_fingerprint()["fingerprint"])
    if not BUILD_DIR.is_dir():
        return
    ready = sorted((p for p in BUILD_DIR.iterdir() if (p / "READY").is_file()),
                   key=lambda p: (p / "READY").stat().st_mtime, reverse=True)
    for directory in ready[KEEP_SNAPSHOTS:]:
        if directory.name in pinned or directory.name == current:
            continue
        shutil.rmtree(directory, ignore_errors=True)
        with lock:
            for key, value in list(SNAPSHOTS.items()):
                if value == directory.name:
                    del SNAPSHOTS[key]
    for directory in BUILD_DIR.iterdir():  # failed builds older than a day
        if not (directory / "READY").is_file() and directory.name != BUILD.get("id"):
            try:
                if time.time() - directory.stat().st_mtime > 86400:
                    shutil.rmtree(directory, ignore_errors=True)
            except OSError:
                pass


def start_manual_build():
    if BUILD["status"] == "building":
        return build_info()

    def worker():
        try:
            build_snapshot("manual", force=True)
        except Exception as error:
            log("manual build failed:", error)
    threading.Thread(target=worker, daemon=True).start()
    time.sleep(0.2)
    return build_info()


# ============================================================================================= run requests
def parse_extra_properties(text):
    props = {}
    if isinstance(text, dict):
        text = "\n".join(f"{k}={v}" for k, v in text.items())
    for number, raw in enumerate((text or "").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise BadRequest(f"Extra property line {number} must be key=value")
        key, value = (part.strip() for part in line.split("=", 1))
        if not PROPERTY_KEY.match(key) or key in RESERVED_KEYS:
            raise BadRequest(f"Extra property '{key}' is not allowed")
        if len(value) > 500 or any(c in value for c in "\r\n\x00"):
            raise BadRequest(f"Extra property '{key}' has an invalid value")
        props[key] = value
    return props


def _bool(value, default):
    if value is None:
        return default
    if isinstance(value, str):
        return value.strip().lower() in ("1", "true", "yes", "on")
    return bool(value)


def clean_request(payload, keep_secrets=True):
    """Validated run request. Returns (request, secrets) - secrets are logins typed for this run only."""
    if not isinstance(payload, dict):
        raise BadRequest("Run request must be an object")
    system = payload.get("system")
    spec = config.spec(system) if system else None
    if spec is None or system not in SYSTEMS or system not in catalog():
        raise BadRequest("Unknown system")
    env = payload.get("env") or next(iter(spec["envs"]))
    if env not in spec["envs"]:
        raise BadRequest(f"Unknown environment '{env}'")
    urls = {}
    for field in spec["urls"]:
        value = str((payload.get("urls") or {}).get(field["label"], "") or "").strip() \
            or spec["envs"][env].get(field["label"], "")
        if not value:
            raise BadRequest(f"{field['label']} is required for environment '{env}'")
        try:
            config.check_url(field["label"], value, field.get("requireHttps", False))
        except config.ConfigError as error:
            raise BadRequest(str(error))
        urls[field["label"]] = value
    scope = payload.get("scope") or "full"
    if scope not in SCOPES:
        raise BadRequest("Unknown scope")
    known = {c["name"]: c for c in catalog()[system]["classes"]}
    classes = []
    for name in payload.get("classes") or []:
        simple = str(name).rsplit(".", 1)[-1]
        if simple not in known:
            raise BadRequest(f"Unknown test class '{name}'")
        if simple not in classes:
            classes.append(simple)
    modules = [str(m) for m in payload.get("modules") or []]
    module_names = {m["name"] for m in config.modules(system)}
    if any(m not in module_names for m in modules):
        raise BadRequest("Unknown module")
    tests = {}
    for name, methods in (payload.get("tests") or {}).items():
        simple = str(name).rsplit(".", 1)[-1]
        cls = known.get(simple)
        if not cls:
            raise BadRequest(f"Unknown test class '{name}'")
        names = {m["name"] for m in cls["methods"]}
        chosen = [str(m) for m in methods or [] if str(m) in names]
        if chosen:
            tests[simple] = list(dict.fromkeys(tests.get(simple, []) + chosen))
    if scope == "classes" and not classes:
        raise BadRequest("Select at least one test class")
    if scope == "modules" and not modules:
        raise BadRequest("Select at least one module")
    if scope in ("retest-tests", "retest-one") and not tests:
        raise BadRequest("Select the tests to retest")
    if scope == "retest-one" and sum(len(v) for v in tests.values()) < 1:
        raise BadRequest("Select one test")
    group = (payload.get("group") or None)
    if group and group not in {t["group"] for t in spec.get("testTypes", [])}:
        raise BadRequest("Unknown test type")
    try:
        threads = int(payload.get("threads") or 1)
    except (TypeError, ValueError):
        raise BadRequest("Parallel classes must be a number")
    threads = max(1, min(threads, 4, int(spec.get("maxThreads") or 1)))
    execution = EXECUTIONS.get(payload.get("execution") or DEFAULT_EXECUTION)
    if not execution:
        raise BadRequest("Unknown browser mode")
    grid = None
    if execution == "Remote":
        host = str(payload.get("gridHost") or (payload.get("grid") or {}).get("host") or "").strip()
        port = str(payload.get("gridPort") or (payload.get("grid") or {}).get("port") or "4444").strip()
        if not re.match(r"^[A-Za-z0-9.\-]{1,253}$", host) or not port.isdigit():
            raise BadRequest("Selenium Grid host/port are invalid")
        grid = {"host": host, "port": port}
    browser = payload.get("browser") or "Chrome"
    if browser not in BROWSERS:
        raise BadRequest("Unknown browser")
    pace = payload.get("pace") or "demo"
    if pace not in PACES:
        raise BadRequest("Unknown pace")
    extra = parse_extra_properties(payload.get("extraProperties") or "")
    gate_open = any(SUBMIT_GATE.search(k) and v.strip().lower() == "true" for k, v in extra.items())
    after = [str(a) for a in payload.get("after") or [] if RUN_ID.match(str(a))]
    request = {
        "system": system, "env": env, "urls": urls, "scope": scope, "classes": classes, "modules": modules,
        "tests": tests, "group": group, "threads": threads, "execution": execution, "browser": browser, "pace": pace,
        "grid": grid, "preflight": _bool(payload.get("preflight"), True),
        "retry": _bool(payload.get("retry"), True) and not gate_open,
        "metrics": _bool(payload.get("metrics"), True),
        "fullRun": _bool(payload.get("fullRun"), scope == "full"),
        "extraProperties": extra, "parallelStart": _bool(payload.get("parallelStart"), False), "after": after,
    }
    for key in ("schedule", "batch", "batchParallel", "origin", "envRetry", "preset"):
        if payload.get(key) is not None:
            request[key] = payload[key]
    secrets_found = {}
    if keep_secrets:
        logins = payload.get("logins") or {}
        for login in spec.get("logins", []):
            value = str(logins.get(login["env"], "") or "")
            if value:
                if len(value) > 500 or any(c in value for c in "\r\n\x00"):
                    raise BadRequest(f"{login['label']} has an invalid value")
                secrets_found[login["env"]] = value
    return request, secrets_found


def schedule_request(payload):
    """Validator used by schedules: same rules, never keeps logins."""
    request, _ = clean_request(payload, keep_secrets=False)
    return request


def wanted_blocks(run):
    """[(block name, [fully qualified class])] for a run's scope."""
    system = run["system"]
    cat = catalog()[system]
    full_of = {c["name"]: c["class"] for c in cat["classes"]}
    order = {c["name"]: i for i, c in enumerate(cat["classes"])}

    def fq(names):
        return [full_of[n] for n in sorted(dict.fromkeys(names), key=lambda n: order.get(n, 10_000)) if n in full_of]

    scope = run["scope"]
    if scope == "full":
        blocks = [(name, classes) for name, classes in plan(system, discovered()[system])]
    elif scope == "e2e-only":
        blocks = [("E2E", fq(cat["e2e"]))]
    elif scope == "e2e":
        regression = [n for n in dict.fromkeys(cat["regression"] + history_failures(system)) if n not in cat["e2e"]]
        blocks = [("E2E", fq(cat["e2e"])), ("Regression", fq(regression))]
    elif scope == "classes":
        blocks = [("Selected classes", fq(run["classes"]))]
    elif scope == "modules":
        rows = {m["name"]: m for m in module_rows(system)}
        blocks = [("Module " + name, fq(rows[name]["classes"])) for name in run["modules"] if name in rows]
    elif scope == "retest":
        failing = {}
        for item in open_defects(system):
            failing.setdefault(item["fullName"].rsplit(".", 2)[-2], []).append(item["fullName"].rsplit(".", 1)[-1])
        blocks = [("Retest", fq(list(failing)))]
    else:
        blocks = [("Retest", fq(list(run["tests"])))]
    if run.get("group"):
        group = run["group"]
        with_group = {c["class"] for c in cat["classes"]
                      if any(group in m["groups"] and m["enabled"] for m in c["methods"])}
        blocks = [(name, [c for c in classes if c in with_group]) for name, classes in blocks]
    return [(name, classes) for name, classes in blocks if classes]


def methods_for(run):
    """{fqcn: [methods]} when the run selects methods (retest scopes), dependencies included."""
    if run["scope"] in ("retest-tests", "retest-one"):
        found = discovered()
        cat = catalog()[run["system"]]
        full_of = {c["name"]: c["class"] for c in cat["classes"]}
        return {full_of[name]: with_dependencies(found, full_of[name], methods)
                for name, methods in run["tests"].items() if name in full_of}
    return {}


def estimate(run):
    cat = {c["class"]: c for c in catalog()[run["system"]]["classes"]}
    methods = methods_for(run)
    total = 0
    for _, classes in wanted_blocks(run):
        for fqcn in classes:
            cls = cat.get(fqcn)
            if not cls:
                continue
            for method in cls["methods"]:
                if not method["enabled"]:
                    continue
                if fqcn in methods and method["name"] not in methods[fqcn]:
                    continue
                if run.get("group") and run["group"] not in method["groups"]:
                    continue
                total += method.get("invocations", 1)
    return total


def history_failures(system, limit=10):
    """Simple names of classes that failed in the system's recent finished metric runs."""
    with lock:
        runs = sorted((r for r in RUNS.values() if r["system"] == system and r.get("status") in COMPLETED
                       and r.get("metrics", True)), key=lambda r: r["created"], reverse=True)[:limit]
    names = set()
    for run in runs:
        for test in (run.get("summary") or {}).get("tests", []):
            if test.get("status") in FAILED and test.get("fullName"):
                names.add(test["fullName"].rsplit(".", 2)[-2])
    return sorted(names)


def new_run_id():
    return datetime.now().strftime("%Y%m%d-%H%M%S-") + secrets.token_hex(2)


def create_run(payload):
    request, secret_values = clean_request(payload)
    spec = config.spec(request["system"])
    blocks = wanted_blocks(request)
    if not blocks:
        raise BadRequest("The selected scope contains no test classes" +
                         (" (no open defect to retest)" if request["scope"] == "retest" else ""))
    run_id = new_run_id()
    run = {
        "id": run_id, "title": SYSTEMS[request["system"]]["title"], "envName": request["env"].capitalize(),
        **request, "source": {"type": "local"}, "estimated": 0, "status": "queued", "created": now_iso(),
        "started": None, "finished": None, "exitCode": None, "snapshot": None, "commandLine": None,
        "progress": {"passed": 0, "failed": 0, "skipped": 0, "done": 0, "retried": 0, "current": None},
        "summary": None, "preflightResult": None, "error": None, "report": None, "message": "Queued",
        "credentialOverrides": sorted(secret_values), "blocks": [{"name": n, "classes": c} for n, c in blocks],
        "envRetry": request.get("envRetry", 0) or 0,
    }
    run["estimated"] = estimate(run)
    run["batchParallel"] = int(run.get("batchParallel") or 0) or None
    if run["fullRun"] and run["scope"] == "full" and not spec:
        run["fullRun"] = False
    with lock:
        RUNS[run_id] = run
        TRANSIENT[run_id] = {"secrets": secret_values}
        persist(run)
    wake.set()
    return run


def persist(run):
    try:
        STORE.save_run(run)
    except Exception as error:  # results must never be lost silently
        log("store error while saving run", run.get("id"), error)


def server_of(run):
    first = next(iter((run.get("urls") or {}).values()), "")
    return (urlparse(first).hostname or "").lower()


def can_start(run, active):
    if run.get("notBefore") and parse_time(run["notBefore"]) and parse_time(run["notBefore"]) > now():
        return False
    if len(active) >= MAX_PARALLEL:
        return False
    if not run.get("parallelStart"):
        for other in active:
            if other.get("parallelStart"):
                continue
            if other["system"] == run["system"] or (server_of(other) and server_of(other) == server_of(run)):
                return False
    if run.get("batch") and run.get("batchParallel"):
        if sum(1 for other in active if other.get("batch") == run["batch"]) >= run["batchParallel"]:
            return False
    for reference in run.get("after") or []:
        other = RUNS.get(reference)
        if other and other.get("status") not in TERMINAL:
            return False
    return True


def scheduler():
    while True:
        wake.wait(2)
        wake.clear()
        try:
            schedule_once()
        except Exception as error:
            log("scheduler error:", error)


def schedule_once():
    started = []
    with lock:
        queued = sorted((r for r in RUNS.values() if r["status"] == "queued"), key=lambda r: r["created"])
        active = [r for r in RUNS.values() if r["status"] in ACTIVE]
        for run in queued:
            if can_start(run, active):
                run.update(status="building", message="Preparing code snapshot", notBefore=None)
                persist(run)
                active.append(run)
                started.append(run)
    for run in started:
        threading.Thread(target=execute, args=(run,), daemon=True, name="run-" + run["id"]).start()


# ============================================================================================= execution
def check_grid(host, port):
    url = f"http://{host}:{port}/status"
    check = {"label": "Selenium Grid", "url": url}
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "taf-runner-preflight"}),
                                    timeout=10) as response:
            value = (json.loads(response.read().decode("utf-8", errors="replace")) or {}).get("value") or {}
        nodes = len(value.get("nodes") or [])
        if value.get("ready") is False:
            check.update(ok=False, status=200, detail=f"Grid answered but is not ready ({nodes} node(s) registered)")
        else:
            check.update(ok=True, status=200, detail=f"{nodes} node(s) registered")
    except Exception as error:
        check.update(ok=False, status=None, detail=f"{type(error).__name__}: {error}. Start a Grid first, or use "
                                                   "Headless/Visible mode.")
    return check


def check_url(label, url, timeout=20):
    check = {"label": label, "url": url}
    started = time.monotonic()
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "taf-runner-preflight"})
        with urllib.request.urlopen(request, timeout=timeout) as response:
            check.update(ok=response.status < 500, status=response.status)
    except urllib.error.HTTPError as error:
        check.update(ok=error.code < 500, status=error.code)
    except Exception as error:
        check.update(ok=False, status=None, detail=f"{type(error).__name__}: {error}")
    check["ms"] = int((time.monotonic() - started) * 1000)
    return check


def preflight(run):
    """URLs must answer (no 5xx / timeout); Grid must be ready. Authentication is not verified (no API contract)."""
    checks = [check_url(label, url) for label, url in (run.get("urls") or {}).items()]
    if run.get("execution") == "Remote" and run.get("grid"):
        checks.append(check_grid(run["grid"]["host"], run["grid"]["port"]))
    ok = all(c["ok"] for c in checks)
    lines = []
    url_checks = [c for c in checks if c["label"] != "Selenium Grid"]
    if all(c["ok"] for c in url_checks):
        lines.append(f"OK: the {len(url_checks)} URL(s) answer")
    for c in checks:
        if not c["ok"]:
            lines.append(f"DOWN: {c['label']} {c['url']} - " + (f"HTTP {c['status']}" if c.get("status") else c.get("detail", "")))
        elif c["label"] == "Selenium Grid":
            lines.append(f"OK: Selenium Grid ({c.get('detail')})")
    checks.append({"label": "Authentication", "ok": None,
                   "detail": "Not verified by preflight (no API sign-in contract); UI login is exercised by the tests."})
    lines.append("NOTE: authentication is verified by the tests themselves")
    return {"ok": ok, "lines": lines, "checks": checks}


def suite_file(run, run_dir):
    """The TestNG suite of a run: the generated suites/<system>.xml for a plain full run, otherwise a custom file."""
    threads = int(run.get("threads") or 1)
    if run["scope"] == "full" and threads == 1 and not run.get("group") and (SUITES_DIR / f"{run['system']}.xml").is_file():
        return SUITES_DIR / f"{run['system']}.xml"
    blocks = wanted_blocks(run)
    methods = methods_for(run)
    spec = config.spec(run["system"]) or {}
    name = f"{SYSTEMS[run['system']]['title']} - runner {run['id']}"
    lines = ['<?xml version="1.0" encoding="UTF-8"?>', '<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">']
    if threads > 1:
        lines.append(f'<suite name={quoteattr(name)} verbose="1" parallel="tests" thread-count="{threads}" '
                     'configfailurepolicy="continue">')
        units, grouped = [], {}
        for _, classes in blocks:
            for fqcn in classes:
                group = next((g["name"] for g in spec.get("serialGroups", [])
                              if any(fragment in fqcn for fragment in g["match"])), None)
                if group:
                    if group not in grouped:
                        grouped[group] = []
                        units.append((group, grouped[group]))
                    grouped[group].append(fqcn)
                else:
                    units.append((fqcn.rsplit(".", 1)[-1], [fqcn]))
        blocks = units
    else:
        lines.append(f'<suite name={quoteattr(name)} verbose="1" parallel="false" configfailurepolicy="continue">')
    if run.get("group"):
        lines += ["    <groups>", "        <run>", f'            <include name={quoteattr(run["group"])}/>',
                  "        </run>", "    </groups>"]
    for block, classes in blocks:
        lines.append(f'    <test name={quoteattr(block)} preserve-order="true">')
        lines.append("        <classes>")
        for fqcn in classes:
            if fqcn in methods:
                lines.append(f"            <class name={quoteattr(fqcn)}>")
                lines.append("                <methods>")
                lines += [f"                    <include name={quoteattr(m)}/>" for m in methods[fqcn]]
                lines.append("                </methods>")
                lines.append("            </class>")
            else:
                lines.append(f"            <class name={quoteattr(fqcn)}/>")
        lines.append("        </classes>")
        lines.append("    </test>")
    lines.append("</suite>")
    path = run_dir / "suite.xml"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def jvm_properties(run, output):
    spec = config.spec(run["system"])
    props = {}
    for field in spec["urls"]:
        value = (run.get("urls") or {}).get(field["label"])
        if value:
            for target in field["targets"]:
                props[target] = value
    props["executionType"] = run["execution"]
    props["browserType"] = run.get("browser") or "Chrome"
    props["execution.pace"] = run.get("pace") or "demo"
    if run.get("grid"):
        props["remoteHost"], props["remotePort"] = run["grid"]["host"], run["grid"]["port"]
    threads = int(run.get("threads") or 1)
    if threads > 1:
        props["DEFAULT_WAIT"] = "25"
        props["zakaadash.explicit.wait"] = "25"
        props["parallel.classes"] = str(threads)
    props.update(run.get("extraProperties") or {})
    props.update({
        "active.system": spec["activeSystem"], "output.dir": str(output),
        "allure.results.directory": str(output / "allure-results"), "reports.dir": str(output / "reports"),
        "report.open": "false", "stdout.encoding": "UTF-8", "stderr.encoding": "UTF-8",
    })
    return props


def write_argfile(path, classpath):
    """Java @argfile with the classpath (keeps the command line short on Windows)."""
    path.write_text('-cp "' + classpath.replace("\\", "/") + '"\n', encoding="utf-8")
    return path


def execute(run):
    run_dir = RUNS_DIR / run["id"]
    run_dir.mkdir(parents=True, exist_ok=True)
    try:
        try:
            snapshot = build_snapshot("run " + run["id"])
        except Exception as error:
            finish(run, "failed to start", f"{error}")
            return
        with lock:
            if run["status"] == "stopping":
                finish(run, "stopped", "Stopped before the tests started")
                return
            run.update(snapshot=snapshot, message="Preflight checks" if run.get("preflight") else "Starting")
            persist(run)
        if run.get("preflight"):
            result = preflight(run)
            run["preflightResult"] = result
            if not result["ok"]:
                finish(run, "environment down", "Environment unreachable (preflight): " + "; ".join(
                    l for l in result["lines"] if l.startswith("DOWN")))
                return
        output = run_dir / "output"
        output.mkdir(parents=True, exist_ok=True)
        suite = suite_file(run, run_dir)
        snapshot_dir = BUILD_DIR / snapshot
        classpath_entries = (snapshot_dir / "classpath.txt").read_text(encoding="utf-8").strip().split(os.pathsep)
        weaver = next((e for e in classpath_entries if Path(e).name.startswith("aspectjweaver")), None)
        classpath = os.pathsep.join([str(snapshot_dir / "target" / "test-classes"),
                                     str(snapshot_dir / "target" / "classes")] + classpath_entries)
        argfile = write_argfile(run_dir / "java.args", classpath)
        props = jvm_properties(run, output)
        command = [java_command()]
        if weaver:
            command.append("-javaagent:" + weaver)
        # Same JVM options as the surefire argLine in pom.xml: Selenium's NIO loopback needs a short socket folder
        # (the user TEMP dir fails on this machine: "Unable to establish loopback connection").
        TMP_DIR.mkdir(exist_ok=True)
        command.append("-Djdk.net.unixdomain.tmpdir=" + str(TMP_DIR))
        command += os.environ.get("RUNNER_JVM_ARGS", "").split()
        command += ["@" + str(argfile)] + ["-D" + k + "=" + v for k, v in props.items()]
        command += ["org.testng.TestNG", "-d", str(output / "testng")]
        if run.get("retry"):
            command += ["-listener", "com.taf.core.listeners.TimingRetry"]
        command.append(str(suite))
        environment = dict(os.environ)
        secret_values = (TRANSIENT.get(run["id"]) or {}).get("secrets") or {}
        environment.update(secret_values)
        cli = allure_cli()
        if cli and not os.environ.get("ALLURE_CLI"):
            environment["ALLURE_CLI"] = cli
        run["commandLine"] = masked(" ".join(f'"{c}"' if " " in c else c for c in command),
                                    list(secret_values.values()))
        with lock:
            if run["status"] == "stopping":
                finish(run, "stopped", "Stopped before the tests started")
                return
            run.update(status="running", started=now_iso(), message="Running")
            persist(run)
        with open(run_dir / "console.log", "w", encoding="utf-8", errors="replace") as console:
            proc = subprocess.Popen(command, cwd=ROOT, env=environment, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, **process_flags())
            with lock:
                TRANSIENT.setdefault(run["id"], {})["process"] = proc
            retry_pending = set()
            last_persist = 0.0
            for raw in proc.stdout:
                line = raw.decode("utf-8", errors="replace")
                console.write(masked(line, list(secret_values.values())) if secret_values else line)
                console.flush()
                if track_progress(run, line, retry_pending) and time.monotonic() - last_persist > 3:
                    persist(run)
                    last_persist = time.monotonic()
            code = proc.wait()
            proc.stdout.close()
        with lock:
            (TRANSIENT.get(run["id"]) or {}).pop("process", None)
        run["exitCode"] = code
        run["message"] = "Summarising results"
        summary = summarize(run_dir, is_test_method)
        run["summary"] = summary
        run["report"] = publish_report(run_dir)
        stopping = run["status"] == "stopping"
        if stopping:
            finish(run, "stopped", "Stopped by user")
        elif not summary or not summary["tests"]:
            tail = (run_dir / "console.log").read_text(encoding="utf-8", errors="replace")[-1500:]
            finish(run, "failed to start", f"TestNG produced no results (exit code {code}). Console tail: {tail[-600:]}")
        else:
            counts = summary["counts"]
            if counts["failed"] + counts["broken"] > 0:
                finish(run, "finished with failures", f"{counts['failed'] + counts['broken']} failed"
                       + (f" ({counts['bugChecks']} bug checks)" if counts["bugChecks"] else ""))
            elif counts["passed"] > 0 and not counts["setupFailures"]:
                finish(run, "passed", "All executed tests passed")
            elif summary["setup"]:
                first = summary["setup"][0]
                finish(run, "finished with failures",
                       f"Setup failed ({counts['setupFailures']}x, e.g. {first['name']}: "
                       f"{(first['message'] or first['exception'])[:200]}); {counts['skipped']} test(s) skipped, "
                       f"{counts['passed']} passed")
            else:
                finish(run, "finished with failures", f"No test executed: all {counts['skipped']} were skipped "
                                                      "(check phase gates / setup)")
    except Exception as error:
        finish(run, "error", f"Runner error: {type(error).__name__}: {error}")


def track_progress(run, line, retry_pending):
    """Live progress from [TAF-PROGRESS] / 'Test Case <name> (status)' lines. Returns True when it changed."""
    progress = run["progress"]
    retry = RETRY_LINE.search(line)
    if retry:
        retry_pending.add(retry.group(1) + "#" + retry.group(2))
        progress["retried"] = progress.get("retried", 0) + 1
        return True
    match = PROGRESS_TAF.search(line)
    if match:
        status, class_name, method = match.groups()
        key = class_name + "#" + method
        if status == "START":
            progress["current"] = class_name.rsplit(".", 1)[-1] + "." + method
            return True
        if status == "SKIP" and key in retry_pending:
            retry_pending.discard(key)
            return True
        progress[{"PASS": "passed", "FAIL": "failed", "SKIP": "skipped"}[status]] += 1
        progress["done"] += 1
        return True
    match = PROGRESS_SPEC.search(line)
    if match:
        progress[match.group(2)] += 1
        progress["done"] += 1
        progress["current"] = match.group(1)
        return True
    return False


def _labels(data):
    labels = {}
    for label in data.get("labels") or []:
        labels.setdefault(label.get("name"), label.get("value"))
    return labels


def _images(node, acc):
    for attachment in node.get("attachments") or []:
        source = attachment.get("source") or ""
        if (attachment.get("type") or "").startswith("image/") or source.lower().endswith((".png", ".jpg", ".jpeg")):
            acc.append(attachment)
    for step in node.get("steps") or []:
        _images(step, acc)
    return acc


def pick_shot(data):
    """The final screenshot of a test: last test-level image, else the last step image."""
    top = [a for a in data.get("attachments") or []
           if (a.get("type") or "").startswith("image/") or (a.get("source") or "").lower().endswith(".png")]
    if top:
        return top[-1].get("source")
    images = _images(data, [])
    final = [a for a in images if re.search(r"_(FAIL|PASS|BROKEN|SKIP)$", a.get("name") or "")]
    chosen = (final or images)
    return chosen[-1].get("source") if chosen else None


def is_test_method(class_name, method):
    """True/False when the class is in the catalogue (is `method` one of its @Test methods?), None when unknown."""
    for system in catalog():
        cls = class_entry(system, class_name)
        if cls:
            return any(m["name"] == method for m in cls["methods"])
    return None


def summarize(run_dir, is_test=None):
    """Test results from allure-results: retried attempts share a historyId; the last attempt is the result.
    Results of setup/teardown fixtures (not @Test methods) are reported apart as `setup`, never as tests."""
    results_dir = Path(run_dir) / "output" / "allure-results"
    if not results_dir.is_dir():
        return None
    setup = []
    groups = {}
    for path in results_dir.glob("*-result.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        key = data.get("historyId") or data.get("fullName") or path.name
        groups.setdefault(key, []).append(data)
    tests = []
    for attempts in groups.values():
        attempts.sort(key=lambda d: (d.get("start") or 0, d.get("stop") or 0))
        last, earlier = attempts[-1], attempts[:-1]
        labels = _labels(last)
        status = last.get("status") or "broken"
        if status not in ("passed", "failed", "broken", "skipped"):
            status = "broken"
        details = last.get("statusDetails") or {}
        trace = (details.get("trace") or "").strip()
        exception = trace.splitlines()[0].split(":", 1)[0].strip() if trace else ""
        soft = any((l.get("type") or "") == "issue" for l in last.get("links") or []) or \
            str(labels.get("bugCheck", "")).lower() == "true"
        cls = labels.get("testClass") or ""
        method = labels.get("testMethod") or last.get("name") or ""
        if is_test is not None and cls and is_test(cls, method) is False:
            if status in FAILED:
                setup.append({"name": method, "class": cls, "fullName": last.get("fullName") or method, "status": status,
                              "message": (details.get("message") or "").strip()[:600], "exception": exception[:200]})
            continue
        start, stop = int(last.get("start") or 0), int(last.get("stop") or 0)
        first_message = ((earlier[0].get("statusDetails") or {}).get("message") or "").strip() if earlier else None
        tests.append({
            "name": method, "fullName": last.get("fullName") or (cls + "." + method if cls else method),
            "status": status, "soft": soft, "message": (details.get("message") or "").strip()[:600],
            "duration": round(max(0, stop - start) / 1000, 2), "shot": pick_shot(last), "start": start,
            "case": (last.get("description") or "")[:4000] if status in FAILED else None,
            "retried": len(earlier), "firstAttempt": first_message[:300] if first_message is not None else None,
            "flaky": bool(earlier) and status == "passed",
            "testId": labels.get("testCaseId") or "", "title": labels.get("testTitle") or last.get("name") or method,
            "feature": labels.get("feature") or labels.get("parentSuite") or "",
            "category": labels.get("failureCategory") or "", "exception": exception[:200],
            "uuid": last.get("uuid"),
        })
    tests.sort(key=lambda t: t["start"])
    counts = {"passed": 0, "failed": 0, "broken": 0, "skipped": 0, "bugChecks": 0, "flaky": 0}
    for test in tests:
        counts[test["status"]] += 1
        counts["bugChecks"] += 1 if test["soft"] and test["status"] in FAILED else 0
        counts["flaky"] += 1 if test["flaky"] else 0
    unique_setup = {}
    for item in setup:
        unique_setup.setdefault((item["fullName"], item["message"]), {**item, "times": 0})["times"] += 1
    counts["setupFailures"] = len(setup)
    return {"counts": counts, "total": len(tests), "tests": tests, "setup": list(unique_setup.values())}


def publish_report(run_dir):
    """Copy the run's Allure HTML to runs/<id>/report.html (framework output, else Allure CLI single-file)."""
    run_dir = Path(run_dir)
    output = run_dir / "output"
    candidates = [output / "reports" / "latest" / "allure-html" / "index.html"]
    candidates += sorted((output / "reports").rglob("AllureReport_*.html"), reverse=True) if (output / "reports").is_dir() else []
    source = next((c for c in candidates if c.is_file()), None)
    if source is None:
        cli = allure_cli()
        results = output / "allure-results"
        if cli and results.is_dir() and any(results.glob("*-result.json")):
            target = run_dir / "allure-report"
            try:
                subprocess.run([cli, "generate", str(results), "-o", str(target), "--clean", "--single-file"],
                               cwd=ROOT, capture_output=True, timeout=180)
            except (OSError, subprocess.TimeoutExpired):
                pass
            if (target / "index.html").is_file():
                source = target / "index.html"
    if source is None:
        return None
    shutil.copyfile(source, run_dir / "report.html")
    if source.parent.name == "allure-report":
        shutil.rmtree(source.parent, ignore_errors=True)
    return f"/runs/{run_dir.name}/report.html"


def finish(run, status, message):
    with lock:
        run["status"] = status
        run["message"] = message
        run["error"] = None if status == "passed" else message
        run["finished"] = now_iso()
        transient = TRANSIENT.get(run["id"]) or {}
        transient.pop("process", None)
        retrying = status == "environment down" and (run.get("envRetry") or 0) < reliability.ENV_RETRIES
        if not retrying:
            TRANSIENT.pop(run["id"], None)
        persist(run)
    threading.Thread(target=after_run, args=(run,), daemon=True).start()
    wake.set()


def after_run(run):
    """Upload reports, quarantine flaky tests, retry env-down runs, notify scheduled runs."""
    try:
        run_dir = RUNS_DIR / run["id"]
        run["reportSize"] = reports.local_size(run_dir)
        if reports.enabled() and run["status"] not in ("queued", "environment down"):
            try:
                result = reports.upload(STORE, run["id"], run_dir)
                if result["files"]:
                    run["reportsInSpaces"] = now_iso()
                    run["reportSize"] = result["size"]
            except Exception as error:
                run["spacesError"] = f"{type(error).__name__}: {error}"[:300]
        if run["status"] in COMPLETED and run.get("metrics", True):
            with lock:
                runs = list(RUNS.values())
            changes = reliability.auto_quarantine(runs, run["system"])
            if changes:
                run["quarantineChanges"] = changes
        env_retry_due(run)
        if run.get("schedule") and run["status"] != "queued":
            note = schedules.notify(run, SYSTEMS[run["system"]]["title"])
            if note:
                run["notified"] = {"email": note}
        persist(run)
    except Exception as error:
        log("after-run error", run.get("id"), error)
    finally:
        _storage_cache["at"] = 0.0
        prune_snapshots()


def env_retry_due(run):
    """An 'environment down' run is queued again after ENV_RETRY_MINUTES, up to ENV_RETRIES times."""
    if run["status"] != "environment down" or (run.get("envRetry") or 0) >= reliability.ENV_RETRIES:
        return False
    with lock:
        attempt = (run.get("envRetry") or 0) + 1
        due = now() + timedelta(minutes=reliability.ENV_RETRY_MINUTES)
        history = run.setdefault("envRetryHistory", [])
        history.append({"at": run.get("finished"), "reason": run.get("error")})
        run.update(status="queued", envRetry=attempt, notBefore=due.isoformat(timespec="seconds"), finished=None,
                   message=f"Environment down - retry {attempt}/{reliability.ENV_RETRIES} at {due:%H:%M}")
        persist(run)
    wake.set()
    return True


def stop_run(run_id):
    with lock:
        run = RUNS.get(run_id)
        if run is None:
            raise NotFound("Unknown run")
        if run["status"] == "queued":
            run.update(status="cancelled", message="Removed from the queue", finished=now_iso(),
                       error="Cancelled before it started")
            TRANSIENT.pop(run_id, None)
            persist(run)
            return run
        if run["status"] not in ("building", "running"):
            raise BadRequest("Run is not active")
        run["status"] = "stopping"
        run["message"] = "Stopping"
        proc = (TRANSIENT.get(run_id) or {}).get("process")
        persist(run)
    kill_tree(proc)
    return run


def rerun_payload(run, only_failed=False):
    payload = {k: run.get(k) for k in ("system", "env", "urls", "scope", "classes", "modules", "tests", "group",
                                        "threads", "execution", "browser", "pace", "grid", "preflight", "retry",
                                        "metrics", "fullRun", "extraProperties", "parallelStart")}
    payload["origin"] = {"type": "rerun", "id": run["id"]}
    if only_failed:
        failed = {}
        for test in (run.get("summary") or {}).get("tests", []):
            if test["status"] in FAILED and test.get("fullName"):
                cls, method = test["fullName"].rsplit(".", 1)
                failed.setdefault(cls.rsplit(".", 1)[-1], []).append(method)
        if not failed:
            return None
        payload.update(scope="retest-tests", tests=failed, classes=[], modules=[], fullRun=False, group=None)
    return payload


def rerun(run_id, only_failed=False):
    run = RUNS.get(run_id)
    if not run:
        raise NotFound("Unknown run")
    payload = rerun_payload(run, only_failed)
    if payload is None:
        raise BadRequest("The run has no failed test")
    return create_run(payload)


# ============================================================================================= history
LEGACY_STATUS = {"failed": "finished with failures", "skipped": "finished with failures"}
LEGACY_SCOPE = {"whole": "full", "e2e_regression": "e2e", "e2e": "e2e-only", "classes": "classes"}


def migrate(run):
    """A run written by the 2026-10 runner, mapped to the current model (results re-read from its Allure files)."""
    run_dir = RUNS_DIR / run["id"]
    run["scope"] = LEGACY_SCOPE.get(run.get("scope"), run.get("scope") or "full")
    run["status"] = LEGACY_STATUS.get(run.get("status"), run.get("status"))
    run["execution"] = EXECUTIONS.get(run.get("execution") or "headless", "LocalHeadless")
    if isinstance(run.get("urls"), list):
        run["urls"] = {u["label"]: u["url"] for u in run["urls"]}
    if run["scope"] == "classes":
        run["classes"] = [c.rsplit(".", 1)[-1] for b in run.get("blocks") or [] for c in b["classes"]]
    run.setdefault("classes", [])
    run.setdefault("modules", [])
    run.setdefault("tests", {})
    run.setdefault("group", None)
    run.setdefault("threads", 1)
    run["envName"] = run.get("envName") or (run.get("env") or "").capitalize()
    old_preflight = run.get("preflight")
    if isinstance(old_preflight, list):
        run["preflightResult"] = {"ok": all(c.get("ok") is not False for c in old_preflight), "lines": [],
                                  "checks": old_preflight}
    run["preflight"] = True
    run.setdefault("retry", False)
    run.setdefault("metrics", True)
    run.setdefault("fullRun", run["scope"] == "full")
    run["estimated"] = run.get("estimated") or (run.get("progress") or {}).get("expected") or 0
    run["summary"] = summarize(run_dir, is_test_method) if (run_dir / "output" / "allure-results").is_dir() else None
    if run["summary"] is None and run.get("results"):
        tests = [{"name": t["method"], "fullName": t["class"] + "." + t["method"],
                  "status": {"PASS": "passed", "FAIL": "failed"}.get(t["status"], "skipped"), "soft": False,
                  "message": "", "duration": round((t.get("durationMs") or 0) / 1000, 2), "shot": None, "start": 0,
                  "retried": 0, "flaky": False, "testId": "", "title": t.get("description") or t["method"]}
                 for t in run["results"].get("tests", [])]
        counts = {s: sum(1 for t in tests if t["status"] == s) for s in ("passed", "failed", "broken", "skipped")}
        run["summary"] = {"counts": {**counts, "bugChecks": 0, "flaky": 0}, "total": len(tests), "tests": tests}
    if run.get("allureReport") and (run_dir / run["allureReport"]).is_file():
        run["report"] = f"/runs/{run['id']}/{run['allureReport']}"
    run.setdefault("report", None)
    if run.get("status") != "passed":
        run.setdefault("error", run.get("message"))
    run.setdefault("source", {"type": "local"})
    for old in ("results", "jvmProperties", "allureReport", "blocks_legacy", "command"):
        run.pop(old, None)
    run["model"] = 2
    return run


def load_history():
    RUNS_DIR.mkdir(parents=True, exist_ok=True)
    pending = []
    for run in STORE.load_runs():
        if not run.get("id"):
            continue
        changed = False
        if run.get("model") != 2:
            migrate(run)
            changed = True
        if run.get("status") not in TERMINAL:
            run.update(status="interrupted", finished=run.get("finished") or now_iso(),
                       error="The runner stopped while this run was queued or running",
                       message="Interrupted (runner restarted)")
            changed = True
        if changed:
            persist(run)
        RUNS[run["id"]] = run
        if reports.enabled() and not run.get("reportsInSpaces") and run["status"] in COMPLETED \
                and reports.report_files(RUNS_DIR / run["id"]):
            pending.append(run)

    def upload_pending():
        for run in pending:
            try:
                result = reports.upload(STORE, run["id"], RUNS_DIR / run["id"])
                if result["files"]:
                    run.update(reportsInSpaces=now_iso(), reportSize=result["size"])
                    persist(run)
            except Exception as error:
                log("Spaces upload failed for", run["id"], error)
                break
    if pending:
        threading.Thread(target=upload_pending, daemon=True).start()


# ============================================================================================= storage
def delete_reports(run):
    run_dir = RUNS_DIR / run["id"]
    if run_dir.is_dir():
        for child in run_dir.iterdir():
            if child.name in reports.KEEP_LOCAL:
                continue
            if child.is_dir():
                shutil.rmtree(child, ignore_errors=True)
            else:
                try:
                    child.unlink()
                except OSError:
                    pass
    try:
        reports.delete(STORE, run["id"])
    except Exception as error:
        log("Spaces delete failed for", run["id"], error)
    run.update(report=None, reportsDeleted=now_iso(), reportSize=0, reportsInSpaces=None)
    persist(run)


def delete_run_completely(run):
    try:
        reports.delete(STORE, run["id"])
    except Exception as error:
        log("Spaces delete failed for", run["id"], error)
    shutil.rmtree(RUNS_DIR / run["id"], ignore_errors=True)
    STORE.delete_run(run["id"])
    with lock:
        RUNS.pop(run["id"], None)


def delete_runs(ids, mode="runs"):
    deleted, skipped = [], []
    for run_id in ids or []:
        run = RUNS.get(str(run_id))
        if run is None or run["status"] not in TERMINAL:
            skipped.append(run_id)
            continue
        if mode == "reports":
            delete_reports(run)
        else:
            delete_run_completely(run)
        deleted.append(run_id)
    _storage_cache["at"] = 0.0
    return {"deleted": deleted, "skipped": skipped, "mode": mode}


def storage(force=False):
    if not force and _storage_cache["value"] and time.monotonic() - _storage_cache["at"] < 60:
        return _storage_cache["value"]
    with lock:
        runs = list(RUNS.values())
    systems = {}
    for run in runs:
        row = systems.setdefault(run["system"], {"system": run["system"], "title": SYSTEMS.get(run["system"], {}).get("title", run["system"]),
                                                 "runs": 0, "withReports": 0, "localBytes": 0, "spacesBytes": 0})
        row["runs"] += 1
        local = reports.local_size(RUNS_DIR / run["id"])
        remote = reports.remote_size(STORE, run["id"]) if run.get("reportsInSpaces") else 0
        row["localBytes"] += local
        row["spacesBytes"] += remote
        row["withReports"] += 1 if (run.get("report") or remote or local > 20_000) else 0
    usage = shutil.disk_usage(RUNNER_DIR)
    snapshots = sum(reports.local_size(p) for p in BUILD_DIR.iterdir()) if BUILD_DIR.is_dir() else 0
    value = {"systems": sorted(systems.values(), key=lambda r: r["system"]),
             "disk": {"total": usage.total, "used": usage.used, "free": usage.free},
             "snapshotsBytes": snapshots, "spaces": reports.status(), "store": STORE.describe()}
    _storage_cache.update(at=time.monotonic(), value=value)
    return value


def clean_storage(systems_wanted, keep, mode):
    if mode not in ("reports", "runs"):
        raise BadRequest("Mode must be reports or runs")
    try:
        keep = max(0, int(keep))
    except (TypeError, ValueError):
        raise BadRequest("Keep must be a number")
    systems_wanted = [s for s in systems_wanted or SYSTEMS if s in SYSTEMS]
    with lock:
        candidates = []
        for system in systems_wanted:
            finished = sorted((r for r in RUNS.values() if r["system"] == system and r["status"] in TERMINAL),
                              key=lambda r: r["created"], reverse=True)
            candidates += finished[keep:]
    if mode == "reports":
        candidates = [r for r in candidates if not r.get("reportsDeleted")]
    before = storage(force=True)
    result = delete_runs([r["id"] for r in candidates], mode)
    after = storage(force=True)
    freed = sum(s["localBytes"] + s["spacesBytes"] for s in before["systems"]) - \
        sum(s["localBytes"] + s["spacesBytes"] for s in after["systems"])
    return {**result, "freedBytes": max(0, freed)}


# ============================================================================================= defect registry
DEFECTS = {}   # system -> signature -> entry (mirror of the store)


def load_defects():
    with registry_lock:
        DEFECTS.clear()
        DEFECTS.update(STORE.load_defects())


def defect_of(system, test):
    signature = bugreport.signature(test)
    with registry_lock:
        return signature, dict((DEFECTS.get(system) or {}).get(signature) or {}) or None


def next_bug_id(system):
    prefix = (config.spec(system) or {}).get("bugPrefix") or system[:3].upper()
    numbers = [int(m.group(1)) for e in (DEFECTS.get(system) or {}).values()
               for m in [re.match(re.escape(prefix) + r"-(\d+)$", e.get("id") or "")] if m]
    return f"{prefix}-{(max(numbers) + 1) if numbers else 1:02d}"


def save_defect(system, signature, entry):
    with registry_lock:
        DEFECTS.setdefault(system, {})[signature] = entry
        STORE.save_defect(system, signature, entry)


def first_failure(system, signature):
    """Date (YYYY-MM-DD) the defect was first seen in a metric run."""
    with lock:
        runs = sorted((r for r in RUNS.values() if r["system"] == system and r.get("summary")),
                      key=lambda r: r.get("started") or r["created"])
    for run in runs:
        for test in run["summary"].get("tests", []):
            if test.get("status") in FAILED and bugreport.signature(test) == signature:
                return (run.get("started") or run["created"])[:10]
    return now_iso()[:10]


def find_test(run_id, full_name):
    run = RUNS.get(run_id)
    if not run:
        raise NotFound("Unknown run")
    test = next((t for t in (run.get("summary") or {}).get("tests", []) if t.get("fullName") == full_name), None)
    if not test:
        raise NotFound("The run has no such test")
    return run, test


def ensure_defect(run, test, severity=None, note=None):
    signature, entry = defect_of(run["system"], test)
    if entry is None:
        entry = {"id": next_bug_id(run["system"]), "first": first_failure(run["system"], signature), "reported": [],
                 "title": test.get("title") or test.get("name"), "test": test.get("fullName"), "severity": None,
                 "status": "New", "note": "", "notBug": False}
    elif not entry.get("id"):
        entry["id"] = next_bug_id(run["system"])
    if severity is not None:
        entry["severity"] = severity if severity in bugreport.SEVERITIES else None
    if note is not None:
        entry["note"] = str(note).strip()[:500]
    return signature, entry


def rate_defect(payload):
    run, test = find_test(str(payload.get("run") or ""), str(payload.get("test") or ""))
    severity = payload.get("severity")
    if severity not in (None, "", *bugreport.SEVERITIES):
        raise BadRequest("Severity must be Critical, High, Medium or Low")
    with registry_lock:
        signature, entry = ensure_defect(run, test, None, payload.get("note"))
        if "severity" in payload:
            entry["severity"] = severity or None  # null / "" = back to Unrated
        if "notBug" in payload:
            entry["notBug"] = bool(payload.get("notBug"))
        entry["updated"] = now_iso()
        save_defect(run["system"], signature, entry)
    return entry


# ============================================================================================= quality
def metric_runs(system=None, days=None):
    since = now() - timedelta(days=days) if days else None
    with lock:
        runs = [r for r in RUNS.values() if (system is None or r["system"] == system) and r["status"] in COMPLETED
                and r.get("metrics", True) is not False and r.get("summary")]
    if since:
        runs = [r for r in runs if (parse_time(r.get("started") or r["created"]) or now()) >= since]
    return sorted(runs, key=lambda r: r.get("started") or r["created"])


def latest_results(system, days=None):
    """{fullName: (test, run)} - the latest executed result of every test in the period."""
    latest = {}
    for run in metric_runs(system, days):
        for test in run["summary"].get("tests", []):
            if test.get("status") in EXECUTED and test.get("fullName"):
                latest[test["fullName"]] = (test, run)
    return {k: v for k, v in latest.items() if test_exists(k)}


def defect_status(system, full_name, signature, failing_run):
    """Reopened: the same defect was seen on an earlier day, then the test passed, and now it fails again."""
    first_day, passed_after = None, False
    failing_day = (failing_run.get("started") or failing_run["created"])[:10]
    for run in metric_runs(system):
        if run is failing_run:
            break
        test = next((t for t in run["summary"].get("tests", []) if t.get("fullName") == full_name), None)
        if not test:
            continue
        if test["status"] in FAILED and bugreport.signature(test) == signature and first_day is None:
            first_day = (run.get("started") or run["created"])[:10]
        elif test["status"] == "passed" and first_day is not None:
            passed_after = True
    return "Reopened" if first_day and passed_after and first_day < failing_day else "New"


def open_defects(system, days=None):
    rows = []
    for full_name, (test, run) in latest_results(system, days).items():
        if test["status"] not in FAILED:
            continue
        signature, entry = defect_of(system, test)
        entry = entry or {}
        if entry.get("notBug") or reliability.is_quarantined(system, full_name):
            continue
        rows.append({
            "fullName": full_name, "name": test.get("name"), "testId": test.get("testId"), "title": test.get("title"),
            "feature": test.get("feature"), "message": test.get("message"), "soft": test.get("soft"),
            "kind": reliability.classify(test.get("message"), test.get("soft"), test.get("category", ""),
                                         test.get("exception", "")),
            "category": test.get("category"), "run": run["id"], "env": run.get("env"), "created": run["created"],
            "shot": test.get("shot"), "signature": signature, "id": entry.get("id"), "severity": entry.get("severity"),
            "note": entry.get("note"), "first": entry.get("first"), "reported": entry.get("reported") or [],
            "status": defect_status(system, full_name, signature, run)})
    rows.sort(key=lambda r: (SEVERITY_RANK.get(r["severity"], 4), r["fullName"]))
    return rows


def fixed_defects(system, days):
    """Tests whose latest result passed after failing earlier in the period."""
    rows = []
    runs = metric_runs(system, days)
    for full_name, (test, run) in latest_results(system, days).items():
        if test["status"] != "passed":
            continue
        failed_in = None
        for earlier in runs:
            if earlier is run:
                break
            prior = next((t for t in earlier["summary"]["tests"] if t.get("fullName") == full_name), None)
            if prior and prior["status"] in FAILED:
                failed_in = (earlier, prior)
        if failed_in:
            _, entry = defect_of(system, failed_in[1])
            rows.append({"fullName": full_name, "name": test.get("name"), "testId": test.get("testId"),
                         "title": test.get("title"), "fixedIn": run["id"], "fixedAt": run.get("finished"),
                         "failedIn": failed_in[0]["id"], "id": (entry or {}).get("id"),
                         "severity": (entry or {}).get("severity")})
    return rows


def quality(system, days=30):
    latest = latest_results(system, days)
    counts = {"passed": 0, "failed": 0, "broken": 0}
    flaky = 0
    for test, _ in latest.values():
        counts[test["status"]] += 1
        flaky += 1 if test.get("flaky") else 0
    executed = sum(counts.values())
    defects = open_defects(system, days)
    severity = {"Critical": 0, "High": 0, "Medium": 0, "Low": 0, "Unrated": 0}
    for defect in defects:
        severity[defect["severity"] or "Unrated"] += 1
    reopened = sum(1 for d in defects if d["status"] == "Reopened")
    return {"system": system, "days": days, "tests": executed, **counts,
            "passRate": round(100 * counts["passed"] / executed, 1) if executed else None,
            "open": len(defects), "bugChecks": sum(1 for d in defects if d["soft"]), "severity": severity,
            "reopened": reopened, "reopenRate": round(100 * reopened / len(defects), 1) if defects else 0.0,
            "flaky": flaky, "flakyRate": round(100 * flaky / executed, 1) if executed else 0.0,
            "quarantined": sum(1 for v in reliability.quarantine(system).values() if v.get("on")),
            "runs": len(metric_runs(system, days)), "defects": defects}


def health(q):
    """Release status: red (a Critical), yellow (other open defects or pass rate < 95 %), green otherwise."""
    if not q["tests"]:
        return {"color": "gray", "emoji": "⚪", "label": "لا توجد نتائج", "headline": "No executed test in the period",
                "passRate": None, "open": 0, "critical": 0}
    critical = q["severity"]["Critical"]
    top = q["defects"][0] if q["defects"] else None
    headline = (top.get("note") or top.get("title") or top.get("name")) if top else "All checks passing"
    if critical:
        color, emoji, label = "red", "🔴", "فيه Critical"
    elif q["open"] or (q["passRate"] is not None and q["passRate"] < 95):
        color, emoji, label = "yellow", "🟡", "فيه مخاطر"
    else:
        color, emoji, label = "green", "🟢", "جاهز للإصدار"
    return {"color": color, "emoji": emoji, "label": label, "headline": headline, "passRate": q["passRate"],
            "open": q["open"], "critical": critical}


def is_complete_full_run(run):
    if run.get("status") not in COMPLETED:
        return False
    tests = (run.get("summary") or {}).get("tests") or []
    if not any(t.get("status") in EXECUTED for t in tests):
        return False
    if "fullRun" in run:
        return bool(run["fullRun"])
    return run.get("scope") == "full"


def last_full_run(system):
    with lock:
        runs = sorted((r for r in RUNS.values() if r["system"] == system and is_complete_full_run(r)),
                      key=lambda r: r.get("started") or r["created"], reverse=True)
    if not runs:
        return None
    run = runs[0]
    return {**run_summary(run), "age": human_age(run.get("finished"))}


def human_age(value):
    moment = parse_time(value)
    if not moment:
        return ""
    seconds = (now() - moment).total_seconds()
    if seconds < 3600:
        return f"{int(seconds // 60)} min ago"
    if seconds < 86400:
        return f"{int(seconds // 3600)} h ago"
    return f"{int(seconds // 86400)} days ago"


def module_overlay(system, days):
    latest = latest_results(system, days)
    rows = []
    for module in module_rows(system):
        passed = failed = 0
        tests = []
        for full_name, (test, _) in latest.items():
            if full_name.rsplit(".", 2)[-2] in module["classes"]:
                tests.append({"fullName": full_name, "name": test["name"], "status": test["status"],
                              "testId": test.get("testId")})
                if test["status"] == "passed":
                    passed += 1
                else:
                    failed += 1
        rows.append({**module, "executed": passed + failed, "passed": passed, "failed": failed,
                     "results": sorted(tests, key=lambda t: (t["status"] == "passed", t["fullName"]))})
    return rows


def read_uptime(days):
    since = (now() - timedelta(days=days)).isoformat(timespec="seconds")
    rows = []
    if UPTIME_FILE.is_file():
        with uptime_lock:
            lines = UPTIME_FILE.read_text(encoding="utf-8", errors="replace").splitlines()
        for line in lines:
            try:
                row = json.loads(line)
            except ValueError:
                continue
            if row.get("t", "") >= since:
                rows.append(row)
    return rows


def availability(system, days=30, rows=None):
    rows = [r for r in (rows if rows is not None else read_uptime(days)) if r.get("system") == system]
    up_seconds = sum(UPTIME_EVERY for r in rows if r.get("up"))
    down_seconds = sum(UPTIME_EVERY for r in rows if not r.get("up"))
    for run in metric_runs(system, days):
        start, end = parse_time(run.get("started")), parse_time(run.get("finished"))
        if start and end:
            up_seconds += max(0, (end - start).total_seconds())
    outages, current = [], None
    for row in rows:
        if not row.get("up"):
            if current is None:
                current = {"from": row["t"], "to": row["t"], "checks": 0, "detail": row.get("detail", "")}
            current["to"], current["checks"] = row["t"], current["checks"] + 1
        elif current is not None:
            outages.append(current)
            current = None
    if current is not None:
        outages.append({**current, "ongoing": True})
    for outage in outages:
        outage["minutes"] = round(outage["checks"] * UPTIME_EVERY / 60)
    total = up_seconds + down_seconds
    return {"percent": round(100 * up_seconds / total, 2) if total else None, "hoursDown": round(down_seconds / 3600, 1),
            "checks": len(rows), "outages": outages[-5:][::-1], "monitoring": UPTIME_ENABLED,
            "lastCheck": rows[-1] if rows else None}


def uptime_check_once():
    for system, spec in config.systems().items():
        env = next(iter(spec.get("envs") or {}), None)
        urls = [(label, url) for label, url in (spec["envs"].get(env) or {}).items() if url] if env else []
        if not urls:
            continue
        checks = [check_url(label, url, timeout=20) for label, url in urls]
        row = {"t": now_iso(), "system": system, "env": env, "up": all(c["ok"] for c in checks),
               "ms": max(c["ms"] for c in checks),
               "detail": "; ".join(f"{c['label']}: {c.get('status') or c.get('detail', '')}" for c in checks if not c["ok"])[:300]}
        with uptime_lock:
            with open(UPTIME_FILE, "a", encoding="utf-8") as handle:
                handle.write(json.dumps(row, ensure_ascii=False) + "\n")


def prune_uptime(days=90):
    if not UPTIME_FILE.is_file():
        return
    keep = read_uptime(days)
    with uptime_lock:
        tmp = UPTIME_FILE.with_name("uptime.jsonl.tmp")
        tmp.write_text("".join(json.dumps(r, ensure_ascii=False) + "\n" for r in keep), encoding="utf-8")
        os.replace(tmp, UPTIME_FILE)


def uptime_monitor():
    checks = 0
    while True:
        try:
            uptime_check_once()
            checks += 1
            if checks % 288 == 0:
                prune_uptime()
        except Exception as error:
            log("uptime monitor error:", error)
        time.sleep(UPTIME_EVERY)


def seconds_per_test(system):
    """Median of duration / tests over the system's last 5 completed runs."""
    with lock:
        runs = sorted((r for r in RUNS.values() if r["system"] == system and r["status"] in COMPLETED
                       and (r.get("summary") or {}).get("total")), key=lambda r: r["created"], reverse=True)[:5]
    paces = []
    for run in runs:
        start, end = parse_time(run.get("started")), parse_time(run.get("finished"))
        if start and end and run["summary"]["total"]:
            paces.append((end - start).total_seconds() / run["summary"]["total"])
    return round(statistics.median(paces), 1) if paces else None


def timing(run):
    start, end = parse_time(run.get("started")), parse_time(run.get("finished"))
    elapsed = ((end or now()) - start).total_seconds() if start else None
    pace = seconds_per_test(run["system"])
    estimated = run.get("estimated") or 0
    expected = round(estimated * pace) if pace and estimated else None
    remaining = None
    if run["status"] == "running" and elapsed is not None and estimated:
        done = (run.get("progress") or {}).get("done") or 0
        own = elapsed / done if done else None
        weight = min(1.0, done / estimated) if estimated else 0
        blended = (weight * own + (1 - weight) * pace) if own and pace else (own or pace)
        remaining = round(max(0, estimated - done) * blended) if blended else None
    return {"elapsed": round(elapsed) if elapsed is not None else None, "expected": expected, "remaining": remaining}


def dashboard(system=None, days=30):
    systems_wanted = [system] if system else [k for k in config.systems() if k in SYSTEMS]
    cards, trend, slow, failing = [], [], {}, {}
    defects, fixed, modules = [], [], {}
    uptime_rows = read_uptime(days)
    for key in systems_wanted:
        q = quality(key, days)
        card = {"key": key, "title": SYSTEMS[key]["title"], "health": health(q),
                "quality": {k: v for k, v in q.items() if k != "defects"}, "lastFullRun": last_full_run(key),
                "availability": availability(key, days, uptime_rows)}
        cards.append(card)
        defects += [{**d, "system": key} for d in q["defects"]]
        fixed += [{**f, "system": key} for f in fixed_defects(key, days)]
        modules[key] = module_overlay(key, days)
        for run in metric_runs(key, days):
            counts = run["summary"]["counts"]
            executed = counts["passed"] + counts["failed"] + counts["broken"]
            trend.append({"id": run["id"], "system": key, "created": run["created"], "status": run["status"],
                          "passed": counts["passed"], "failed": counts["failed"] + counts["broken"],
                          "skipped": counts["skipped"], "passRate": round(100 * counts["passed"] / executed, 1) if executed else None})
            for test in run["summary"]["tests"]:
                if test.get("status") in EXECUTED:
                    row = slow.setdefault(test["fullName"], {"fullName": test["fullName"], "name": test["name"],
                                                              "testId": test.get("testId"), "system": key, "durations": []})
                    row["durations"].append(test.get("duration") or 0)
                    entry = failing.setdefault(test["fullName"], {"fullName": test["fullName"], "name": test["name"],
                                                                   "testId": test.get("testId"), "system": key,
                                                                   "failures": 0, "runs": 0, "last": None})
                    entry["runs"] += 1
                    entry["failures"] += 1 if test["status"] in FAILED else 0
                    entry["last"] = test["status"]
    slowest = sorted(({**{k: v for k, v in r.items() if k != "durations"},
                       "avg": round(sum(r["durations"]) / len(r["durations"]), 1), "max": max(r["durations"]),
                       "runs": len(r["durations"])} for r in slow.values()), key=lambda r: -r["avg"])[:10]
    most_failing = sorted((r for r in failing.values() if r["failures"]), key=lambda r: (-r["failures"], r["fullName"]))[:10]
    defects.sort(key=lambda d: (SEVERITY_RANK.get(d["severity"], 4), d["system"], d["fullName"]))
    return {"days": days, "system": system, "generated": now_iso(), "uptimeEvery": UPTIME_EVERY, "systems": cards,
            "trend": sorted(trend, key=lambda r: r["created"]), "slowest": slowest, "mostFailing": most_failing,
            "openDefects": defects, "fixed": fixed, "modules": modules}


# ============================================================================================= retest
def retest_plan(system):
    defects = open_defects(system)
    found = discovered()
    classes = {}
    for defect in defects:
        cls, method = defect["fullName"].rsplit(".", 1)
        classes.setdefault(cls, {"class": cls, "name": cls.rsplit(".", 1)[-1], "failing": [], "run": defect["run"]})
        classes[cls]["failing"].append(method)
    rows = []
    for cls, row in classes.items():
        plan_methods = with_dependencies(found, cls, row["failing"])
        rows.append({**row, "plan": plan_methods, "added": [m for m in plan_methods if m not in row["failing"]]})
    latest = max(defects, key=lambda d: d["created"]) if defects else None
    source = RUNS.get(latest["run"]) if latest else None
    return {"system": system, "classes": rows, "tests": sum(len(r["plan"]) for r in rows),
            "defects": len(defects), "env": (source or {}).get("env"), "urls": (source or {}).get("urls")}


def _retest_payload(source_run, tests, scope, origin):
    payload = {k: source_run.get(k) for k in ("system", "env", "urls", "execution", "browser", "pace", "grid",
                                               "extraProperties")}
    payload.update(scope=scope, tests=tests, preflight=True, retry=True, metrics=True, fullRun=False, origin=origin)
    return payload


def retest_request(system, env=None):
    plan_value = retest_plan(system)
    if not plan_value["classes"]:
        return None
    source = RUNS.get(plan_value["classes"][0]["run"]) or {"system": system}
    payload = _retest_payload(source, {r["name"]: r["failing"] for r in plan_value["classes"]}, "retest-tests",
                              {"type": "defects-retest"})
    if env:
        payload["env"], payload["urls"] = env, {}
    return payload


def defect_retest(payload):
    run, test = find_test(str(payload.get("run") or ""), str(payload.get("test") or ""))
    cls, method = test["fullName"].rsplit(".", 1)
    return create_run(_retest_payload(run, {cls.rsplit(".", 1)[-1]: [method]}, "retest-one",
                                      {"type": "defect-retest", "test": test["fullName"], "run": run["id"]}))


def defect_retest_all(payload):
    by_system = {}
    for item in payload.get("defects") or []:
        try:
            run, test = find_test(str(item.get("run") or ""), str(item.get("test") or ""))
        except NotFound:
            continue
        entry = by_system.setdefault(run["system"], {"run": run, "tests": {}})
        if run["created"] > entry["run"]["created"]:
            entry["run"] = run
        cls, method = test["fullName"].rsplit(".", 1)
        entry["tests"].setdefault(cls.rsplit(".", 1)[-1], [])
        if method not in entry["tests"][cls.rsplit(".", 1)[-1]]:
            entry["tests"][cls.rsplit(".", 1)[-1]].append(method)
    if not by_system:
        raise BadRequest("No retestable defect in the list")
    created = []
    for system, entry in by_system.items():
        created.append(run_summary(create_run(_retest_payload(entry["run"], entry["tests"], "retest-tests",
                                                              {"type": "defects-retest-all"}))))
    return {"runs": created}


# ============================================================================================= run views
def run_summary(run):
    summary = run.get("summary") or {}
    keys = ("id", "system", "title", "env", "envName", "scope", "status", "message", "created", "started", "finished",
            "progress", "estimated", "report", "schedule", "batch", "batchParallel", "error", "metrics", "fullRun",
            "envRetry", "notBefore", "execution", "group", "threads", "origin", "reportsInSpaces", "reportsDeleted",
            "classes", "modules", "preset")
    value = {k: run.get(k) for k in keys}
    value["counts"] = summary.get("counts")
    value["total"] = summary.get("total")
    value["timing"] = timing(run)
    value["completeFullRun"] = is_complete_full_run(run)
    return value


def runs_list(system=None):
    with lock:
        runs = sorted((r for r in RUNS.values() if not system or r["system"] == system),
                      key=lambda r: r["created"], reverse=True)[:RUN_LIST_LIMIT]
        active = sum(1 for r in RUNS.values() if r["status"] in ACTIVE)
        queued = sum(1 for r in RUNS.values() if r["status"] == "queued")
    return {"runs": [run_summary(r) for r in runs], "build": build_info(), "maxParallel": MAX_PARALLEL,
            "active": active, "queued": queued, "total": len(RUNS)}


def run_detail(run_id):
    run = RUNS.get(run_id)
    if not run:
        raise NotFound("Unknown run")
    log_path = RUNS_DIR / run_id / "console.log"
    log_text = ""
    if log_path.is_file():
        with open(log_path, "rb") as handle:
            size = handle.seek(0, 2)
            handle.seek(max(0, size - LOG_TAIL))
            log_text = handle.read().decode("utf-8", errors="replace")
    tests = []
    for test in (run.get("summary") or {}).get("tests", []):
        row = dict(test)
        if test["status"] in FAILED:
            row["kind"] = reliability.classify(test.get("message"), test.get("soft"), test.get("category", ""),
                                               test.get("exception", ""))
            _, entry = defect_of(run["system"], test)
            if entry:
                row["defect"] = {k: entry.get(k) for k in ("id", "severity", "note", "notBug", "reported")}
        row["quarantined"] = reliability.is_quarantined(run["system"], test.get("fullName"))
        tests.append(row)
    value = store_module.clean_run(run)
    value["summary"] = {**(run.get("summary") or {}), "tests": tests} if run.get("summary") else None
    value.update(log=log_text, timing=timing(run), healable=len(reliability.heal_candidates(tests)),
                 completeFullRun=is_complete_full_run(run), spaces=bool(run.get("reportsInSpaces")),
                 consoleAvailable=log_path.is_file() or bool(run.get("reportsInSpaces")))
    return value


def read_report_file(run_id, relative):
    """Bytes of a run file: local folder first, then Spaces."""
    base = (RUNS_DIR / run_id).resolve()
    target = (base / relative).resolve()
    if base in target.parents and target.is_file():
        return target.read_bytes()
    if reports.enabled() and reports.find(STORE, run_id, relative):
        url = reports.link(STORE, run_id, relative, seconds=300)
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.read()
    return None


def allure_steps(run_id, test):
    data = None
    if test.get("uuid"):
        raw = read_report_file(run_id, f"output/allure-results/{test['uuid']}-result.json")
        if raw:
            try:
                data = json.loads(raw.decode("utf-8"))
            except ValueError:
                data = None
    if not data:
        return []
    steps = []
    for step in data.get("steps") or []:
        name = step.get("name") or ""
        status = step.get("status")
        steps.append(name + ("" if status in (None, "passed") else f"  [{status}]"))
    return steps


def console_lines(run_id, name, limit=40):
    raw = read_report_file(run_id, "console.log")
    if not raw or not name:
        return []
    lines = raw.decode("utf-8", errors="replace").splitlines()
    picked = []
    for index, line in enumerate(lines):
        if name in line:
            picked += lines[index:index + 15]
        if len(picked) >= limit:
            break
    return picked[:limit]


def seen_before(system, test, run):
    signature = bugreport.signature(test)
    day = (run.get("started") or run["created"])[:10]
    return first_failure(system, signature) < day


def bug_suggestions(run_id):
    run = RUNS.get(run_id)
    if not run:
        raise NotFound("Unknown run")
    bugs = []
    for test in (run.get("summary") or {}).get("tests", []):
        if test["status"] not in FAILED:
            continue
        _, entry = defect_of(run["system"], test)
        kind = reliability.classify(test.get("message"), test.get("soft"), test.get("category", ""),
                                    test.get("exception", ""))
        bugs.append(bugreport.suggestion(run, test, kind=kind, steps=allure_steps(run_id, test),
                                         log_lines=console_lines(run_id, test.get("name")), registry_entry=entry,
                                         seen_before=seen_before(run["system"], test, run)))
    return {"run": run_summary(run), "bugs": bugs, "formats": {"docx": _has("docx"), "xlsx": _has("openpyxl")}}


def _has(module):
    import importlib.util
    return importlib.util.find_spec(module) is not None


def report_meta(run):
    return {"systemTitle": SYSTEMS[run["system"]]["title"], "envName": run.get("envName") or run.get("env"),
            "runId": run["id"], "started": run.get("started"), "finished": run.get("finished"),
            "execution": run.get("execution"), "status": run.get("status"), "urls": run.get("urls") or {},
            "counts": (run.get("summary") or {}).get("counts")}


def generate_bug_report(run_id, fmt, bugs=None):
    run = RUNS.get(run_id)
    if not run:
        raise NotFound("Unknown run")
    if fmt not in ("docx", "xlsx"):
        raise BadRequest("Format must be docx or xlsx")
    if bugs is None:
        bugs = [b for b in bug_suggestions(run_id)["bugs"] if b["include"]]
    cleaned = [bugreport.clean_bug(b) for b in bugs if (b or {}).get("include", True)]
    if not cleaned:
        raise BadRequest("Select at least one bug")
    today = now_iso()[:10]
    tests = {t["fullName"]: t for t in (run.get("summary") or {}).get("tests", [])}
    with registry_lock:
        for bug in cleaned:
            test = tests.get(bug["test"])
            if not test:
                continue
            signature, entry = ensure_defect(run, test, bug["severity"])
            if today not in entry["reported"]:
                entry["reported"].append(today)
            entry["title"] = bug["title"] or entry.get("title")
            save_defect(run["system"], signature, entry)
            bug["id"] = entry["id"]

    def load_image(bug):
        if not bug.get("shot") or not re.match(r"^[\w\-]+-attachment\.(png|jpg|jpeg)$", bug["shot"]):
            return None
        try:
            return read_report_file(run_id, f"output/allure-results/{bug['shot']}")
        except Exception:
            return None
    meta = report_meta(run)
    try:
        data = bugreport.write_docx(meta, cleaned, load_image) if fmt == "docx" else \
            bugreport.write_xlsx(meta, cleaned, load_image)
    except bugreport.ExportError as error:
        raise BadRequest(str(error))
    return f"bug-report-{run['system']}-{run_id}.{fmt}", data


def run_markdown(run, label):
    summary = (run or {}).get("summary") or {}
    counts = summary.get("counts") or {}
    lines = [f"### {label}: run `{run['id']}` — **{run.get('status')}**",
             f"- Passed {counts.get('passed', 0)} · Failed {counts.get('failed', 0) + counts.get('broken', 0)} · "
             f"Skipped {counts.get('skipped', 0)} · Bug checks {counts.get('bugChecks', 0)}"]
    if run.get("error") and run.get("status") != "passed":
        lines.append(f"- Note: {run['error'][:300]}")
    failures = [t for t in summary.get("tests", []) if t["status"] in FAILED]
    for test in failures[:15]:
        lines.append(f"  - `{test['fullName'].rsplit('.', 2)[-2]}.{test['name']}` {test.get('testId') or ''}: "
                     f"{(test.get('message') or '')[:200]}")
    return "\n".join(lines)


# ============================================================================================= Claude
def failures_context(run, tests, include_steps=True):
    parts = [f"Run `{run['id']}` · {SYSTEMS[run['system']]['title']} · environment {run.get('env')} · "
             f"status {run.get('status')}", ""]
    for test in tests[:30]:
        kind = reliability.classify(test.get("message"), test.get("soft"), test.get("category", ""),
                                    test.get("exception", ""))
        parts.append(f"### `{test['fullName']}` ({test.get('testId') or test['name']}) — {kind}")
        parts.append(f"- Exception: {test.get('exception') or '-'}")
        parts.append(f"- Message: {(test.get('message') or '')[:600]}")
        if test.get("firstAttempt"):
            parts.append(f"- First attempt: {test['firstAttempt']}")
        if include_steps:
            steps = allure_steps(run["id"], test)
            if steps:
                parts.append("- Steps: " + " → ".join(steps[:12]))
        if test.get("shot"):
            parts.append(f"- Screenshot: .runner-input/{test['shot']}")
        parts.append("")
    return "\n".join(parts)


def screenshot_files(run, tests, limit=10):
    files = []
    for test in tests:
        if test.get("shot") and len(files) < limit:
            data = read_report_file(run["id"], f"output/allure-results/{test['shot']}")
            if data:
                files.append({"name": test["shot"], "data": data, "size": len(data)})
    return files


def ask_claude(run_id, auto_run=False, heal=False):
    run = RUNS.get(run_id)
    if not run:
        raise NotFound("Unknown run")
    tests = [t for t in (run.get("summary") or {}).get("tests", []) if t["status"] in FAILED]
    if heal:
        tests = reliability.heal_candidates(tests)
        if not tests:
            raise BadRequest("No locator/wait failure to heal in this run")
        text = ("أصلح فقط الـ locators والانتظارات (waits) في Page Objects / Flows لهذه الاختبارات الفاشلة. "
                "ممنوع تغيير أي تحقق أو قيمة متوقعة أو إضعافها. استخدم الصور المرفقة لفهم الصفحة.")
        title = f"Heal {len(tests)} locator/wait failure(s) · run {run_id}"
    else:
        if not tests:
            raise BadRequest("The run has no failure")
        text = ("حلل هذه الاختبارات الفاشلة وصنف كل فشل (PRODUCT_DEFECT / AUTOMATION_DEFECT / TEST_DATA_ERROR / "
                "ENVIRONMENT_ERROR / INCONCLUSIVE) مع الدليل. أصلح أخطاء الأتمتة فقط بدون إضعاف أي تحقق، "
                "واترك أخطاء المنتج تفشل واكتب لها وصف خطأ واضح.")
        title = f"Failures of run {run_id} ({len(tests)})"
    try:
        return agent.create(run["system"], text, files=screenshot_files(run, tests), auto_run=auto_run, mode="code",
                            title=title, context=failures_context(run, tests),
                            origin={"type": "heal" if heal else "run", "id": run_id},
                            system_title=SYSTEMS[run["system"]]["title"])
    except agent.AgentError as error:
        raise BadRequest(str(error))


def reliability_ask_claude(system):
    if system not in SYSTEMS:
        raise BadRequest("Unknown system")
    with lock:
        runs = list(RUNS.values())
    rows = reliability.flaky(runs, system)
    if not rows:
        raise BadRequest("No flaky test in this system")
    context = "\n".join(f"- `{r['test']}` sequence {r['sequence']} (score {r['score']}, flips {r['flips']}, "
                        f"retries {r['retries']})" for r in rows[:30])
    text = ("هذه الاختبارات غير مستقرة (flaky). حدد سبب عدم الاستقرار لكل اختبار (انتظار ناقص، بيانات مشتركة، "
            "ترتيب، بيئة) وأصلح أسباب الأتمتة فقط بدون إضعاف أي تحقق وبدون إضافة sleep ثابت.")
    try:
        return agent.create(system, text, mode="code", title=f"Flaky tests · {SYSTEMS[system]['title']}",
                            context=context, origin={"type": "reliability", "id": system},
                            system_title=SYSTEMS[system]["title"])
    except agent.AgentError as error:
        raise BadRequest(str(error))


def merge_request(request_id, force=False):
    try:
        result = agent.merge(request_id, force=force)
    except agent.AgentError as error:
        raise BadRequest(str(error))
    if result.get("ok"):
        refresh_catalog()
        request = agent.get(request_id) or {}
        origin = request.get("origin") or {}
        if request.get("autoRun") and origin.get("type") in ("run", "heal") and origin.get("id") in RUNS:
            try:
                result["rerun"] = run_summary(rerun(origin["id"], only_failed=True))
            except (BadRequest, NotFound) as error:
                result["rerunError"] = str(error)
        elif request.get("autoRun") and origin.get("type") == "manual":
            classes = [Path(c["path"]).stem for c in request.get("changed") or []
                       if c["status"] != "deleted" and c["path"].startswith("src/test/java/")
                       and class_exists(request["system"], Path(c["path"]).stem)]
            if classes:
                try:
                    result["rerun"] = run_summary(create_run({"system": request["system"], "scope": "classes",
                                                              "classes": classes, "fullRun": False,
                                                              "origin": {"type": "claude", "id": request_id}}))
                except BadRequest as error:
                    result["rerunError"] = str(error)
    return result


# ============================================================================================= update from GitHub
UPDATE_ITEMS = ["pom.xml", "src", "suites", "tools", "runner/static"]


def update_allowed():
    if not GITHUB_REPO:
        return False, "Set RUNNER_GITHUB_REPO (owner/name) to enable updates"
    if (ROOT / ".git").exists() and not ALLOW_UPDATE:
        return False, "This folder is a git checkout: use git, or set RUNNER_ALLOW_UPDATE=1"
    return True, ""


def update_from_github():
    allowed, reason = update_allowed()
    if not allowed:
        raise BadRequest(reason)
    if UPDATE["status"] == "updating":
        raise BadRequest("An update is already running")
    UPDATE.update(status="updating", message="Downloading", at=now_iso())

    def worker():
        try:
            url = f"https://codeload.github.com/{GITHUB_REPO}/zip/refs/heads/{quote(GITHUB_BRANCH)}"
            headers = {"User-Agent": "automation-runner"}
            if GITHUB_TOKEN:
                headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"
            with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=120) as response:
                data = response.read()
            with tempfile.TemporaryDirectory() as folder:
                with zipfile.ZipFile(io.BytesIO(data)) as archive:
                    for member in archive.namelist():
                        target = (Path(folder) / member).resolve()
                        if Path(folder).resolve() not in target.parents and target != Path(folder).resolve():
                            raise RuntimeError("Unsafe path in the downloaded archive")
                    archive.extractall(folder)
                top = next(Path(folder).iterdir())
                changed_py = False
                for item in UPDATE_ITEMS:
                    source, target = top / item, ROOT / item
                    if not source.exists():
                        continue
                    if source.is_dir():
                        keep = [p for p in target.glob("allure-*")] if item == "tools" else []
                        staged = {}
                        for kept in keep:
                            staged[kept.name] = Path(folder) / ("keep-" + kept.name)
                            shutil.move(str(kept), staged[kept.name])
                        shutil.rmtree(target, ignore_errors=True)
                        shutil.copytree(source, target)
                        for name, path in staged.items():
                            shutil.move(str(path), target / name)
                    else:
                        shutil.copy2(source, target)
                for source in (top / "runner").glob("*.py"):
                    target = RUNNER_DIR / source.name
                    if not target.is_file() or target.read_bytes() != source.read_bytes():
                        shutil.copy2(source, target)
                        changed_py = True
            UPDATE.update(message="Rebuilding the code snapshot")
            refresh_catalog()
            build_snapshot("update", force=True)
            UPDATE.update(status="done", message="Updated from GitHub" + (" - restarting when idle" if changed_py else ""),
                          at=now_iso())
            if changed_py:
                restart_when_idle()
        except Exception as error:
            UPDATE.update(status="failed", message=f"{type(error).__name__}: {error}"[:500], at=now_iso())
    threading.Thread(target=worker, daemon=True).start()
    return UPDATE


def restart_when_idle():
    def worker():
        while True:
            with lock:
                busy = any(r["status"] in ACTIVE | {"queued"} for r in RUNS.values())
            if not busy:
                break
            time.sleep(5)
        log("restarting the runner after an update")
        subprocess.Popen([sys.executable, str(RUNNER_DIR / "server.py")] + sys.argv[1:], cwd=ROOT, **process_flags())
    threading.Thread(target=worker, daemon=True).start()


# ============================================================================================= orchestrator glue
class OrchestratorServices:
    """What the orchestrator may use from the server."""

    def system_exists(self, system):
        return system in SYSTEMS and config.spec(system) is not None

    def system_title(self, system):
        return SYSTEMS.get(system, {}).get("title", system or "")

    def clean_emails(self, emails):
        try:
            return schedules.clean_emails(emails)
        except schedules.ScheduleError as error:
            raise orchestrator.JobError(str(error))

    def refresh_catalog(self):
        refresh_catalog()

    def class_exists(self, system, name):
        return class_exists(system, name)

    def classes_request(self, system, classes, env=None):
        return {"system": system, "env": env, "scope": "classes", "classes": classes, "metrics": True,
                "origin": {"type": "job"}}

    def scope_request(self, system, scope, env=None):
        return {"system": system, "env": env, "scope": scope, "metrics": True, "origin": {"type": "job"}}

    def retest_request(self, system, env=None):
        return retest_request(system, env)

    def rerun_failed_request(self, run_id):
        run = RUNS.get(run_id)
        return rerun_payload(run, only_failed=True) if run else None

    def create_run(self, payload):
        try:
            return run_summary(create_run(payload))
        except BadRequest as error:
            raise orchestrator.JobError(str(error))

    def run_state(self, run_id):
        run = RUNS.get(run_id)
        return json.loads(json.dumps(store_module.clean_run(run))) if run else None

    def stop_run(self, run_id):
        try:
            stop_run(run_id)
        except (BadRequest, NotFound):
            pass

    def run_markdown(self, run, label):
        return run_markdown(run, label)

    def run_failures(self, run_id):
        run = RUNS.get(run_id)
        rows = []
        for test in ((run or {}).get("summary") or {}).get("tests", []):
            if test["status"] in FAILED:
                rows.append({**test, "kind": reliability.classify(test.get("message"), test.get("soft"),
                                                                  test.get("category", ""), test.get("exception", ""))})
        return rows

    def bug_report(self, run_id, fmt):
        return generate_bug_report(run_id, fmt)

    def release(self, system):
        return health(quality(system))

    def email_ready(self):
        return schedules.email_status()["ready"]

    def send_email(self, emails, subject, html_body, text_body):
        return schedules.send_mail(emails, subject, html_body, text_body)

    def public_url(self):
        return schedules.PUBLIC_URL


# ============================================================================================= HTTP
CONTENT_TYPES = {".png": "image/png", ".jpg": "image/jpeg", ".html": "text/html; charset=utf-8",
                 ".log": "text/plain; charset=utf-8", ".txt": "text/plain; charset=utf-8",
                 ".md": "text/plain; charset=utf-8", ".xml": "text/xml; charset=utf-8", ".json": "application/json",
                 ".mp4": "video/mp4", ".webm": "video/webm", ".csv": "text/csv; charset=utf-8",
                 ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}
MAX_BODY = 60_000_000


def _query(url, name, default=""):
    return parse_qs(url.query).get(name, [default])[0]


class Handler(BaseHTTPRequestHandler):
    server_version = "AutomationControlCenter/2.0"
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        text = str(args[0]) if args else ""
        if any(part in text for part in ("/console", "GET /api/runs", "GET /api/agent/", "GET /api/jobs/")):
            return
        sys.stderr.write("%s - %s\n" % (self.log_date_time_string(), fmt % args))

    # ------------------------------------------------------------------ guards
    def _authorised(self):
        if not AUTH_PASSWORD:
            return True
        header = self.headers.get("Authorization") or ""
        if not header.startswith("Basic "):
            return False
        try:
            user, _, password = base64.b64decode(header[6:]).decode("utf-8").partition(":")
        except (ValueError, UnicodeDecodeError):
            return False
        return hmac.compare_digest(user, AUTH_USER) & hmac.compare_digest(password, AUTH_PASSWORD)

    def _host_allowed(self):
        allowed = self.server.allowed_hosts
        if allowed is None:
            return True
        host = (self.headers.get("Host") or "").rsplit(":", 1)[0].strip("[]").lower() \
            if not (self.headers.get("Host") or "").startswith("[") else \
            (self.headers.get("Host") or "").split("]")[0].strip("[").lower()
        return host in allowed

    def _guard(self, write):
        if not self._host_allowed():
            self._error(HTTPStatus.FORBIDDEN, "Host not allowed")
            return False
        if not self._authorised():
            body = b'{"error": "Authentication required"}'
            self.send_response(HTTPStatus.UNAUTHORIZED)
            self.send_header("WWW-Authenticate", 'Basic realm="Automation Runner", charset="UTF-8"')
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return False
        if write and self.headers.get("X-Runner") != "1":
            self._error(HTTPStatus.FORBIDDEN, "Missing X-Runner: 1 header")
            return False
        return True

    # ------------------------------------------------------------------ responses
    def _send(self, status, body, content_type="application/json; charset=utf-8", headers=None):
        data = body if isinstance(body, bytes) else body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        for key, value in (headers or {}).items():
            self.send_header(key, value)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(data)

    def _json(self, value, status=HTTPStatus.OK):
        self._send(status, json.dumps(value, ensure_ascii=False, default=str))

    def _error(self, status, message):
        self._json({"error": message}, status)

    def _download(self, name, data):
        kind = CONTENT_TYPES.get(Path(name).suffix.lower(), "application/octet-stream")
        self._send(HTTPStatus.OK, data, kind,
                   {"Content-Disposition": f"attachment; filename=\"{name}\"; filename*=UTF-8''{quote(name)}"})

    def _redirect(self, url):
        self.send_response(HTTPStatus.FOUND)
        self.send_header("Location", url)
        self.send_header("Content-Length", "0")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()

    def _read_body(self):
        """Read the whole request body before any answer: on HTTP/1.1 keep-alive an unread body makes Windows reset
        the connection, so even refused requests (403/401/404) must be drained. Returns False when too large."""
        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            length = 0
        if length > MAX_BODY:
            self.close_connection = True
            self._raw_body = b""
            return False
        self._raw_body = self.rfile.read(length) if length > 0 else b""
        return True

    def _body(self):
        raw = getattr(self, "_raw_body", b"")
        if not raw:
            return {}
        try:
            value = json.loads(raw)
        except ValueError:
            raise BadRequest("Invalid JSON")
        if not isinstance(value, dict):
            raise BadRequest("JSON body must be an object")
        return value

    # ------------------------------------------------------------------ dispatch
    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        if not self._guard(False):
            return
        self._dispatch(self._get)

    def do_POST(self):
        if not self._read_body():
            return self._error(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "Request too large")
        if not self._guard(True):
            return
        self._dispatch(self._post)

    def _dispatch(self, route):
        url = urlparse(self.path)
        try:
            route(url, unquote(url.path))
        except BadRequest as error:
            self._error(HTTPStatus.BAD_REQUEST, str(error))
        except NotFound as error:
            self._error(HTTPStatus.NOT_FOUND, str(error))
        except (config.ConfigError, schedules.ScheduleError, orchestrator.JobError, agent.AgentError) as error:
            self._error(HTTPStatus.BAD_REQUEST, str(error))
        except RuntimeError as error:
            self._error(HTTPStatus.INTERNAL_SERVER_ERROR, str(error))
        except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
            pass
        except Exception as error:
            log("handler error", self.path, repr(error))
            try:
                self._error(HTTPStatus.INTERNAL_SERVER_ERROR, f"{type(error).__name__}: {error}")
            except OSError:
                pass

    def _get(self, url, path):
        if path in ("/", "/index.html"):
            return self._send(HTTPStatus.OK, (STATIC_DIR / "index.html").read_bytes(), "text/html; charset=utf-8")
        if path == "/api/catalog":
            return self._json(catalog_view())
        if path == "/api/runs":
            return self._json(runs_list(_query(url, "system") or None))
        if path == "/api/build":
            return self._json(build_info())
        if path == "/api/build/log":
            build_id = BUILD.get("id")
            log_file = BUILD_DIR / build_id / "build.log" if build_id else None
            text = log_file.read_text(encoding="utf-8", errors="replace")[-20000:] if log_file and log_file.is_file() else ""
            return self._send(HTTPStatus.OK, text, "text/plain; charset=utf-8")
        if path == "/api/dashboard":
            system = _query(url, "system") or None
            if system and system not in SYSTEMS:
                raise BadRequest("Unknown system")
            days = max(1, min(365, int(_query(url, "days", "30") or 30)))
            return self._json(dashboard(system, days))
        if path == "/api/config":
            return self._json(config_view())
        if path == "/api/schedules":
            return self._json({"schedules": schedules.all_schedules(), "email": schedules.email_status(),
                               "publicUrl": schedules.PUBLIC_URL})
        if path == "/api/storage":
            return self._json(storage(force=_query(url, "fresh") == "1"))
        if path == "/api/agent":
            return self._json({"status": agent.status(), "requests": agent.list_requests()})
        if path == "/api/agent/auth":
            return self._json({"auth": agent.auth_status(force=_query(url, "fresh") == "1"),
                               "login": agent.status()["login"]})
        if path == "/api/reliability":
            system = _query(url, "system") or None
            return self._json(reliability_view(system))
        if path == "/api/jobs":
            return self._json({"workflows": orchestrator.workflows(), "jobs": orchestrator.list_jobs(),
                               "claude": {"installed": bool(agent.command())}, "email": schedules.email_status()})
        if path == "/api/jobs/prompt":
            return self._json({"prompt": orchestrator.default_prompt(_query(url, "type"), _query(url, "step"),
                                                                     _query(url, "system"))})
        if path == "/api/health":
            system = _query(url, "system")
            if system not in SYSTEMS:
                raise BadRequest("Unknown system")
            return self._json(ARCHIVE.health(system, suite_methods(system)))
        if path == "/api/archive":
            system = _query(url, "system") or None
            if system and system not in SYSTEMS:
                raise BadRequest("Unknown system")
            return self._json({"runs": ARCHIVE.run_list(system, int(_query(url, "limit", "200") or 200))})
        match = re.fullmatch(r"/api/archive/(cli|dashboard)/([\w\-]+)", path)
        if match:
            run = ARCHIVE.run(match.group(1), match.group(2))
            if not run:
                raise NotFound("Unknown archived run")
            return self._json({**run, "tests": [{**t, "state": bucket(t)} for t in run["tests"]]})
        match = re.fullmatch(r"/archive/(cli|dashboard)/([\w\-]+)/(.+)", path)
        if match:
            return self._archive_file(match.group(1), match.group(2), match.group(3))
        match = re.fullmatch(r"/api/runs/([\w\-]+)", path)
        if match:
            return self._json(run_detail(match.group(1)))
        match = re.fullmatch(r"/api/runs/([\w\-]+)/bugs", path)
        if match:
            return self._json(bug_suggestions(match.group(1)))
        match = re.fullmatch(r"/api/runs/([\w\-]+)/console", path)
        if match:
            return self._console(match.group(1), int(_query(url, "offset", "0") or 0))
        match = re.fullmatch(r"/api/retest/(\w+)", path)
        if match:
            if match.group(1) not in SYSTEMS:
                raise NotFound("Unknown system")
            return self._json(retest_plan(match.group(1)))
        match = re.fullmatch(r"/api/agent/([\w\-]+)/diff", path)
        if match:
            return self._json(agent.diff(match.group(1)))
        match = re.fullmatch(r"/api/agent/([\w\-]+)", path)
        if match:
            return self._json(agent.detail(match.group(1), int(_query(url, "after", "0") or 0)))
        match = re.fullmatch(r"/api/jobs/([\w\-]+)/steps/(\d+)/prompt", path)
        if match:
            return self._json(orchestrator.step_prompt(match.group(1), int(match.group(2))))
        match = re.fullmatch(r"/api/jobs/([\w\-]+)/steps/(\d+)/report", path)
        if match:
            name, data = orchestrator.step_report_docx(match.group(1), int(match.group(2)))
            return self._download(name, data)
        match = re.fullmatch(r"/api/jobs/([\w\-]+)/file/(.+)", path)
        if match:
            target = orchestrator.job_file(match.group(1), match.group(2))
            if not target:
                raise NotFound("Unknown file")
            return self._download(target.name, target.read_bytes())
        match = re.fullmatch(r"/api/jobs/([\w\-]+)", path)
        if match:
            job = orchestrator.get(match.group(1))
            if not job:
                raise NotFound("Unknown job")
            return self._json(job)
        match = re.fullmatch(r"/runs/([\w\-]+)/shot/([\w\-]+-attachment\.(?:png|jpg|jpeg))", path)
        if match:
            return self._run_file(match.group(1), "output/allure-results/" + match.group(2))
        match = re.fullmatch(r"/runs/([\w\-]+)/(.+)", path)
        if match:
            return self._run_file(match.group(1), match.group(2))
        raise NotFound("Not found")

    def _post(self, url, path):
        if path == "/api/runs":
            return self._json(run_summary(create_run(self._body())), HTTPStatus.CREATED)
        if path == "/api/runs/all":
            return self._json(create_all_systems(self._body()), HTTPStatus.CREATED)
        if path == "/api/runs/delete":
            body = self._body()
            return self._json(delete_runs(body.get("ids") or [], body.get("mode") or "runs"))
        if path == "/api/build":
            return self._json(start_manual_build())
        if path == "/api/update":
            return self._json(update_from_github())
        if path == "/api/schedules":
            return self._json(schedules.save(self._body(), schedule_request))
        if path == "/api/bugs/rate":
            return self._json(rate_defect(self._body()))
        if path == "/api/defects/retest":
            return self._json(run_summary(defect_retest(self._body())), HTTPStatus.CREATED)
        if path == "/api/defects/retest-all":
            return self._json(defect_retest_all(self._body()), HTTPStatus.CREATED)
        if path == "/api/storage/clean":
            body = self._body()
            return self._json(clean_storage(body.get("systems"), body.get("keep", 10), body.get("mode", "reports")))
        if path == "/api/agent":
            body = self._body()
            if body.get("system") not in SYSTEMS:
                raise BadRequest("Unknown system")
            return self._json(agent.create(body["system"], body.get("text"), files=body.get("files"),
                                           auto_run=bool(body.get("autoRun")), mode=body.get("mode") or "code",
                                           system_title=SYSTEMS[body["system"]]["title"]), HTTPStatus.CREATED)
        if path == "/api/agent/auth/login":
            return self._json(agent.auth_login(self._body().get("method") or "claudeai"))
        if path == "/api/agent/auth/code":
            return self._json(agent.auth_code(self._body().get("code")))
        if path == "/api/agent/auth/cancel":
            return self._json(agent.auth_cancel())
        if path == "/api/agent/auth/logout":
            return self._json(agent.auth_logout())
        if path == "/api/jobs":
            return self._json(orchestrator.create(self._body()), HTTPStatus.CREATED)
        if path == "/api/reliability/quarantine":
            body = self._body()
            if body.get("system") not in SYSTEMS or not body.get("test"):
                raise BadRequest("system and test are required")
            return self._json(reliability.set_quarantine(body["system"], str(body["test"])[:500], bool(body.get("on")),
                                                         str(body.get("reason") or ""), by="user"))
        if path == "/api/reliability/ask-claude":
            return self._json(reliability_ask_claude(self._body().get("system")), HTTPStatus.CREATED)
        match = re.fullmatch(r"/api/config/(\w+)/reset", path)
        if match:
            config.reset(match.group(1))
            refresh_catalog()
            return self._json(config_view())
        match = re.fullmatch(r"/api/config/(\w+)", path)
        if match:
            system = match.group(1)
            if system not in SYSTEMS:
                raise NotFound("Unknown system")
            known = [c["name"] for c in catalog()[system]["classes"]]
            config.save(system, self._body(), known)
            return self._json(config_view())
        match = re.fullmatch(r"/api/runs/([\w\-]+)/(stop|rerun|rerun-failed|fullrun|metrics|bugreport|ask-claude|heal)", path)
        if match:
            return self._run_action(match.group(1), match.group(2))
        match = re.fullmatch(r"/api/schedules/(\w+)/(delete|run|toggle|test)", path)
        if match:
            schedule_id, action = match.groups()
            if action == "delete":
                schedules.delete(schedule_id)
                return self._json({"deleted": schedule_id})
            if action == "run":
                return self._json(run_summary(schedules.fire(schedule_id, create_run, "manual")), HTTPStatus.CREATED)
            if action == "toggle":
                return self._json(schedules.toggle(schedule_id))
            return self._json({"sent": schedules.send_test(schedule_id)})
        match = re.fullmatch(r"/api/agent/([\w\-]+)/(reply|stop|merge|undo-merge|discard|delete)", path)
        if match:
            request_id, action = match.groups()
            if action == "reply":
                return self._json(agent.reply(request_id, self._body().get("text")))
            if action == "stop":
                return self._json(agent.stop(request_id))
            if action == "merge":
                return self._json(merge_request(request_id, bool(self._body().get("force"))))
            if action == "undo-merge":
                result = agent.undo_merge(request_id)
                refresh_catalog()
                return self._json(result)
            if action == "discard":
                return self._json(agent.discard(request_id))
            return self._json(agent.delete(request_id))
        match = re.fullmatch(r"/api/jobs/([\w\-]+)/(decide|retry|delete)", path)
        if match:
            job_id, action = match.groups()
            if action == "decide":
                body = self._body()
                return self._json(orchestrator.decide(job_id, body.get("decision"), body.get("note")))
            if action == "retry":
                return self._json(orchestrator.retry(job_id))
            return self._json(orchestrator.delete(job_id))
        match = re.fullmatch(r"/api/jobs/([\w\-]+)/steps/(\d+)/(prompt|rerun)", path)
        if match:
            job_id, index, action = match.group(1), int(match.group(2)), match.group(3)
            body = self._body()
            if action == "prompt":
                return self._json(orchestrator.set_step_prompt(job_id, index, body.get("prompt")))
            return self._json(orchestrator.rerun_step(job_id, index, body.get("prompt")))
        raise NotFound("Not found")

    def _run_action(self, run_id, action):
        run = RUNS.get(run_id)
        if not run:
            raise NotFound("Unknown run")
        body = self._body()
        if action == "stop":
            return self._json(run_summary(stop_run(run_id)))
        if action in ("rerun", "rerun-failed"):
            return self._json(run_summary(rerun(run_id, action == "rerun-failed")), HTTPStatus.CREATED)
        if action in ("fullrun", "metrics"):
            key = "fullRun" if action == "fullrun" else "metrics"
            run[key] = bool(body.get("count"))
            persist(run)
            return self._json(run_summary(run))
        if action == "bugreport":
            name, data = generate_bug_report(run_id, body.get("format") or "docx", body.get("bugs"))
            return self._download(name, data)
        return self._json(ask_claude(run_id, bool(body.get("autoRun")), heal=action == "heal"), HTTPStatus.CREATED)

    def _console(self, run_id, offset):
        run = RUNS.get(run_id)
        if not run:
            raise NotFound("Unknown run")
        log_file = RUNS_DIR / run_id / "console.log"
        if not log_file.is_file():
            return self._json({"offset": 0, "text": "", "status": run["status"], "progress": run.get("progress")})
        with open(log_file, "rb") as handle:
            handle.seek(max(0, offset))
            chunk = handle.read(256_000)
        return self._json({"offset": offset + len(chunk), "text": chunk.decode("utf-8", errors="replace"),
                           "status": run["status"], "progress": run.get("progress")})

    def _run_file(self, run_id, relative):
        if run_id not in RUNS:
            raise NotFound("Unknown run")
        base = (RUNS_DIR / run_id).resolve()
        target = (base / relative).resolve()
        if base not in target.parents or target.name in ("run.json", "files.json", "java.args"):
            raise NotFound("Not found")
        if target.is_file():
            return self._send(HTTPStatus.OK, target.read_bytes(),
                              CONTENT_TYPES.get(target.suffix.lower(), "application/octet-stream"))
        link = reports.link(STORE, run_id, relative) if reports.enabled() else None
        if link:
            return self._redirect(link)
        raise NotFound("Report file not available (deleted or not produced)")

    def _archive_file(self, source, run_id, relative):
        folder = ARCHIVE.folder_of(source, run_id)
        if folder is None:
            raise NotFound("Unknown archived run")
        base = folder.resolve()
        target = (base / relative).resolve()
        if base not in target.parents or not target.is_file() or target.name == "run.json":
            raise NotFound("Not found")
        return self._send(HTTPStatus.OK, target.read_bytes(),
                          CONTENT_TYPES.get(target.suffix.lower(), "application/octet-stream"))


def suite_methods(system):
    cat = catalog()[system]
    full = set(cat["full"])
    return {c["class"]: [{"method": m["name"], "id": m["id"], "description": m["description"], "module": c["module"]}
                         for m in c["methods"]] for c in cat["classes"] if c["name"] in full}


def config_view():
    systems = []
    for key, spec in config.systems().items():
        if key not in SYSTEMS:
            continue
        systems.append({"key": key, "title": SYSTEMS[key]["title"], "spec": spec, "modules": config.modules(key),
                        "customised": config.is_customised(key),
                        "classes": [c["name"] for c in catalog().get(key, {}).get("classes", [])]})
    return {"editable": True, "store": STORE.describe(), "systems": systems,
            "note": "Secret login defaults are never stored; logins reach the tests only as environment variables."}


def reliability_view(system=None):
    with lock:
        runs = list(RUNS.values())
    systems_wanted = [system] if system else list(SYSTEMS)
    result = {"settings": reliability.settings(), "systems": []}
    for key in systems_wanted:
        if key not in SYSTEMS:
            raise BadRequest("Unknown system")
        quarantine = [{"test": test, "name": test.rsplit(".", 1)[-1], **entry}
                      for test, entry in reliability.quarantine(key).items()]
        env_down = [run_summary(r) for r in sorted(runs, key=lambda r: r["created"], reverse=True)
                    if r["system"] == key and (r.get("envRetry") or r["status"] == "environment down")][:10]
        result["systems"].append({"key": key, "title": SYSTEMS[key]["title"], "flaky": reliability.flaky(runs, key),
                                  "quarantine": sorted(quarantine, key=lambda q: (not q.get("on"), q["test"])),
                                  "cutOff": reliability.cut_off_runs(runs, key), "envDown": env_down})
    return result


def create_all_systems(payload):
    """Queue one run per system (batch); at most parallelSystems of the batch run at once."""
    batch = datetime.now().strftime("%Y%m%d-%H%M%S-") + secrets.token_hex(3)
    try:
        parallel = max(1, min(int(payload.get("parallelSystems") or 1), MAX_PARALLEL))
    except (TypeError, ValueError):
        raise BadRequest("parallelSystems must be a number")
    created, errors = [], []
    for key, spec in config.systems().items():
        if key not in SYSTEMS:
            continue
        env = payload.get("env") if payload.get("env") in spec["envs"] else next(iter(spec["envs"]))
        request = {k: payload.get(k) for k in ("execution", "browser", "pace", "preflight", "retry", "metrics",
                                                "gridHost", "gridPort")}
        request.update(system=key, env=env, scope=payload.get("scope") or "full", batch=batch, batchParallel=parallel,
                       fullRun=payload.get("fullRun", True))
        try:
            created.append(run_summary(create_run(request)))
        except BadRequest as error:
            errors.append(f"{SYSTEMS[key]['title']}: {error}")
    if not created:
        raise BadRequest("; ".join(errors) or "No system could be queued")
    return {"batch": batch, "runs": created, "errors": errors}


# ============================================================================================= startup
class ExclusiveServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = os.name != "nt"

    def server_bind(self):
        if os.name == "nt" and hasattr(socket, "SO_EXCLUSIVEADDRUSE"):
            self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        super().server_bind()


def _read_pid():
    for path in (PID_FILE, LEGACY_LOCK):
        try:
            data = json.loads(path.read_text())
            return int(data.get("pid", 0)), int(data.get("port", 0)), path
        except (OSError, ValueError):
            continue
    return 0, 0, None


def _remote_active(port):
    try:
        request = urllib.request.Request(f"http://127.0.0.1:{port}/api/runs")
        if AUTH_PASSWORD:
            token = base64.b64encode(f"{AUTH_USER}:{AUTH_PASSWORD}".encode()).decode()
            request.add_header("Authorization", "Basic " + token)
        with urllib.request.urlopen(request, timeout=5) as response:
            data = json.loads(response.read().decode("utf-8"))
        return int(data.get("active") or 0) + int(data.get("queued") or 0)
    except Exception:
        return 0


def take_over(port, force=False):
    """Stop a runner already running from this folder (PID file) - only when it has no active run, or with --force."""
    pid, other_port, path = _read_pid()
    if pid and pid != os.getpid() and process_alive(pid):
        busy = _remote_active(other_port or port)
        if busy and not force:
            raise SystemExit(f"A runner is already active (pid {pid}, http://localhost:{other_port or port}) with "
                             f"{busy} queued/active run(s). Open it, or restart with --force (its runs end as "
                             "'interrupted').")
        log(f"taking over from runner pid {pid}")
        kill_pid(pid)
        for _ in range(40):
            if not process_alive(pid):
                break
            time.sleep(0.25)
    for stale in (PID_FILE, LEGACY_LOCK):
        try:
            stale.unlink()
        except OSError:
            pass


def stop_existing():
    pid, port, _ = _read_pid()
    if not pid or not process_alive(pid):
        for stale in (PID_FILE, LEGACY_LOCK):
            try:
                stale.unlink()
            except OSError:
                pass
        print("No runner is active.")
        return
    kill_pid(pid)
    for _ in range(40):
        if not process_alive(pid):
            break
        time.sleep(0.25)
    for stale in (PID_FILE, LEGACY_LOCK):
        try:
            stale.unlink()
        except OSError:
            pass
    print(f"Stopped runner pid {pid} (port {port}). Its active runs show as 'interrupted' on the next start.")


def allowed_hosts(bind_host):
    if AUTH_PASSWORD and bind_host in ("0.0.0.0", "::", ""):
        return None  # reachable by any name; Basic auth protects every request
    hosts = {"localhost", "127.0.0.1", "::1", bind_host.lower()}
    public = urlparse(schedules.PUBLIC_URL).hostname
    if public:
        hosts.add(public.lower())
    hosts |= {h.strip().lower() for h in os.environ.get("RUNNER_ALLOWED_HOSTS", "").split(",") if h.strip()}
    return hosts


def startup():
    """Load everything the server needs (also used by the tests)."""
    global STORE
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    STORE = store_module.open_store(DATA_DIR, log=log)
    config.load(STORE, set(SYSTEMS))
    load_history()
    load_defects()
    load_snapshots()
    reliability.load(DATA_DIR)
    agent.load()
    orchestrator.load(OrchestratorServices())
    schedules.load(STORE)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--port", type=int, default=_int_env("RUNNER_PORT", 8080))
    parser.add_argument("--host", default=os.environ.get("RUNNER_HOST", "127.0.0.1"),
                        help="bind address (default 127.0.0.1; expose only with RUNNER_PASSWORD behind HTTPS)")
    parser.add_argument("--stop", action="store_true", help="stop the runner that is already active, then exit")
    parser.add_argument("--force", action="store_true", help="take over even when the active runner has runs")
    args = parser.parse_args()
    if args.stop:
        return stop_existing()
    take_over(args.port, args.force)
    try:
        server = ExclusiveServer((args.host, args.port), Handler)
    except OSError as error:
        raise SystemExit(f"Port {args.port} is in use by another program ({error}). Use --port.")
    server.allowed_hosts = allowed_hosts(args.host)
    PID_FILE.write_text(json.dumps({"pid": os.getpid(), "port": args.port, "started": now_iso()}))
    if AUTH_PASSWORD == "" and args.host not in ("127.0.0.1", "localhost", "::1"):
        log("WARNING: bound to", args.host, "without RUNNER_PASSWORD - anyone who can reach it can run tests")
    startup()
    threading.Thread(target=scheduler, daemon=True, name="scheduler").start()
    threading.Thread(target=schedules.loop, args=(create_run,), daemon=True, name="schedules").start()
    if UPTIME_ENABLED:
        threading.Thread(target=uptime_monitor, daemon=True, name="uptime").start()
    threading.Thread(target=ARCHIVE.refresh, kwargs={"force": True}, daemon=True).start()
    wake.set()

    def shutdown(*_):
        threading.Thread(target=server.shutdown, daemon=True).start()
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, shutdown)
    log(f"Automation Control Center on http://localhost:{args.port}  (store {STORE.kind}, max parallel {MAX_PARALLEL}, "
        f"auth {'on' if AUTH_PASSWORD else 'off'}; Ctrl+C to stop)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        with lock:
            for transient in TRANSIENT.values():
                kill_tree(transient.get("process"))
        try:
            PID_FILE.unlink()
        except OSError:
            pass


if __name__ == "__main__":
    main()
