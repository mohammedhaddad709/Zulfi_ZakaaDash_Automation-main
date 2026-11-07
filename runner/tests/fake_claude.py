"""Stand-in for the Claude Code CLI in the runner tests (stream-json protocol subset).

* --version / auth status            print fixed answers
* -p (prompt on stdin)               emits init, an assistant text + tool_use, then a result; in code mode it edits
                                     src/test/java/com/demo/DemoTest.java and adds NewFlowTest.java in the cwd copy
* a prompt containing FAIL_PLEASE     ends with an error result
Every call is appended to the JSON-lines file named by FAKE_CLAUDE_LOG (args, whether secrets were visible).
"""

import json
import os
import sys
import uuid
from pathlib import Path


def emit(value):
    sys.stdout.write(json.dumps(value, ensure_ascii=False) + "\n")
    sys.stdout.flush()


def main():
    args = sys.argv[1:]
    if args[:1] == ["--version"]:
        print("9.9.9 (Claude Code fake)")
        return 0
    if args[:2] == ["auth", "status"]:
        print(json.dumps({"loggedIn": True, "authMethod": "fake"}))
        return 0
    prompt = sys.stdin.read()
    log = os.environ.get("FAKE_CLAUDE_LOG")
    if log:
        with open(log, "a", encoding="utf-8") as handle:
            handle.write(json.dumps({"args": args, "cwd": os.getcwd(), "prompt": prompt[:400],
                                     "secretVisible": any(k in os.environ for k in ("ZULFI_PASSWORD", "RUNNER_SECRET_KEY"))}) + "\n")
    session = args[args.index("--resume") + 1] if "--resume" in args else str(uuid.uuid4())
    emit({"type": "system", "subtype": "init", "session_id": session, "model": "fake-model"})
    emit({"type": "assistant", "message": {"content": [
        {"type": "text", "text": "أحلل الطلب"},
        {"type": "tool_use", "name": "Edit", "input": {"file_path": "src/test/java/com/demo/DemoTest.java"}}]}})
    failing = "FAIL_PLEASE" in prompt or os.environ.get("FAKE_CLAUDE_FAIL") == "1"
    if "acceptEdits" in args and not failing:
        root = Path.cwd() / "src" / "test" / "java" / "com" / "demo"
        root.mkdir(parents=True, exist_ok=True)
        demo = root / "DemoTest.java"
        demo.write_text((demo.read_text(encoding="utf-8") if demo.is_file() else "") + "// edited by fake claude\n",
                        encoding="utf-8")
        (root / "NewFlowTest.java").write_text("class NewFlowTest {}\n", encoding="utf-8")
    emit({"type": "result", "subtype": "error_during_execution" if failing else "success", "is_error": failing,
          "result": "## النتيجة\n\nتم التعديل" if not failing else "", "total_cost_usd": 0.0125, "num_turns": 3,
          "session_id": session})
    return 1 if failing else 0


if __name__ == "__main__":
    sys.exit(main())
