"""Daily scheduled runs and Gmail notifications.

A schedule = name + daily trigger (HH:MM, weekdays 0=Mon..6=Sun) + a saved run request + notification e-mails.
The timer loop checks every 20 s; a schedule fires when now >= nextAt (a run missed by more than MISSED_GRACE because
the runner was down is skipped, not started hours late). Saved run requests never contain logins: scheduled runs use
the credentials the runner inherits (environment / config/credentials.local.properties), per AGENTS.md.
Notifications: Gmail SMTP with STARTTLS on port 587, HTML + plain text.
"""

import html
import os
import re
import secrets
import smtplib
import ssl
import threading
import time
from datetime import datetime, timedelta
from email.message import EmailMessage

GMAIL_USER = os.environ.get("RUNNER_GMAIL_USER", "").strip()
GMAIL_APP_PASSWORD = os.environ.get("RUNNER_GMAIL_APP_PASSWORD", "").replace(" ", "").strip()
PUBLIC_URL = (os.environ.get("RUNNER_PUBLIC_URL", "") or "http://localhost:8080").rstrip("/")
SMTP_HOST, SMTP_PORT = "smtp.gmail.com", 587
LOOP_SECONDS = 20
MISSED_GRACE = timedelta(hours=6)
MAX_EMAILS = 20
GMAIL = re.compile(r"^[A-Za-z0-9._%+\-]{1,64}@(gmail\.com|googlemail\.com)$", re.I)
TIME = re.compile(r"^([01]\d|2[0-3]):([0-5]\d)$")
DAY_NAMES = ("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

_lock = threading.RLock()
_state = {"store": None, "items": {}}
_smtp = {"factory": smtplib.SMTP}  # replaced in tests


class ScheduleError(ValueError):
    pass


def now():
    return datetime.now().replace(microsecond=0)


def iso(value):
    return value.isoformat(timespec="seconds") if value else None


def load(store):
    items = {s["id"]: s for s in store.load_schedules() if isinstance(s, dict) and s.get("id")}
    with _lock:
        _state.update(store=store, items=items)
        for schedule in items.values():
            if schedule.get("enabled") and not schedule.get("nextAt"):
                schedule["nextAt"] = iso(next_at(schedule["trigger"]))
                store.save_schedule(schedule)


def email_status():
    ready = bool(GMAIL_USER and GMAIL_APP_PASSWORD)
    return {"ready": ready, "from": GMAIL_USER if ready else "", "publicUrl": PUBLIC_URL,
            "reason": "" if ready else "set RUNNER_GMAIL_USER and RUNNER_GMAIL_APP_PASSWORD"}


def all_schedules():
    with _lock:
        return sorted((dict(s) for s in _state["items"].values()), key=lambda s: (s.get("name") or "").lower())


def get(schedule_id):
    with _lock:
        value = _state["items"].get(schedule_id)
        return dict(value) if value else None


def next_at(trigger, after=None):
    """Next occurrence strictly after `after` (default now) on one of the trigger's weekdays."""
    after = after or now()
    hour, minute = (int(x) for x in trigger["time"].split(":"))
    days = set(trigger.get("days") or range(7))
    candidate = after.replace(hour=hour, minute=minute, second=0, microsecond=0)
    if candidate <= after:
        candidate += timedelta(days=1)
    for _ in range(8):
        if candidate.weekday() in days:
            return candidate
        candidate += timedelta(days=1)
    raise ScheduleError("No weekday selected")


def clean_emails(values):
    emails = []
    for raw in values or []:
        email = str(raw).strip().lower()
        if not email:
            continue
        if not GMAIL.match(email):
            raise ScheduleError(f"'{raw}' is not a Gmail address (@gmail.com / @googlemail.com only)")
        if email not in emails:
            emails.append(email)
    if len(emails) > MAX_EMAILS:
        raise ScheduleError(f"At most {MAX_EMAILS} e-mail addresses")
    return emails


def validate(payload, validate_run):
    """Clean schedule fields; validate_run(run_request) raises on an invalid run and returns the cleaned request."""
    if not isinstance(payload, dict):
        raise ScheduleError("Schedule must be an object")
    name = str(payload.get("name") or "").strip()
    if not 1 <= len(name) <= 80 or any(c in name for c in "\r\n"):
        raise ScheduleError("Name must be 1-80 characters")
    trigger = payload.get("trigger") or {}
    if trigger.get("type", "daily") != "daily":
        raise ScheduleError("Only daily triggers are supported")
    time_text = str(trigger.get("time") or "").strip()
    if not TIME.match(time_text):
        raise ScheduleError("Time must be HH:MM (24h)")
    try:
        days = sorted({int(d) for d in trigger.get("days") or []})
    except (TypeError, ValueError):
        raise ScheduleError("Days must be numbers 0-6")
    if not days or any(d < 0 or d > 6 for d in days):
        raise ScheduleError("Pick at least one weekday (0=Mon .. 6=Sun)")
    run = dict(payload.get("run") or {})
    run.pop("logins", None)  # never persist credentials
    for key in ("schedule", "batch", "after", "envRetry"):
        run.pop(key, None)
    run = validate_run(run)
    notify = payload.get("notify") or {}
    return {
        "name": name,
        "enabled": bool(payload.get("enabled", True)),
        "trigger": {"type": "daily", "time": time_text, "days": days},
        "run": run,
        "notify": {"emails": clean_emails(notify.get("emails")), "onlyFailures": bool(notify.get("onlyFailures"))},
    }


def save(payload, validate_run):
    fields = validate(payload, validate_run)
    with _lock:
        existing = _state["items"].get(str(payload.get("id") or ""))
        if existing:
            schedule = {**existing, **fields}
        else:
            schedule = {"id": secrets.token_hex(4), "created": iso(now()), **fields}
        schedule["nextAt"] = iso(next_at(schedule["trigger"])) if schedule["enabled"] else None
        _state["items"][schedule["id"]] = schedule
        _state["store"].save_schedule(schedule)
        return dict(schedule)


def delete(schedule_id):
    with _lock:
        if schedule_id not in _state["items"]:
            raise ScheduleError("Unknown schedule")
        del _state["items"][schedule_id]
        _state["store"].delete_schedule(schedule_id)


def toggle(schedule_id):
    with _lock:
        schedule = _state["items"].get(schedule_id)
        if not schedule:
            raise ScheduleError("Unknown schedule")
        schedule["enabled"] = not schedule.get("enabled")
        schedule["nextAt"] = iso(next_at(schedule["trigger"])) if schedule["enabled"] else None
        _state["store"].save_schedule(schedule)
        return dict(schedule)


def fire(schedule_id, create_run, reason="manual", at=None):
    with _lock:
        schedule = _state["items"].get(schedule_id)
        if not schedule:
            raise ScheduleError("Unknown schedule")
        request = {**schedule["run"], "schedule": {"id": schedule["id"], "name": schedule["name"]}}
    try:
        run = create_run(request)
        error = None
    except Exception as exc:  # the schedule records why it could not start
        run, error = None, f"{type(exc).__name__}: {exc}"
    with _lock:
        schedule = _state["items"].get(schedule_id)
        if schedule is None:
            return run
        schedule["lastRunAt"] = iso(now())
        schedule["lastReason"] = reason
        if run:
            schedule["lastRunId"] = run["id"]
            schedule.pop("lastError", None)
        else:
            schedule["lastError"] = error
        if reason == "timer" and schedule.get("enabled"):
            schedule["nextAt"] = iso(next_at(schedule["trigger"], at or now()))
        _state["store"].save_schedule(schedule)
    if error:
        raise ScheduleError(error)
    return run


def tick(create_run, at=None):
    """One timer pass; returns the ids that fired."""
    at = at or now()
    fired = []
    for schedule in all_schedules():
        if not schedule.get("enabled") or not schedule.get("nextAt"):
            continue
        due = datetime.fromisoformat(schedule["nextAt"])
        if at < due:
            continue
        if at - due > MISSED_GRACE:
            with _lock:
                live = _state["items"].get(schedule["id"])
                if live:
                    live["nextAt"] = iso(next_at(live["trigger"], at))
                    live["lastReason"] = f"missed {schedule['nextAt']} (runner was not running)"
                    _state["store"].save_schedule(live)
            continue
        try:
            fire(schedule["id"], create_run, "timer", at=max(at, due))
        except ScheduleError:
            pass  # recorded on the schedule (lastError); nextAt already moved on
        fired.append(schedule["id"])
    return fired


def loop(create_run):
    while True:
        try:
            tick(create_run)
        except Exception as error:  # keep the timer alive
            print("schedules loop error:", error)
        time.sleep(LOOP_SECONDS)


# ------------------------------------------------------------------------------------------------- e-mail
def send_mail(recipients, subject, html_body, text_body):
    status = email_status()
    if not status["ready"]:
        raise ScheduleError("E-mail is not configured: " + status["reason"])
    recipients = clean_emails(recipients)
    if not recipients:
        raise ScheduleError("No recipients")
    message = EmailMessage()
    message["From"] = GMAIL_USER
    message["To"] = ", ".join(recipients)
    message["Subject"] = subject
    message.set_content(text_body)
    message.add_alternative(html_body, subtype="html")
    with _smtp["factory"](SMTP_HOST, SMTP_PORT, timeout=30) as smtp:
        smtp.starttls(context=ssl.create_default_context())
        smtp.login(GMAIL_USER, GMAIL_APP_PASSWORD)
        smtp.send_message(message)
    return len(recipients)


def _duration(run):
    try:
        seconds = (datetime.fromisoformat(run["finished"]) - datetime.fromisoformat(run["started"])).total_seconds()
    except (KeyError, TypeError, ValueError):
        return ""
    return f"{int(seconds // 60)}m {int(seconds % 60)}s"


def run_email(run, title=""):
    """(subject, html, text) for a finished run."""
    counts = (run.get("summary") or {}).get("counts") or {}
    failures = [t for t in (run.get("summary") or {}).get("tests", []) if t.get("status") in ("failed", "broken")]
    link = f"{PUBLIC_URL}/#run={run['id']}"
    status = run.get("status", "")
    system = title or run.get("system", "")
    subject = f"[{'PASS' if status == 'passed' else 'ATTENTION'}] {system} · {run.get('envName') or run.get('env')} · {status}"
    facts = [("System", system), ("Environment", run.get("envName") or run.get("env") or ""), ("Result", status),
             ("Scope", run.get("scope", "")), ("Passed", counts.get("passed", 0)),
             ("Failed", (counts.get("failed", 0) or 0) + (counts.get("broken", 0) or 0)),
             ("Skipped", counts.get("skipped", 0)), ("Bug checks", counts.get("bugChecks", 0)),
             ("Duration", _duration(run)), ("Run", run["id"])]
    if run.get("error"):
        facts.append(("Note", run["error"]))
    colour = "#12814a" if status == "passed" else "#c62a35"
    rows = "".join(f"<tr><td style='padding:4px 12px 4px 0;color:#6a778b'>{html.escape(str(k))}</td>"
                   f"<td style='padding:4px 0'><b>{html.escape(str(v))}</b></td></tr>" for k, v in facts)
    failure_items = "".join(
        f"<li style='margin:6px 0'><b>{html.escape(t.get('testId') or t.get('name') or '')}</b> "
        f"{html.escape(t.get('title') or '')}<br><span style='color:#6a778b'>{html.escape((t.get('message') or '')[:240])}</span></li>"
        for t in failures[:8])
    more = f"<p>… and {len(failures) - 8} more.</p>" if len(failures) > 8 else ""
    html_body = (f"<div style='font-family:Segoe UI,Arial,sans-serif;font-size:14px;color:#172033'>"
                 f"<h2 style='margin:0 0 8px;color:{colour}'>{html.escape(subject)}</h2>"
                 f"<table style='border-collapse:collapse'>{rows}</table>"
                 + (f"<h3>Failures</h3><ol>{failure_items}</ol>{more}" if failures else "")
                 + f"<p><a href='{html.escape(link)}'>Open the run</a></p></div>")
    text_body = "\n".join(f"{k}: {v}" for k, v in facts) + "\n"
    if failures:
        text_body += "\nFailures:\n" + "\n".join(
            f"- {t.get('testId') or t.get('name')}: {(t.get('message') or '')[:200]}" for t in failures[:8]) + "\n"
    text_body += f"\nOpen the run: {link}\n"
    return subject, html_body, text_body


def notify(run, title=""):
    """E-mail the result of a scheduled run. Returns the note stored on the run, or None when not scheduled."""
    reference = run.get("schedule") or {}
    schedule = get(reference.get("id", ""))
    if not schedule:
        return None
    emails = schedule["notify"]["emails"]
    if not emails:
        return "no recipients"
    if schedule["notify"].get("onlyFailures") and run.get("status") == "passed":
        return "skipped (passed; failures only)"
    try:
        count = send_mail(emails, *run_email(run, title))
        return f"sent to {count} Gmail address(es)"
    except Exception as error:
        return f"not sent: {type(error).__name__}: {error}"


def send_test(schedule_id):
    schedule = get(schedule_id)
    if not schedule:
        raise ScheduleError("Unknown schedule")
    if not schedule["notify"]["emails"]:
        raise ScheduleError("Add at least one Gmail address first")
    subject = f"[TEST] Automation runner · {schedule['name']}"
    text = f"Test e-mail for schedule '{schedule['name']}'. Runs at {schedule['trigger']['time']} on " \
           f"{', '.join(DAY_NAMES[d] for d in schedule['trigger']['days'])}.\n{PUBLIC_URL}/#schedules\n"
    body = f"<div style='font-family:Segoe UI,Arial'><h3>{html.escape(subject)}</h3><p>{html.escape(text)}</p></div>"
    return send_mail(schedule["notify"]["emails"], subject, body, text)
