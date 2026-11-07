"""Reliability: failure classification, flaky tests, quarantine, environment-down retry settings, cut-off runs.

* classify(): environment | locator | timeout | data | assertion | other, from the failure message (bug checks are
  always assertions); the framework's failureCategory label is used when the message says nothing.
* Flaky: results flip between P(assed) and F(ailed) in the last WINDOW results (two flips or more; a single change
  is a fix or a regression), or the test passed only on retry (R). Score = (flips + retries) / (WINDOW - 1).
* Quarantine (runner/quarantine.json): auto when flips >= FLIPS in the last WINDOW results; auto-released after
  STABLE consecutive passes; manual on/off by the user. A user-released test is never auto-quarantined again.
  Quarantined tests are left out of open defects and release status (they still run and are still reported).
"""

import json
import os
import re
import threading
from datetime import datetime
from pathlib import Path


def _int(name, default):
    try:
        return int(os.environ.get(name, default))
    except ValueError:
        return default


FLAKY_AUTO = os.environ.get("RUNNER_FLAKY_AUTO", "1") != "0"
FLIPS = max(1, _int("RUNNER_FLAKY_FLIPS", 3))
WINDOW = max(3, _int("RUNNER_FLAKY_WINDOW", 8))
STABLE = max(1, _int("RUNNER_FLAKY_STABLE", 5))
ENV_RETRIES = max(0, _int("RUNNER_ENV_RETRIES", 3))
ENV_RETRY_MINUTES = max(1, _int("RUNNER_ENV_RETRY_MINUTES", 10))

KINDS = ("environment", "locator", "timeout", "data", "assertion", "other")
PATTERNS = [
    ("environment", re.compile(r"ERR_CONNECTION|ERR_NAME_NOT_RESOLVED|ERR_INTERNET_DISCONNECTED|ERR_TIMED_OUT|net::ERR_|"
                               r"HTTP\s*50\d|\b50[234]\s+(bad gateway|service unavailable|gateway time)|"
                               r"SessionNotCreated|session not created|chrome not reachable|Connection refused|"
                               r"unable to connect|UnknownHost|no route to host|DevToolsActivePort", re.I)),
    ("locator", re.compile(r"NoSuchElement|StaleElementReference|ElementClickIntercepted|ElementNotInteractable|"
                           r"no such element|stale element|element click intercepted|element not interactable|"
                           r"Unable to locate element|InvalidSelector", re.I)),
    ("timeout", re.compile(r"TimeoutException|timed out|Timeout|tried for \d+ second", re.I)),
    ("data", re.compile(r"already exists|duplicate|HTTP\s*4(09|22)|\b(409|422)\b|unique constraint", re.I)),
    ("assertion", re.compile(r"expected \[|AssertionError|expected:|Step failed|but found", re.I)),
]
CATEGORY_KIND = {"ENVIRONMENT_ERROR": "environment", "TIMEOUT": "timeout", "TEST_DATA_ERROR": "data",
                 "PRODUCT_DEFECT": "assertion", "CONFIGURATION_ERROR": "environment"}
EXECUTED = {"passed", "failed", "broken"}
CUT_OFF = {"stopped", "interrupted", "error", "environment down", "failed to start", "cancelled"}

_lock = threading.RLock()
_state = {"path": None, "data": {}}


def classify(message, soft=False, category="", exception=""):
    """Bug checks are assertions; the framework's own failureCategory wins when it maps to a kind (it knows that
    "No duplicate number" in an expectation is a product check, not a data clash); otherwise message patterns."""
    if soft:
        return "assertion"
    if (category or "").upper() in CATEGORY_KIND:
        return CATEGORY_KIND[category.upper()]
    text = " ".join(p for p in (exception or "", message or "") if p)
    for kind, pattern in PATTERNS:
        if pattern.search(text):
            return kind
    return "other"


def settings():
    return {"auto": FLAKY_AUTO, "flips": FLIPS, "window": WINDOW, "stable": STABLE,
            "envRetries": ENV_RETRIES, "envRetryMinutes": ENV_RETRY_MINUTES}


# ------------------------------------------------------------------------------------------- quarantine store
def load(runner_dir):
    path = Path(runner_dir) / "quarantine.json"
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        data = {}
    with _lock:
        _state.update(path=path, data=data if isinstance(data, dict) else {})


def _save():
    path = _state["path"]
    if path is None:
        return
    tmp = path.with_name(path.name + ".tmp")
    tmp.write_text(json.dumps(_state["data"], indent=1, ensure_ascii=False), encoding="utf-8")
    os.replace(tmp, path)


def quarantine(system):
    with _lock:
        return {k: dict(v) for k, v in (_state["data"].get(system) or {}).items()}


def is_quarantined(system, test):
    with _lock:
        return bool(((_state["data"].get(system) or {}).get(test) or {}).get("on"))


def set_quarantine(system, test, on, reason="", by="user"):
    now = datetime.now().isoformat(timespec="seconds")
    with _lock:
        entries = _state["data"].setdefault(system, {})
        entry = entries.get(test, {})
        if on:
            entry.update(on=True, auto=by == "auto", by=by, reason=(reason or "")[:300] or entry.get("reason", ""),
                         since=now)
            if by == "user":
                entry.pop("releasedByUser", None)
        else:
            entry.update(on=False, released=now, releasedBy=by)
            if by == "user":
                entry["releasedByUser"] = True
        entries[test] = entry
        _save()
        return dict(entry)


# ------------------------------------------------------------------------------------------- history / flaky
def code_of(test):
    status = test.get("status")
    if status not in EXECUTED:
        return None
    if status == "passed":
        return "R" if test.get("flaky") else "P"
    return "F"


def history(runs, system):
    """{fullName: [(code, run id, created)]} chronological, over finished metric runs of the system."""
    result = {}
    for run in sorted((r for r in runs if r.get("system") == system and r.get("metrics", True) is not False),
                      key=lambda r: r.get("started") or r.get("created") or ""):
        for test in (run.get("summary") or {}).get("tests", []):
            code = code_of(test)
            if code:
                result.setdefault(test.get("fullName"), []).append((code, run["id"], run.get("created")))
    return result


def flips_and_retries(codes):
    recent = codes[-WINDOW:]
    passes = [c in "PR" for c in recent]
    flips = sum(1 for a, b in zip(passes, passes[1:]) if a != b)
    return flips, sum(1 for c in recent if c == "R")


def flaky(runs, system):
    rows = []
    for test, entries in history(runs, system).items():
        codes = [c for c, _, _ in entries]
        flips, retries = flips_and_retries(codes)
        if flips >= 2 or retries >= 1:
            rows.append({"test": test, "name": test.rsplit(".", 1)[-1], "flips": flips, "retries": retries,
                         "score": round(min(1.0, (flips + retries) / (WINDOW - 1)), 2),
                         "sequence": "".join(codes[-WINDOW:]), "lastRun": entries[-1][1],
                         "quarantined": is_quarantined(system, test)})
    rows.sort(key=lambda r: (-r["score"], r["test"]))
    return rows


def auto_quarantine(runs, system):
    """Apply the auto rules after a run; returns [{test, on, reason}] for every change."""
    if not FLAKY_AUTO:
        return []
    changes = []
    current = quarantine(system)
    for test, entries in history(runs, system).items():
        codes = [c for c, _, _ in entries]
        entry = current.get(test) or {}
        if entry.get("on"):
            if entry.get("auto") and len(codes) >= STABLE and all(c == "P" for c in codes[-STABLE:]):
                set_quarantine(system, test, False, by="auto")
                changes.append({"test": test, "on": False, "reason": f"{STABLE} consecutive passes"})
            continue
        if entry.get("releasedByUser"):
            continue
        flips, _ = flips_and_retries(codes)
        if flips >= FLIPS:
            reason = f"{flips} pass/fail flips in the last {min(WINDOW, len(codes))} results"
            set_quarantine(system, test, True, reason, by="auto")
            changes.append({"test": test, "on": True, "reason": reason})
    return changes


def cut_off_runs(runs, system, limit=20):
    """Runs that did not reach the end of their scope (stopped, interrupted, crashed, env down, or fewer tests)."""
    rows = []
    for run in sorted((r for r in runs if r.get("system") == system), key=lambda r: r.get("created") or "", reverse=True):
        summary = run.get("summary") or {}
        executed = len(summary.get("tests") or [])
        estimated = run.get("estimated") or 0
        status = run.get("status")
        short = status in ("passed", "finished with failures") and estimated and executed < estimated * 0.9
        if status in CUT_OFF or short:
            rows.append({"id": run["id"], "status": status, "created": run.get("created"), "executed": executed,
                         "estimated": estimated, "scope": run.get("scope"),
                         "reason": run.get("error") or ("fewer tests than expected" if short else status)})
        if len(rows) >= limit:
            break
    return rows


def heal_candidates(tests):
    """Hard (non bug-check) failures that look like locator/wait problems."""
    return [t for t in tests if t.get("status") in ("failed", "broken") and not t.get("soft")
            and classify(t.get("message"), False, t.get("category", ""), t.get("exception", "")) in ("locator", "timeout")]
