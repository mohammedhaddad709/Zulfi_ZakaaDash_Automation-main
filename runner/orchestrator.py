"""QA orchestrator: multi-step jobs (new sprint, reported bug, regression, defects retest).

Steps run one after another in a job thread. A step that needs approval (or every step with askEach) waits for the
user: approve (merges Claude's code changes), redo (with a note), skip, cancel; a failed step can be retried.
Each step's output is attached to the next Claude prompt. Jobs live in runner/jobs/<id>/job.json; uploaded files
in inputs/, produced files (Word/Excel reports) in files/.

Server functions are reached through an OrchestratorServices object (see server.py) so this module stays testable.
"""

import base64
import json
import os
import secrets
import shutil
import threading
import time
from datetime import datetime
from pathlib import Path

import agent

RUNNER_DIR = Path(__file__).resolve().parent
JOBS_DIR = Path(os.environ.get("RUNNER_DATA_DIR") or RUNNER_DIR) / "jobs"
RUN_TERMINAL = {"passed", "finished with failures", "failed to start", "environment down", "stopped",
                "cancelled", "error", "interrupted"}
JOB_TERMINAL = {"done", "cancelled"}
MAX_OUTPUT = 6000
TRIAGE_ROUNDS = 2

WORKFLOWS = {
    "sprint": {"title": "New sprint", "steps": ["analyze", "design", "model", "coverage", "automate", "run", "triage",
                                                 "report", "release", "notify"]},
    "bug": {"title": "Reported bug", "steps": ["check", "automate", "run", "report", "notify"]},
    "regression": {"title": "Regression", "steps": ["run", "triage", "report", "release", "notify"]},
    "retest": {"title": "Defects retest", "steps": ["run", "release", "notify"]},
}

STEP_DEFS = {
    "analyze": {"title": "Analyse the stories", "kind": "agent-read", "approval": True, "optional": False},
    "design": {"title": "Design test cases", "kind": "agent-read", "approval": True, "optional": True},
    "model": {"title": "Model flows and test data", "kind": "agent-read", "approval": True, "optional": True},
    "coverage": {"title": "Coverage against existing automation", "kind": "agent-read", "approval": True, "optional": True},
    "automate": {"title": "Automate the tests", "kind": "agent-code", "approval": True, "optional": False},
    "check": {"title": "Does a test already catch the bug?", "kind": "agent-read:bugcheck", "approval": True,
              "optional": False},
    "run": {"title": "Run", "kind": "run-new", "approval": False, "optional": False},
    "triage": {"title": "Triage failures", "kind": "triage", "approval": False, "optional": True},
    "report": {"title": "Bug report (Word + Excel)", "kind": "report", "approval": False, "optional": True},
    "release": {"title": "Release status", "kind": "release", "approval": False, "optional": False},
    "notify": {"title": "E-mail summary", "kind": "notify", "approval": False, "optional": True},
}
RUN_KIND = {"sprint": "run-new", "bug": "run-new", "regression": "run-scope", "retest": "run-retest"}
RUN_TITLE = {"run-new": "Run the new tests", "run-scope": "Run the regression scope", "run-retest": "Retest open defects"}

PROMPTS = {
    "analyze": "حلل قصص المستخدم المرفقة لهذا السبرنت: النطاق، قواعد العمل، الحالات الحدية، المخاطر، والأسئلة المفتوحة "
               "لصاحب المنتج. اربط كل قاعدة بالصفحة أو الميزة الموجودة في المشروع إن وجدت. لا تكتب كودًا.",
    "design": "صمم حالات الاختبار من التحليل السابق: لكل حالة معرّف بنمط docs/testing/TEST_ID_NAMING_STANDARD.md، العنوان، "
              "الشروط المسبقة، الخطوات، النتيجة المتوقعة، الأولوية، والنوع (smoke / regression / e2e). لا تكتب كودًا.",
    "model": "صمم نموذج الاختبار: الصفحات (Page Objects) والتدفقات (Flows) المطلوبة، بيانات الاختبار وكيف تُولّد بدون "
             "بيانات دخول، وبوابات المراحل (phase gates) لكل حالة. لا تكتب كودًا.",
    "coverage": "قارن الحالات المصممة بالاختبارات الموجودة (docs/testing/FEATURE_TEST_CATALOG.md و src/test/java): "
                "ما المغطى، ما المغطى جزئيًا، وما الناقص. اقترح أين يضاف كل اختبار ناقص. لا تكتب كودًا.",
    "automate": "نفذ الاختبارات الناقصة حسب المخرجات السابقة: Page Object + Flow قبل الاختبارات، نفس هيكل الحزم الموجود، "
                "بوابات المراحل، وبدون أي إنشاء أو إرسال بيانات إلا خلف بوابة معطلة افتراضيًا. بعد إضافة كلاس اختبار "
                "شغّل python tools/gen_suites.py و python tools/gen_test_catalog.py. تأكد أن mvn -q test-compile ينجح.",
    "check": "هذا خطأ مُبلّغ عنه. ابحث في الاختبارات الحالية: هل يوجد اختبار يكشفه بالفعل؟ اذكر الكلاس والميثود والتحقق "
             "المعني، أو اشرح لماذا لا يكتشفه أي اختبار وما الذي يجب إضافته. لا تكتب كودًا.",
    "bugautomate": "أضف اختبارًا (أو تحققًا) يكشف هذا الخطأ المُبلّغ عنه، في الكلاس المناسب، مع @Issue يحمل معرّف الخطأ "
                   "إن وجد. الاختبار يجب أن يفشل ما دام الخطأ موجودًا ويمر بعد إصلاحه. شغّل python tools/gen_suites.py "
                   "إن أضفت كلاسًا جديدًا. تأكد أن mvn -q test-compile ينجح.",
    "triage": "هذه الاختبارات فشلت في التشغيل الأخير. صنّف كل فشل: PRODUCT_DEFECT أو AUTOMATION_DEFECT أو TEST_DATA_ERROR "
              "أو ENVIRONMENT_ERROR أو INCONCLUSIVE، مع الدليل من الرسالة. أصلح أخطاء الأتمتة فقط (locators، waits، "
              "التدفق) بدون إضعاف أي تحقق. لا تغير أي توقع لإخفاء خطأ في المنتج.",
}

_lock = threading.RLock()
_jobs = {}
_signals = {}
_threads = {}
_services = {"value": None}


class JobError(Exception):
    pass


def now_iso():
    return datetime.now().isoformat(timespec="seconds")


def services():
    if _services["value"] is None:
        raise JobError("Orchestrator is not loaded")
    return _services["value"]


# ------------------------------------------------------------------------------------------------ persistence
def _folder(job_id):
    return JOBS_DIR / job_id


def _save(job):
    folder = _folder(job["id"])
    folder.mkdir(parents=True, exist_ok=True)
    tmp = folder / "job.json.tmp"
    tmp.write_text(json.dumps(job, indent=1, ensure_ascii=False), encoding="utf-8")
    os.replace(tmp, folder / "job.json")


def load(service_object):
    _services["value"] = service_object
    JOBS_DIR.mkdir(parents=True, exist_ok=True)
    for path in sorted(JOBS_DIR.glob("*/job.json")):
        try:
            job = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        with _lock:
            _jobs[job["id"]] = job
            _signals[job["id"]] = {"event": threading.Event(), "decision": None}
        if job["status"] == "running":
            step = job["steps"][job["current"]] if job["current"] < len(job["steps"]) else None
            if step and step["status"] == "running":
                step.update(status="failed", error="The runner stopped while this step was running", finished=now_iso())
            job["status"] = "failed"
            _save(job)
        elif job["status"] == "waiting":
            _start_thread(job["id"])


def workflows():
    result = {}
    for key, workflow in WORKFLOWS.items():
        result[key] = {"title": workflow["title"], "steps": [
            {"key": step, "title": _step_title(key, step), "kind": _step_kind(key, step),
             "optional": STEP_DEFS[step]["optional"], "approval": STEP_DEFS[step]["approval"]}
            for step in workflow["steps"]]}
    return result


def _step_kind(job_type, step):
    if step == "run":
        return RUN_KIND[job_type]
    if step == "automate" and job_type == "bug":
        return "agent-code:bugautomate"
    return STEP_DEFS[step]["kind"]


def _step_title(job_type, step):
    if step == "run":
        return RUN_TITLE[RUN_KIND[job_type]]
    if step == "automate" and job_type == "bug":
        return "Add the bug's test"
    return STEP_DEFS[step]["title"]


def _prompt_key(job_type, step):
    return "bugautomate" if job_type == "bug" and step == "automate" else step


def default_prompt(job_type, step, system=None):
    if job_type not in WORKFLOWS or step not in WORKFLOWS[job_type]["steps"]:
        raise JobError("Unknown workflow step")
    return PROMPTS.get(_prompt_key(job_type, step), "")


def summary(job):
    keys = ("id", "type", "title", "system", "status", "created", "finished", "current", "askEach", "error")
    value = {k: job.get(k) for k in keys}
    value["steps"] = [{"key": s["key"], "title": s["title"], "status": s["status"]} for s in job["steps"]]
    return value


def list_jobs():
    with _lock:
        return sorted((summary(j) for j in _jobs.values()), key=lambda j: j["created"] or "", reverse=True)


def get(job_id):
    with _lock:
        job = _jobs.get(job_id)
        return json.loads(json.dumps(job)) if job else None


# ------------------------------------------------------------------------------------------------ create
def create(payload):
    job_type = payload.get("type")
    if job_type not in WORKFLOWS:
        raise JobError("Unknown workflow")
    system = payload.get("system")
    svc = services()
    if not svc.system_exists(system):
        raise JobError("Unknown system")
    text = str(payload.get("text") or "").strip()
    if job_type in ("sprint", "bug") and not text:
        raise JobError("Describe the sprint stories / the reported bug")
    if len(text) > agent.MAX_TEXT:
        raise JobError("Text is too long")
    included = payload.get("steps")
    keys = []
    for step in WORKFLOWS[job_type]["steps"]:
        if STEP_DEFS[step]["optional"] and included is not None and step not in included:
            continue
        keys.append(step)
    needs_claude = any(_step_kind(job_type, k).startswith(("agent", "triage")) for k in keys)
    if needs_claude and not agent.command():
        raise JobError("This workflow needs Claude Code, which is not installed on the runner machine")
    emails = payload.get("emails") or []
    if isinstance(emails, str):
        emails = [e.strip() for e in emails.replace(";", ",").split(",") if e.strip()]
    emails = svc.clean_emails(emails)
    files = agent.clean_files(payload.get("files"))
    scope = payload.get("scope") or "full"
    if job_type == "regression" and scope not in ("full", "e2e", "e2e-only"):
        raise JobError("Regression scope must be full, e2e or e2e-only")
    prompts = payload.get("prompts") or {}
    job_id = datetime.now().strftime("%Y%m%d-%H%M%S-") + secrets.token_hex(2)
    job = {
        "id": job_id, "type": job_type, "title": (str(payload.get("title") or "").strip()
                                                  or f"{WORKFLOWS[job_type]['title']} · {svc.system_title(system)}")[:140],
        "system": system, "text": text, "scope": scope, "emails": emails, "askEach": bool(payload.get("askEach")),
        "files": [{"name": f["name"], "size": f["size"]} for f in files], "status": "running", "created": now_iso(),
        "finished": None, "current": 0, "error": None, "env": payload.get("env"),
        "steps": [{"key": key, "kind": _step_kind(job_type, key), "title": _step_title(job_type, key),
                   "approval": STEP_DEFS[key]["approval"], "status": "pending", "started": None, "finished": None,
                   "output": "", "files": [], "runIds": [], "agentIds": [], "classes": [], "rounds": 0,
                   "prompt": (str(prompts.get(key)).strip()[:20000] or None) if prompts.get(key) else None,
                   "sentPrompt": None, "note": None, "error": None}
                  for key in keys],
    }
    inputs = _folder(job_id) / "inputs"
    inputs.mkdir(parents=True, exist_ok=True)
    for item in files:
        (inputs / item["name"]).write_bytes(item["data"])
    with _lock:
        _jobs[job_id] = job
        _signals[job_id] = {"event": threading.Event(), "decision": None}
    _save(job)
    _start_thread(job_id)
    return summary(job)


def _start_thread(job_id):
    with _lock:
        thread = _threads.get(job_id)
        if thread and thread.is_alive():
            return
        thread = threading.Thread(target=_run_job, args=(job_id,), daemon=True)
        _threads[job_id] = thread
    thread.start()


# ------------------------------------------------------------------------------------------------ decisions
def decide(job_id, decision, note=""):
    with _lock:
        job = _jobs.get(job_id)
        if not job:
            raise JobError("Unknown job")
        if decision not in ("approve", "redo", "skip", "cancel"):
            raise JobError("Decision must be approve, redo, skip or cancel")
        if decision == "cancel":
            if job["status"] in JOB_TERMINAL:
                raise JobError("Job already ended")
            job["cancelRequested"] = True
            if job["status"] in ("failed",):
                _cancel(job)
                return summary(job)
            if job["status"] == "running" and job["current"] < len(job["steps"]):
                current = job["steps"][job["current"]]
                for request_id in current.get("agentIds") or []:
                    try:
                        agent.stop(request_id)
                    except agent.AgentError:
                        pass
                for run_id in current.get("runIds") or []:
                    try:
                        services().stop_run(run_id)
                    except Exception:
                        pass
        elif job["status"] != "waiting":
            raise JobError("The job is not waiting for a decision")
        signal = _signals[job_id]
        signal["decision"] = {"decision": decision, "note": str(note or "").strip()[:4000]}
        signal["event"].set()
        return summary(job)


def retry(job_id):
    with _lock:
        job = _jobs.get(job_id)
        if not job:
            raise JobError("Unknown job")
        if job["status"] != "failed":
            raise JobError("Only a failed job can be retried")
        step = job["steps"][job["current"]]
        step.update(status="pending", error=None)
        job.update(status="running", error=None, finished=None)
        _save(job)
    _start_thread(job_id)
    return summary(job)


def delete(job_id):
    with _lock:
        job = _jobs.get(job_id)
        if not job:
            raise JobError("Unknown job")
        if job["status"] in ("running", "waiting"):
            raise JobError("Cancel the job first")
        _jobs.pop(job_id, None)
        _signals.pop(job_id, None)
    shutil.rmtree(_folder(job_id), ignore_errors=True)
    return {"deleted": job_id}


def step_prompt(job_id, index):
    job = get(job_id)
    if not job or not 0 <= index < len(job["steps"]):
        raise JobError("Unknown step")
    step = job["steps"][index]
    default = default_prompt(job["type"], step["key"]) if step["kind"].startswith(("agent", "triage")) else ""
    return {"editable": step["status"] in ("pending", "waiting", "failed", "skipped") and bool(default),
            "custom": step.get("prompt"), "sent": step.get("sentPrompt"), "default": default}


def set_step_prompt(job_id, index, prompt):
    with _lock:
        job = _jobs.get(job_id)
        if not job or not 0 <= index < len(job["steps"]):
            raise JobError("Unknown step")
        step = job["steps"][index]
        if step["status"] in ("running",):
            raise JobError("The step is running")
        step["prompt"] = str(prompt or "").strip()[:20000] or None
        _save(job)
        return step_prompt(job_id, index)


def rerun_step(job_id, index, prompt=None):
    """Run a finished/skipped/failed step again (and continue the job from there)."""
    with _lock:
        job = _jobs.get(job_id)
        if not job or not 0 <= index < len(job["steps"]):
            raise JobError("Unknown step")
        if job["status"] == "running" or (job["status"] == "waiting" and index != job["current"]):
            raise JobError("Wait until the job is idle (waiting on this step, failed or finished)")
        if prompt is not None:
            job["steps"][index]["prompt"] = str(prompt).strip()[:20000] or None
        for later in job["steps"][index:]:
            if later["status"] != "skipped" or later is job["steps"][index]:
                later.update(status="pending", error=None)
        job.update(current=index, status="running", error=None, finished=None)
        signal = _signals[job_id]
        if signal["event"] is not None:
            signal["decision"] = {"decision": "restart", "note": ""}
            signal["event"].set()
        _save(job)
    time.sleep(0.05)
    _start_thread(job_id)
    return summary(job)


def _cancel(job):
    for step in job["steps"]:
        for request_id in step.get("agentIds") or []:
            try:
                agent.stop(request_id)
            except agent.AgentError:
                pass
        if step["status"] in ("running", "waiting", "pending"):
            step["status"] = "cancelled" if step["status"] != "pending" else "pending"
    job.update(status="cancelled", finished=now_iso())
    _save(job)


# ------------------------------------------------------------------------------------------------ job thread
def _wait_decision(job, step):
    signal = _signals[job["id"]]
    with _lock:
        signal["event"].clear()
        signal["decision"] = None
        step["status"] = "waiting"
        job["status"] = "waiting"
        _save(job)
    while not signal["event"].wait(1.0):
        pass
    with _lock:
        decision = signal["decision"] or {"decision": "cancel", "note": ""}
        signal["decision"] = None
        signal["event"].clear()
        job["status"] = "running"
        step["status"] = "running"
        _save(job)
    return decision


def _run_job(job_id):
    while True:
        with _lock:
            job = _jobs.get(job_id)
            if not job or job["status"] in JOB_TERMINAL or job["status"] == "failed":
                return
            if job.get("cancelRequested"):
                _cancel(job)
                return
            if job["current"] >= len(job["steps"]):
                job.update(status="done", finished=now_iso())
                _save(job)
                return
            step = job["steps"][job["current"]]
        if step["status"] in ("done", "skipped"):
            with _lock:
                job["current"] += 1
            continue
        if step["status"] == "waiting":
            outcome = _decision_loop(job, step, resumed=True)
        else:
            outcome = _execute(job, step)
        if outcome == "stop":
            return
        if outcome == "next":
            with _lock:
                job["current"] += 1
                _save(job)


def _execute(job, step):
    with _lock:
        step.update(status="running", started=now_iso(), finished=None, error=None)
        job["status"] = "running"
        _save(job)
    try:
        EXECUTORS[step["kind"].split(":")[0]](job, step)
    except Exception as error:
        with _lock:
            if job.get("cancelRequested"):
                _cancel(job)
                return "stop"
            step.update(status="failed", error=f"{type(error).__name__}: {error}"[:2000], finished=now_iso())
            job.update(status="failed", error=step["error"])
            _save(job)
        return "stop"
    if job.get("cancelRequested"):
        _cancel(job)
        return "stop"
    if step.get("status") == "done":  # an executor may finish the step itself (e.g. triage after its own approval)
        return "next"
    return _decision_loop(job, step)


def _decision_loop(job, step, resumed=False):
    needs = step["approval"] or job.get("askEach")
    while needs:
        decision = _wait_decision(job, step)
        kind = decision["decision"]
        if kind == "restart":
            return "again"
        if kind == "cancel":
            _cancel(job)
            return "stop"
        if kind == "skip":
            with _lock:
                step.update(status="skipped", finished=now_iso(), note=decision["note"] or step.get("note"))
                _save(job)
            return "next"
        if kind == "redo":
            with _lock:
                step["note"] = decision["note"]
            outcome = _execute_redo(job, step)
            if outcome == "stop":
                return "stop"
            continue
        if kind == "approve":
            if step["kind"].startswith(("agent-code", "triage")):
                merged = _merge_step(job, step)
                if not merged:
                    continue  # conflicts recorded on the step; ask again
            break
    with _lock:
        step.update(status="done", finished=now_iso())
        _save(job)
    return "next"


def _execute_redo(job, step):
    with _lock:
        step.update(status="running", error=None)
        _save(job)
    try:
        EXECUTORS[step["kind"].split(":")[0]](job, step, redo=True)
    except Exception as error:
        with _lock:
            step.update(status="failed", error=f"{type(error).__name__}: {error}"[:2000], finished=now_iso())
            job.update(status="failed", error=step["error"])
            _save(job)
        return "stop"
    return "ok"


def _merge_step(job, step):
    if not step["agentIds"]:
        return True
    request_id = step["agentIds"][-1]
    request = agent.get(request_id)
    if not request or not request.get("changed"):
        return True
    if request.get("merged"):
        return True
    result = agent.merge(request_id)
    if not result["ok"]:
        with _lock:
            step["error"] = "Merge conflict (the project changed these files meanwhile): " + ", ".join(result["conflicts"])
            _save(job)
        return False
    with _lock:
        step["error"] = None
        step["classes"] = _new_test_classes(request.get("changed") or [])
        step["output"] += f"\n\n---\n**Merged** {len(result['files'])} file(s) into the project."
        _save(job)
    services().refresh_catalog()
    return True


def _new_test_classes(changes):
    names = []
    for change in changes:
        path = change["path"]
        if change["status"] != "deleted" and path.startswith("src/test/java/") and path.endswith(".java"):
            names.append(Path(path).stem)
    return names


# ------------------------------------------------------------------------------------------------ prompts
def _context(job, step):
    parts = []
    for previous in job["steps"]:
        if previous is step:
            break
        if previous["status"] == "done" and previous.get("output"):
            parts.append(f"### {previous['title']}\n\n{previous['output'][:MAX_OUTPUT]}")
    return "\n\n".join(parts)


def build_prompt(job, step, extra=""):
    svc = services()
    instruction = step.get("prompt") or default_prompt(job["type"], step["key"])
    lines = [f"# {WORKFLOWS[job['type']]['title']} — {step['title']} — {svc.system_title(job['system'])}", ""]
    if job.get("text"):
        lines += ["## طلب المستخدم", job["text"], ""]
    if job.get("files"):
        lines += ["## الملفات المرفقة (في المجلد .runner-input/)"] + [f"- .runner-input/{f['name']}" for f in job["files"]] + [""]
    context = _context(job, step)
    if context:
        lines += ["## مخرجات الخطوات السابقة", context, ""]
    if extra:
        lines += [extra, ""]
    if step.get("note"):
        lines += ["## ملاحظات المراجع (أعد الخطوة مع مراعاتها)", step["note"], ""]
    lines += ["## المطلوب في هذه الخطوة", instruction, "", "اكتب الرد بالعربية وبتنسيق Markdown."]
    return "\n".join(lines)


def _input_files(job):
    folder = _folder(job["id"]) / "inputs"
    return [{"name": p.name, "data": p.read_bytes(), "size": p.stat().st_size} for p in sorted(folder.glob("*"))] \
        if folder.is_dir() else []


def _ask_claude(job, step, mode, extra="", redo=False):
    svc = services()
    prompt = build_prompt(job, step, extra)
    with _lock:
        step["sentPrompt"] = prompt
        _save(job)
    previous = agent.get(step["agentIds"][-1]) if step["agentIds"] else None
    if redo and previous and previous.get("sessionId") and Path(previous.get("worktree") or "").is_dir() \
            and not previous.get("merged"):
        agent.reply(previous["id"], "ملاحظات المراجع: " + (step.get("note") or "") + "\n\nأعد الخطوة مع مراعاتها.")
        request_id = previous["id"]
        time.sleep(0.3)
    else:
        if previous and not previous.get("merged") and previous.get("mode") == "code":
            try:
                agent.discard(previous["id"])
            except agent.AgentError:
                pass
        created = agent.create(job["system"], step["title"], files=_input_files(job), mode=mode, title=step["title"],
                               origin={"type": "job", "id": job["id"], "step": step["key"]},
                               system_title=svc.system_title(job["system"]), prompt=prompt)
        request_id = created["id"]
        with _lock:
            step["agentIds"].append(request_id)
            _save(job)
    request = agent.wait(request_id, poll=1.0)
    if job.get("cancelRequested"):
        raise JobError("Cancelled")
    if not request or request["status"] != "done":
        raise JobError((request or {}).get("error") or "Claude request failed")
    return request


def exec_agent_read(job, step, redo=False):
    request = _ask_claude(job, step, "read", redo=redo)
    with _lock:
        step["output"] = request.get("result") or "(no answer)"
        _save(job)


def exec_agent_code(job, step, redo=False):
    request = _ask_claude(job, step, "code", redo=redo)
    changed = request.get("changed") or []
    listing = "\n".join(f"- `{c['path']}` ({c['status']})" for c in changed) or "- (no file changed)"
    with _lock:
        step["output"] = (request.get("result") or "") + f"\n\n**Changed files** (review the diff, then approve to merge):\n{listing}"
        _save(job)


def _wait_run(job, run_id):
    svc = services()
    while True:
        run = svc.run_state(run_id)
        if not run or run.get("status") in RUN_TERMINAL:
            return run
        if job.get("cancelRequested"):
            svc.stop_run(run_id)
            raise JobError("Cancelled")
        time.sleep(2)


def _run_and_report(job, step, payload, label):
    svc = services()
    run = svc.create_run(payload)
    with _lock:
        step["runIds"].append(run["id"])
        _save(job)
    finished = _wait_run(job, run["id"])
    text = svc.run_markdown(finished or run, label)
    with _lock:
        step["output"] = (step["output"] + "\n\n" if step["output"] else "") + text
        _save(job)
    return finished


def _new_classes(job):
    names = []
    for step in job["steps"]:
        if step["key"] == "automate":
            names += step.get("classes") or []
    return names


def exec_run_new(job, step, redo=False):
    svc = services()
    classes = [c for c in _new_classes(job) if svc.class_exists(job["system"], c)]
    if not classes:
        with _lock:
            step["output"] = "No new test class was merged by the automate step, so there is nothing new to run."
            _save(job)
        return
    with _lock:
        step["output"] = ""
    _run_and_report(job, step, svc.classes_request(job["system"], classes, job.get("env")), "New tests")


def exec_run_scope(job, step, redo=False):
    svc = services()
    with _lock:
        step["output"] = ""
    _run_and_report(job, step, svc.scope_request(job["system"], job.get("scope") or "full", job.get("env")),
                    f"Regression ({job.get('scope') or 'full'})")


def exec_run_retest(job, step, redo=False):
    svc = services()
    payload = svc.retest_request(job["system"], job.get("env"))
    if not payload:
        with _lock:
            step["output"] = "No open defect to retest."
            _save(job)
        return
    with _lock:
        step["output"] = ""
    _run_and_report(job, step, payload, "Defects retest")


def _last_run_id(job, before):
    for step in reversed(job["steps"][: job["steps"].index(before)]):
        if step.get("runIds"):
            return step["runIds"][-1]
    return None


def exec_triage(job, step, redo=False):
    svc = services()
    run_id = (step["runIds"] or [None])[-1] or _last_run_id(job, step)
    if not run_id:
        with _lock:
            step["output"] = "No run to triage."
            _save(job)
        return
    with _lock:
        step["output"], step["rounds"] = "", 0
        _save(job)
    for round_number in range(1, TRIAGE_ROUNDS + 1):
        failures = svc.run_failures(run_id)
        if not failures:
            with _lock:
                step["output"] += f"\n\n**Round {round_number}:** no failures left in run `{run_id}`."
                _save(job)
            break
        extra = "## الاختبارات الفاشلة\n" + "\n".join(
            f"- `{f['fullName']}` [{f.get('kind')}]: {(f.get('message') or '')[:400]}" for f in failures[:40])
        request = _ask_claude(job, step, "code", extra=extra)
        changed = request.get("changed") or []
        with _lock:
            step["rounds"] = round_number
            step["output"] += f"\n\n## Round {round_number}\n\n{request.get('result') or ''}\n\n**Changed files:** " \
                              + (", ".join(f"`{c['path']}`" for c in changed) or "none")
            _save(job)
        if not changed:
            break
        decision = _wait_decision(job, step)
        if decision["decision"] == "cancel":
            raise JobError("Cancelled")
        if decision["decision"] != "approve":
            with _lock:
                step["output"] += "\n\nFixes not merged (" + decision["decision"] + ")."
                _save(job)
            break
        if not _merge_step(job, step):
            raise JobError(step.get("error") or "Merge conflict")
        rerun = svc.rerun_failed_request(run_id)
        if not rerun:
            break
        finished = _run_and_report(job, step, rerun, f"Rerun after round {round_number}")
        run_id = (finished or {}).get("id") or run_id
    with _lock:
        step.update(status="done", finished=now_iso())
        _save(job)


def exec_report(job, step, redo=False):
    svc = services()
    run_ids = [r for s in job["steps"] for r in s.get("runIds") or []]
    if not run_ids:
        with _lock:
            step["output"] = "No run in this job, so no bug report."
            _save(job)
        return
    run_id = run_ids[-1]
    if not svc.run_failures(run_id):
        with _lock:
            step["output"] = f"Run `{run_id}` has no failure: nothing to report."
            _save(job)
        return
    folder = _folder(job["id"]) / "files"
    folder.mkdir(parents=True, exist_ok=True)
    produced = []
    for fmt in ("docx", "xlsx"):
        name, data = svc.bug_report(run_id, fmt)
        (folder / name).write_bytes(data)
        produced.append(name)
    with _lock:
        step["files"] = produced
        step["output"] = f"Bug report of run `{run_id}`: " + ", ".join(f"`{n}`" for n in produced)
        _save(job)


def exec_release(job, step, redo=False):
    health = services().release(job["system"])
    with _lock:
        step["output"] = (f"**{health.get('emoji', '')} {health.get('label', '')}** — {health.get('headline') or ''}\n\n"
                          f"- Pass rate: {health.get('passRate')}%\n- Open defects: {health.get('open')}\n"
                          f"- Critical: {health.get('critical')}")
        step["release"] = health
        _save(job)


def exec_notify(job, step, redo=False):
    svc = services()
    if not job.get("emails"):
        with _lock:
            step["output"] = "No e-mail address on this job: nothing sent."
            _save(job)
        return
    if not svc.email_ready():
        with _lock:
            step["output"] = "E-mail is not configured on the runner (RUNNER_GMAIL_USER / RUNNER_GMAIL_APP_PASSWORD): not sent."
            _save(job)
        return
    lines = [f"{job['title']} ({job['id']})", ""]
    for previous in job["steps"]:
        if previous is step:
            break
        lines.append(f"- {previous['title']}: {previous['status']}")
    release = next((s.get("release") for s in job["steps"] if s.get("release")), None)
    if release:
        lines += ["", f"Release: {release.get('label')} — {release.get('headline') or ''}"]
    text = "\n".join(lines) + f"\n\n{svc.public_url()}/#job={job['id']}\n"
    body = "<div style='font-family:Segoe UI,Arial'>" + "".join(
        f"<p>{line}</p>" if line else "" for line in text.replace("<", "&lt;").splitlines()) + "</div>"
    count = svc.send_email(job["emails"], f"[QA] {job['title']}", body, text)
    with _lock:
        step["output"] = f"Summary sent to {count} address(es)."
        _save(job)


EXECUTORS = {"agent-read": exec_agent_read, "agent-code": exec_agent_code, "run-new": exec_run_new,
             "run-scope": exec_run_scope, "run-retest": exec_run_retest, "triage": exec_triage,
             "report": exec_report, "release": exec_release, "notify": exec_notify}


def job_file(job_id, name):
    name = Path(name).name
    for folder in ("files", "inputs"):
        path = _folder(job_id) / folder / name
        if path.is_file():
            return path
    return None


def step_report_docx(job_id, index):
    import bugreport
    job = get(job_id)
    if not job or not 0 <= index < len(job["steps"]):
        raise JobError("Unknown step")
    step = job["steps"][index]
    return f"{job['id']}-{step['key']}.docx", bugreport.markdown_docx(f"{job['title']} — {step['title']}",
                                                                      step.get("output") or "")


def decode_files(files):
    """Helper for tests: [{name, text}] -> [{name, data(base64)}]."""
    return [{"name": f["name"], "data": base64.b64encode(f["text"].encode()).decode()} for f in files]
