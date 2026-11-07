"""Claude Code integration: requests run `claude -p` in a private copy of the project, never in the project itself.

* A request copies the project sources (pom.xml, src/, suites/, tools/, docs/, scripts/, AGENTS.md, README…) into
  RUNNER_AGENT_WORK/<id> and commits a throw-away git baseline there, so Claude's changes are an exact `git diff`.
  config/credentials.local.properties, target/, reports and runner state are never copied, and credentials /
  runner secrets are removed from Claude's environment.
* mode "code": Claude may edit the copy (acceptEdits; Bash limited to Maven / project tools / git status+diff).
  mode "read": analysis only (no edit tools); the copy is discarded when the request ends.
* merge copies the changed files into the project after a conflict check (a project file changed since the copy
  was taken is a conflict unless forced) and keeps a backup, so undo-merge restores the previous files exactly.
* Sign-in: `claude auth status|login [--console]|logout`; the login's code is written to the CLI's stdin.
"""

import hashlib
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import threading
import time
from datetime import datetime
from pathlib import Path

RUNNER_DIR = Path(__file__).resolve().parent
ROOT = RUNNER_DIR.parent
STATE_DIR = Path(os.environ.get("RUNNER_DATA_DIR") or RUNNER_DIR) / "agent"
CLAUDE = os.environ.get("RUNNER_CLAUDE", "claude")
MODEL = os.environ.get("RUNNER_AGENT_MODEL", "").strip()
BUDGET = os.environ.get("RUNNER_AGENT_BUDGET_USD", "").strip()
WORK = Path(os.environ.get("RUNNER_AGENT_WORK") or (ROOT.parent / ".runner-agent"))
CODE_TOOLS = os.environ.get("RUNNER_AGENT_TOOLS",
                            "Read,Grep,Glob,Edit,Write,Bash(mvn *),Bash(python tools/*),Bash(git status*),Bash(git diff*)")
READ_TOOLS = "Read,Grep,Glob"
COPY_ITEMS = ["pom.xml", "src", "suites", "tools", "docs", "scripts", "AGENTS.md", "README.md", "START_HERE_AR.md",
              "config/credentials.example.properties", ".gitignore"]
NEVER_MERGE = re.compile(r"^(config/credentials\.local|runner/|target/|test-execution-reports/|\.git/|\.runner-input/)")
SECRET_ENV = re.compile(r"^(RUNNER_(DB_PASSWORD|SECRET_KEY|SPACES_SECRET|SPACES_KEY|GMAIL_APP_PASSWORD|PASSWORD|GITHUB_TOKEN)"
                        r"|ZULFI_(USER|PASSWORD)|ZAKAADASH_(EMAIL|PASSWORD)|ZULFI_OFFICIAL_FIXTURE)$")
MAX_TEXT = 60_000
MAX_FILE = 5_000_000
MAX_FILES = 10
MAX_EVENTS = 5000
TERMINAL = {"done", "failed", "stopped", "interrupted", "discarded"}
RULES = (
    "You work on a copy of a Java 21 + Selenium 4 + TestNG + Allure test automation framework (TAF) for two systems: "
    "Zulfi Portal (com.taf.systems.zulfi) and ZakaaDash (com.taf.systems.zakaadash). A QA engineer reviews your diff "
    "before anything is merged. Follow AGENTS.md exactly. Never add, print or commit credentials. Never weaken, "
    "remove or disable an assertion or a test to make it pass, and never fabricate results. Keep Page Object / Flow "
    "structure. Never enable Create Analysis or any submit gate. Answer in Arabic, formatted as Markdown.")

_lock = threading.RLock()
_requests = {}
_events = {}
_procs = {}
_hooks = []
_login = {"proc": None, "output": "", "method": None, "started": None, "url": None, "exit": None}
_cache = {"version": None, "auth": None, "authAt": 0.0}


class AgentError(Exception):
    pass


def now_iso():
    return datetime.now().isoformat(timespec="seconds")


def remove_tree(path):
    """rmtree that also removes read-only files (git objects on Windows)."""
    import stat

    def retry(function, target, *_):
        try:
            os.chmod(target, stat.S_IWRITE)
            function(target)
        except OSError:
            pass
    for attempt in range(5):  # Windows: a file can stay locked for a moment (indexer, antivirus)
        if not path or not Path(path).exists():
            return
        if sys.version_info >= (3, 12):
            shutil.rmtree(path, onexc=retry)
        else:
            shutil.rmtree(path, onerror=retry)
        if Path(path).exists():
            time.sleep(0.2 * (attempt + 1))


def on_finish(hook):
    """hook(request) is called when a request ends (orchestrator, auto-run)."""
    _hooks.append(hook)


# ---------------------------------------------------------------------------------------------- CLI discovery
def command():
    if CLAUDE.lower().endswith(".py") and Path(CLAUDE).is_file():
        return [sys.executable, CLAUDE]
    found = shutil.which(CLAUDE) or (CLAUDE if Path(CLAUDE).is_file() else None)
    return [found] if found else None


def _run_cli(args, timeout=30, stdin_text=None):
    base = command()
    if not base:
        raise AgentError("Claude Code is not installed (install it, or set RUNNER_CLAUDE)")
    return subprocess.run(base + args, input=stdin_text, capture_output=True, text=True, encoding="utf-8",
                          errors="replace", timeout=timeout, env=_child_env(), cwd=str(ROOT))


def _child_env():
    env = {k: v for k, v in os.environ.items() if not SECRET_ENV.match(k)}
    env.setdefault("CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC", "1")
    return env


def version():
    if _cache["version"] is None and command():
        try:
            out = _run_cli(["--version"], timeout=20)
            _cache["version"] = (out.stdout or out.stderr).strip().splitlines()[0] if (out.stdout or out.stderr) else ""
        except (AgentError, OSError, subprocess.TimeoutExpired, IndexError):
            _cache["version"] = ""
    return _cache["version"] or ""


def auth_status(force=False):
    if not force and _cache["auth"] is not None and time.monotonic() - _cache["authAt"] < 30:
        return _cache["auth"]
    if not command():
        result = {"installed": False, "loggedIn": False, "detail": "Claude Code is not installed"}
    elif os.environ.get("ANTHROPIC_API_KEY"):
        result = {"installed": True, "loggedIn": True, "method": "api-key", "detail": "ANTHROPIC_API_KEY is set"}
    else:
        try:
            out = _run_cli(["auth", "status"], timeout=20)
            detail = {}
            try:
                detail = json.loads(out.stdout or "{}")
            except ValueError:
                detail = {"text": (out.stdout or out.stderr).strip()[:500]}
            safe = {k: v for k, v in detail.items() if k in ("loggedIn", "authMethod", "method", "subscriptionType",
                                                               "email", "organization", "text", "apiProvider")}
            result = {"installed": True, "loggedIn": out.returncode == 0 and detail.get("loggedIn", True) is not False,
                      "detail": safe}
        except (AgentError, OSError, subprocess.TimeoutExpired) as error:
            result = {"installed": True, "loggedIn": False, "detail": f"{type(error).__name__}: {error}"}
    _cache.update(auth=result, authAt=time.monotonic())
    return result


def status():
    with _lock:
        login = {k: _login[k] for k in ("method", "started", "url", "exit")}
        login["active"] = bool(_login["proc"] and _login["proc"].poll() is None)
        login["output"] = _strip_ansi(_login["output"])[-1500:]
    return {"installed": bool(command()), "binary": (command() or [""])[-1], "version": version(), "model": MODEL,
            "budgetUsd": BUDGET, "work": str(WORK), "auth": auth_status(), "login": login}


_ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]|\x1b\][^\x07]*\x07")


def _strip_ansi(text):
    return _ANSI.sub("", text or "")


def auth_login(method="claudeai"):
    base = command()
    if not base:
        raise AgentError("Claude Code is not installed")
    with _lock:
        if _login["proc"] and _login["proc"].poll() is None:
            raise AgentError("A sign-in is already in progress")
        args = base + ["auth", "login"] + (["--console"] if method == "console" else [])
        proc = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                cwd=str(ROOT), env=_child_env(), **_process_flags())
        _login.update(proc=proc, output="", method=method, started=now_iso(), url=None, exit=None)

    def reader():
        while True:
            chunk = proc.stdout.read1(4096) if hasattr(proc.stdout, "read1") else proc.stdout.read(1)
            if not chunk:
                break
            with _lock:
                _login["output"] += chunk.decode("utf-8", errors="replace")
                match = re.search(r"https://\S+", _strip_ansi(_login["output"]))
                if match and not _login["url"]:
                    _login["url"] = match.group(0).rstrip(".,)")
        with _lock:
            _login["exit"] = proc.wait()
            _cache["authAt"] = 0.0

    threading.Thread(target=reader, daemon=True).start()
    for _ in range(40):
        if _login["url"] or proc.poll() is not None:
            break
        time.sleep(0.25)
    return status()["login"]


def auth_code(code):
    code = str(code or "").strip()
    if not code or len(code) > 2000 or any(c in code for c in "\r\n"):
        raise AgentError("Paste the code shown after signing in")
    with _lock:
        proc = _login["proc"]
        if not proc or proc.poll() is not None:
            raise AgentError("No sign-in is waiting for a code; start the sign-in again")
        proc.stdin.write((code + "\n").encode("utf-8"))
        proc.stdin.flush()
    for _ in range(80):
        if proc.poll() is not None:
            break
        time.sleep(0.25)
    _cache["authAt"] = 0.0
    return {"login": status()["login"], "auth": auth_status(force=True)}


def auth_cancel():
    with _lock:
        proc = _login["proc"]
    if proc and proc.poll() is None:
        _kill(proc)
    return status()["login"]


def auth_logout():
    out = _run_cli(["auth", "logout"], timeout=30)
    _cache["authAt"] = 0.0
    return {"ok": out.returncode == 0, "auth": auth_status(force=True)}


# ---------------------------------------------------------------------------------------------- persistence
def _folder(request_id):
    return STATE_DIR / request_id


def _save(request):
    folder = _folder(request["id"])
    folder.mkdir(parents=True, exist_ok=True)
    tmp = folder / "request.json.tmp"
    tmp.write_text(json.dumps(request, indent=1, ensure_ascii=False), encoding="utf-8")
    os.replace(tmp, folder / "request.json")


def load():
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    for path in sorted(STATE_DIR.glob("*/request.json")):
        try:
            request = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if request.get("status") not in TERMINAL:
            request.update(status="interrupted", finished=request.get("finished") or now_iso(),
                           error="The runner stopped while this request was running")
            _save(request)
        events = []
        events_file = path.parent / "events.jsonl"
        if events_file.is_file():
            for line in events_file.read_text(encoding="utf-8", errors="replace").splitlines()[-MAX_EVENTS:]:
                try:
                    events.append(json.loads(line))
                except ValueError:
                    pass
        with _lock:
            _requests[request["id"]] = request
            _events[request["id"]] = events


def _event(request, kind, text, **extra):
    with _lock:
        events = _events.setdefault(request["id"], [])
        seq = (events[-1]["seq"] + 1) if events else 1
        event = {"seq": seq, "t": now_iso(), "type": kind, "text": (text or "")[:20000], **extra}
        events.append(event)
        if len(events) > MAX_EVENTS:
            del events[: len(events) - MAX_EVENTS]
        request["events"] = seq
    folder = _folder(request["id"])
    folder.mkdir(parents=True, exist_ok=True)
    with open(folder / "events.jsonl", "a", encoding="utf-8") as handle:
        handle.write(json.dumps(event, ensure_ascii=False) + "\n")
    return event


def summary(request):
    keys = ("id", "system", "title", "mode", "status", "created", "finished", "autoRun", "origin", "costUsd",
            "turns", "merged", "undone", "discarded", "error", "events", "sessionId")
    value = {k: request.get(k) for k in keys}
    value["changed"] = len(request.get("changed") or [])
    return value


def list_requests():
    with _lock:
        return sorted((summary(r) for r in _requests.values()), key=lambda r: r["created"] or "", reverse=True)


def get(request_id):
    with _lock:
        request = _requests.get(request_id)
        return json.loads(json.dumps(request)) if request else None


def detail(request_id, after=0):
    request = get(request_id)
    if not request:
        raise AgentError("Unknown request")
    with _lock:
        events = [e for e in _events.get(request_id, []) if e["seq"] > after]
    return {**request, "eventsAfter": events, "worktreeExists": Path(request.get("worktree") or "").is_dir()}


# ---------------------------------------------------------------------------------------------- git copy
def _git(worktree, *args, check=True):
    completed = subprocess.run(["git", "-c", "user.name=runner", "-c", "user.email=runner@localhost",
                                "-c", "core.autocrlf=false", "-c", "core.quotepath=false", *args],
                               cwd=str(worktree), capture_output=True, text=True, encoding="utf-8", errors="replace")
    if check and completed.returncode != 0:
        raise AgentError(f"git {' '.join(args[:2])} failed: {completed.stderr.strip()[:400]}")
    return completed.stdout


def _ignore(directory, names):
    skip = {"__pycache__", "target", ".git", "node_modules"}
    return [n for n in names if n in skip or n.endswith(".pyc") or (Path(directory).name == "tools" and n.startswith("allure-"))]


def prepare_copy(request_id, files=()):
    if not shutil.which("git"):
        raise AgentError("git is required for Claude requests (to show and merge exact changes)")
    worktree = WORK / request_id
    if worktree.exists():
        remove_tree(worktree)
    worktree.mkdir(parents=True)
    for item in COPY_ITEMS:
        source = ROOT / item
        target = worktree / item
        if source.is_dir():
            shutil.copytree(source, target, ignore=_ignore)
        elif source.is_file():
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
    _git(worktree, "init", "-q")
    exclude = worktree / ".git" / "info" / "exclude"
    exclude.parent.mkdir(parents=True, exist_ok=True)
    exclude.write_text(".runner-input/\ntarget/\n__pycache__/\n*.pyc\n", encoding="utf-8")
    _git(worktree, "add", "-A")
    _git(worktree, "commit", "-q", "-m", "baseline", "--no-verify")
    _git(worktree, "tag", "baseline")
    if files:
        inbox = worktree / ".runner-input"
        inbox.mkdir()
        for item in files:
            (inbox / item["name"]).write_bytes(item["data"])
    return worktree


def changed_files(worktree):
    if not (Path(worktree) / ".git").is_dir():
        return []
    _git(worktree, "add", "-A")
    out = _git(worktree, "diff", "--cached", "--name-status", "--no-renames", "baseline")
    rows = []
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) >= 2:
            rows.append({"status": {"A": "added", "M": "modified", "D": "deleted"}.get(parts[0][0], parts[0]),
                         "path": parts[-1]})
    return rows


def diff(request_id):
    request = get(request_id)
    if not request:
        raise AgentError("Unknown request")
    worktree = Path(request.get("worktree") or "")
    if not (worktree / ".git").is_dir():
        return {"diff": "", "files": request.get("changed") or [], "available": False}
    _git(worktree, "add", "-A")
    text = _git(worktree, "diff", "--cached", "--no-renames", "--no-color", "baseline")
    return {"diff": text[:600_000], "truncated": len(text) > 600_000, "files": changed_files(worktree), "available": True}


# ---------------------------------------------------------------------------------------------- requests
def clean_files(files):
    """[{name, data (bytes or base64 text)}] -> [{name, data bytes, size}] with name and size limits."""
    import base64
    result = []
    for item in files or []:
        name = Path(str(item.get("name") or "")).name
        if not name or name.startswith("."):
            raise AgentError("Attached files need a name")
        data = item.get("data") or b""
        if not isinstance(data, bytes):
            try:
                data = base64.b64decode(data, validate=True)
            except ValueError:
                raise AgentError(f"{name}: file content must be base64")
        if len(data) > MAX_FILE:
            raise AgentError(f"{name}: larger than {MAX_FILE // 1_000_000} MB")
        result.append({"name": name, "data": data, "size": len(data)})
    if len(result) > MAX_FILES:
        raise AgentError(f"At most {MAX_FILES} files")
    return result


def build_prompt(system_title, text, files=(), context=""):
    parts = [f"# طلب أتمتة — {system_title}" if system_title else "# طلب أتمتة",
             "", "## المطلوب", text.strip()]
    if context:
        parts += ["", "## السياق", context.strip()]
    if files:
        parts += ["", "## الملفات المرفقة (في المجلد .runner-input/)"]
        parts += [f"- .runner-input/{f['name']} ({f['size']} bytes)" for f in files]
    parts += ["", "## القواعد", "- اتبع AGENTS.md ومعمارية TAF (Page Object + Flow).",
              "- لا تضعف أي تحقق (assertion) ولا تعطل أي اختبار، ولا تضع أي بيانات دخول.",
              "- اكتب الرد بالعربية وبتنسيق Markdown، واذكر الملفات التي غيرتها وسبب كل تغيير."]
    return "\n".join(parts)


def create(system, text, files=(), auto_run=False, mode="code", title=None, context="", origin=None,
           system_title="", prompt=None):
    if not command():
        raise AgentError("Claude Code is not installed on the runner machine (install it, or set RUNNER_CLAUDE)")
    text = str(text or "").strip()
    if not text and not prompt:
        raise AgentError("Write what Claude should do")
    if len(text) > MAX_TEXT:
        raise AgentError("Request text is too long")
    if mode not in ("code", "read"):
        raise AgentError("Unknown mode")
    attachments = clean_files(files)
    request_id = datetime.now().strftime("%Y%m%d-%H%M%S-") + secrets.token_hex(2)
    first_line = text.splitlines()[0] if text else "Request"
    request = {"id": request_id, "system": system, "title": (title or first_line)[:140],
               "text": text, "mode": mode, "autoRun": bool(auto_run), "origin": origin or {"type": "manual"},
               "status": "preparing", "created": now_iso(), "finished": None, "sessionId": None, "costUsd": None,
               "turns": None, "result": None, "changed": [], "merged": None, "undone": None, "discarded": None,
               "error": None, "events": 0, "worktree": str(WORK / request_id),
               "files": [{"name": f["name"], "size": f["size"]} for f in attachments]}
    with _lock:
        _requests[request_id] = request
        _events[request_id] = []
    _save(request)
    full_prompt = prompt or build_prompt(system_title, text, attachments, context)
    request["prompt"] = full_prompt[:200_000]

    def start():
        try:
            prepare_copy(request_id, attachments)
        except Exception as error:
            _finish(request, "failed", f"Could not prepare the project copy: {error}")
            return
        _invoke(request, full_prompt)

    threading.Thread(target=start, daemon=True).start()
    return summary(request)


def _process_flags():
    return {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP} if os.name == "nt" else {"start_new_session": True}


def _kill(proc):
    if proc.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"], capture_output=True)
    else:
        import signal
        try:
            os.killpg(proc.pid, signal.SIGTERM)
        except OSError:
            proc.terminate()


def _tool_text(name, data):
    data = data or {}
    for key in ("file_path", "path", "command", "pattern", "url", "description"):
        if data.get(key):
            return f"{name}: {data[key]}"[:400]
    return name


def _invoke(request, prompt, resume=None):
    args = ["-p", "--output-format", "stream-json", "--verbose"]
    if request["mode"] == "read":
        args += ["--permission-mode", "dontAsk", "--allowedTools", READ_TOOLS,
                 "--disallowedTools", "Edit,Write,NotebookEdit,Bash"]
    else:
        args += ["--permission-mode", "acceptEdits", "--allowedTools", CODE_TOOLS]
    if MODEL:
        args += ["--model", MODEL]
    if BUDGET:
        args += ["--max-budget-usd", BUDGET]
    args += ["--append-system-prompt", RULES]
    if resume:
        args += ["--resume", resume]
    request.update(status="running", finished=None, error=None)
    _save(request)
    _event(request, "user", prompt)
    try:
        proc = subprocess.Popen(command() + args, cwd=request["worktree"], env=_child_env(), stdin=subprocess.PIPE,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, **_process_flags())
    except OSError as error:
        _finish(request, "failed", f"Could not start Claude Code: {error}")
        return
    with _lock:
        _procs[request["id"]] = proc

    def feed():
        try:
            proc.stdin.write(prompt.encode("utf-8"))
            proc.stdin.close()
        except OSError:
            pass

    threading.Thread(target=feed, daemon=True).start()
    outcome = {"ok": False, "result": None}
    for raw in proc.stdout:
        line = raw.decode("utf-8", errors="replace").strip()
        if not line:
            continue
        try:
            message = json.loads(line)
        except ValueError:
            _event(request, "log", _strip_ansi(line))
            continue
        _handle(request, message, outcome)
    code = proc.wait()
    proc.stdout.close()
    with _lock:
        _procs.pop(request["id"], None)
    if request.get("status") == "stopping":
        _finish(request, "stopped", "Stopped by user")
    elif outcome["ok"]:
        _finish(request, "done", None)
    else:
        _finish(request, "failed", outcome.get("error") or f"Claude Code exited with code {code}")


def _handle(request, message, outcome):
    kind = message.get("type")
    if kind == "system" and message.get("subtype") == "init":
        request["sessionId"] = message.get("session_id") or request.get("sessionId")
        _event(request, "system", f"Session started · model {message.get('model', '?')}")
    elif kind == "assistant":
        for block in (message.get("message") or {}).get("content") or []:
            if block.get("type") == "text" and block.get("text", "").strip():
                _event(request, "text", block["text"])
            elif block.get("type") == "tool_use":
                _event(request, "tool", _tool_text(block.get("name", "tool"), block.get("input")))
    elif kind == "user":
        for block in (message.get("message") or {}).get("content") or []:
            if isinstance(block, dict) and block.get("type") == "tool_result" and block.get("is_error"):
                content = block.get("content")
                text = content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)
                _event(request, "tool-error", text[:600])
    elif kind == "result":
        request["sessionId"] = message.get("session_id") or request.get("sessionId")
        cost = message.get("total_cost_usd")
        if cost is not None:
            request["costUsd"] = round((request.get("costUsd") or 0) + float(cost), 4)
        request["turns"] = message.get("num_turns")
        result_text = message.get("result") or ""
        if result_text:
            request["result"] = result_text
            _event(request, "result", result_text)
        outcome["ok"] = message.get("subtype") == "success" and not message.get("is_error")
        if not outcome["ok"]:
            outcome["error"] = f"Claude Code ended: {message.get('subtype') or 'error'}"
    _save(request)


def _finish(request, status, error):
    worktree = Path(request.get("worktree") or "")
    try:
        request["changed"] = changed_files(worktree) if worktree.is_dir() else request.get("changed") or []
    except AgentError:
        pass
    if request.get("mode") == "read" and worktree.is_dir():
        remove_tree(worktree)  # before the status changes: a finished read-only request never leaves its copy behind
    _event(request, "end" if status == "done" else "error", error or f"Finished · {len(request['changed'])} changed file(s)")
    request.update(status=status, error=error, finished=now_iso())
    _save(request)
    for hook in list(_hooks):
        try:
            hook(get(request["id"]))
        except Exception as hook_error:  # a broken hook must not break the request
            print("agent hook error:", hook_error)


def reply(request_id, text):
    request = _requests.get(request_id)
    if not request:
        raise AgentError("Unknown request")
    text = str(text or "").strip()
    if not text or len(text) > MAX_TEXT:
        raise AgentError("Write a reply")
    if request["status"] in ("running", "preparing", "stopping"):
        raise AgentError("Claude is still working on this request")
    if not request.get("sessionId"):
        raise AgentError("This request has no Claude session to continue")
    if not Path(request["worktree"]).is_dir():
        raise AgentError("The project copy of this request was discarded")
    if request.get("merged"):
        raise AgentError("Already merged: start a new request for further changes")
    threading.Thread(target=_invoke, args=(request, text, request["sessionId"]), daemon=True).start()
    return summary(request)


def stop(request_id):
    with _lock:
        request = _requests.get(request_id)
        if not request:
            raise AgentError("Unknown request")
        proc = _procs.get(request_id)
        if request["status"] not in ("running", "preparing") or not proc:
            raise AgentError("Request is not running")
        request["status"] = "stopping"
    _kill(proc)
    return summary(request)


def _sha(path):
    return hashlib.sha1(Path(path).read_bytes()).hexdigest() if Path(path).is_file() else None


def _baseline_sha(worktree, relative):
    completed = subprocess.run(["git", "show", f"baseline:{relative}"], cwd=str(worktree), capture_output=True)
    return hashlib.sha1(completed.stdout).hexdigest() if completed.returncode == 0 else None


def _safe_target(relative):
    if NEVER_MERGE.match(relative) or ".." in Path(relative).parts:
        raise AgentError(f"'{relative}' is outside what a Claude change may touch")
    target = (ROOT / relative).resolve()
    if ROOT.resolve() not in target.parents:
        raise AgentError(f"'{relative}' is outside the project")
    return target


def merge(request_id, force=False):
    request = _requests.get(request_id)
    if not request:
        raise AgentError("Unknown request")
    if request["status"] in ("running", "preparing", "stopping"):
        raise AgentError("Wait until Claude has finished")
    if request.get("merged"):
        raise AgentError("Already merged")
    worktree = Path(request.get("worktree") or "")
    if not (worktree / ".git").is_dir():
        raise AgentError("The project copy of this request no longer exists")
    changes = changed_files(worktree)
    if not changes:
        raise AgentError("Claude did not change any file")
    conflicts, plan = [], []
    for change in changes:
        target = _safe_target(change["path"])
        base = _baseline_sha(worktree, change["path"])
        current = _sha(target)
        if current != base:
            conflicts.append(change["path"])
        plan.append((change, target))
    if conflicts and not force:
        return {"ok": False, "conflicts": conflicts}
    backup = _folder(request_id) / "backup"
    if backup.exists():
        remove_tree(backup)
    manifest = []
    for change, target in plan:
        relative = change["path"]
        existed = target.is_file()
        if existed:
            (backup / relative).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(target, backup / relative)
        manifest.append({"path": relative, "status": change["status"], "existed": existed,
                         "merged": _sha(worktree / relative)})
    for change, target in plan:
        if change["status"] == "deleted":
            if target.is_file():
                target.unlink()
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(worktree / change["path"], target)
    request.update(merged=now_iso(), undone=None, mergeManifest=manifest, changed=changes)
    _event(request, "system", f"Merged {len(manifest)} file(s) into the project" + (" (forced)" if conflicts else ""))
    _save(request)
    return {"ok": True, "files": [m["path"] for m in manifest], "forced": bool(conflicts)}


def undo_merge(request_id):
    request = _requests.get(request_id)
    if not request or not request.get("merged"):
        raise AgentError("Nothing to undo")
    backup = _folder(request_id) / "backup"
    changed_since = []
    for entry in request.get("mergeManifest") or []:
        target = _safe_target(entry["path"])
        if entry["status"] != "deleted" and _sha(target) != entry.get("merged"):
            changed_since.append(entry["path"])
        if entry["existed"]:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(backup / entry["path"], target)
        elif target.is_file():
            target.unlink()
    request.update(merged=None, undone=now_iso())
    _event(request, "system", "Merge undone: previous files restored"
           + (f" (also reverted later edits in {', '.join(changed_since)})" if changed_since else ""))
    _save(request)
    return {"ok": True, "changedSince": changed_since}


def discard(request_id):
    request = _requests.get(request_id)
    if not request:
        raise AgentError("Unknown request")
    if request["status"] in ("running", "preparing", "stopping"):
        raise AgentError("Stop the request first")
    remove_tree(request.get("worktree"))
    request.update(discarded=now_iso())
    if request["status"] == "done" and not request.get("merged"):
        request["status"] = "discarded"
    _event(request, "system", "Project copy discarded")
    _save(request)
    return summary(request)


def delete(request_id):
    with _lock:
        request = _requests.get(request_id)
        proc = _procs.get(request_id)
    if not request:
        raise AgentError("Unknown request")
    if proc:
        _kill(proc)
    remove_tree(request.get("worktree"))
    remove_tree(_folder(request_id))
    with _lock:
        _requests.pop(request_id, None)
        _events.pop(request_id, None)
    return {"deleted": request_id}


def wait(request_id, stop_event=None, poll=1.0):
    """Block until a request ends (used by the orchestrator); returns the request."""
    while True:
        request = get(request_id)
        if not request or request["status"] in TERMINAL:
            return request
        if stop_event is not None and stop_event.is_set():
            return request
        time.sleep(poll)
