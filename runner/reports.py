"""Report files of a run: local folder, optionally moved to DigitalOcean Spaces (S3-compatible, boto3).

Upload (after a run): every file under output/, report.html and console.log goes to
<prefix>/<run id>/<path> as a private object, is listed in the store (run_files) and is deleted locally.
The page reaches an uploaded file through a signed link (1 hour) or an HTTP redirect to one.
Results (run.json / MySQL rows) are never touched here.
"""

import mimetypes
import os
import threading
from datetime import datetime
from pathlib import Path

KEY = os.environ.get("RUNNER_SPACES_KEY", "")
SECRET = os.environ.get("RUNNER_SPACES_SECRET", "")
BUCKET = os.environ.get("RUNNER_SPACES_BUCKET", "")
REGION = os.environ.get("RUNNER_SPACES_REGION", "")
PREFIX = os.environ.get("RUNNER_SPACES_PREFIX", "automation-runner").strip("/")
ENDPOINT = os.environ.get("RUNNER_SPACES_ENDPOINT", "") or (f"https://{REGION}.digitaloceanspaces.com" if REGION else "")
LINK_SECONDS = 3600
KEEP_LOCAL = {"run.json", "files.json", "suite.xml", "java.args"}
CONTENT_TYPES = {".png": "image/png", ".jpg": "image/jpeg", ".html": "text/html; charset=utf-8",
                 ".log": "text/plain; charset=utf-8", ".txt": "text/plain; charset=utf-8",
                 ".md": "text/markdown; charset=utf-8", ".json": "application/json", ".xml": "text/xml; charset=utf-8",
                 ".mp4": "video/mp4", ".webm": "video/webm", ".csv": "text/csv; charset=utf-8"}

_lock = threading.Lock()
_client = {"value": None}


def content_type(path):
    suffix = Path(path).suffix.lower()
    return CONTENT_TYPES.get(suffix) or mimetypes.guess_type(str(path))[0] or "application/octet-stream"


def _boto_available():
    try:
        import boto3  # noqa: F401
        return True
    except ImportError:
        return False


def enabled():
    return bool(KEY and SECRET and BUCKET and ENDPOINT) and _boto_available()


def status():
    if not (KEY and SECRET and BUCKET and ENDPOINT):
        reason = "not configured (RUNNER_SPACES_KEY / SECRET / BUCKET / REGION)"
    elif not _boto_available():
        reason = "boto3 is not installed"
    else:
        reason = ""
    return {"enabled": not reason, "bucket": BUCKET, "region": REGION, "prefix": PREFIX,
            "endpoint": ENDPOINT, "reason": reason}


def client():
    with _lock:
        if _client["value"] is None:
            import boto3
            _client["value"] = boto3.session.Session().client(
                "s3", region_name=REGION or None, endpoint_url=ENDPOINT,
                aws_access_key_id=KEY, aws_secret_access_key=SECRET)
        return _client["value"]


def object_key(run_id, relative):
    return f"{PREFIX}/{run_id}/{relative}"


def report_files(run_dir):
    """Relative paths of the report files of a run folder (what upload sends)."""
    run_dir = Path(run_dir)
    files = []
    for name in ("report.html", "console.log"):
        if (run_dir / name).is_file():
            files.append(name)
    output = run_dir / "output"
    if output.is_dir():
        files += sorted(p.relative_to(run_dir).as_posix() for p in output.rglob("*") if p.is_file())
    return files


def local_size(run_dir):
    run_dir = Path(run_dir)
    if not run_dir.is_dir():
        return 0
    total = 0
    for path in run_dir.rglob("*"):
        try:
            if path.is_file():
                total += path.stat().st_size
        except OSError:
            pass
    return total


def upload(store, run_id, run_dir, log=print):
    """Upload a run's reports and delete them locally. Returns {"files", "size"}; raises on the first failure."""
    if not enabled():
        return {"files": 0, "size": 0}
    run_dir = Path(run_dir)
    entries = {e["path"]: e for e in store.load_run_files(run_id)}
    uploaded = []
    s3 = client()
    for relative in report_files(run_dir):
        path = run_dir / relative
        size = path.stat().st_size
        key = object_key(run_id, relative)
        kind = content_type(relative)
        s3.upload_file(str(path), BUCKET, key, ExtraArgs={"ACL": "private", "ContentType": kind})
        entries[relative] = {"path": relative, "key": key, "contentType": kind, "size": size,
                             "uploaded": datetime.now().isoformat(timespec="seconds")}
        uploaded.append(path)
    store.save_run_files(run_id, list(entries.values()))
    for path in uploaded:
        try:
            path.unlink()
        except OSError:
            pass
    output = run_dir / "output"
    if output.is_dir():
        for folder in sorted((p for p in output.rglob("*") if p.is_dir()), key=lambda p: len(p.parts), reverse=True):
            try:
                folder.rmdir()
            except OSError:
                pass
        try:
            output.rmdir()
        except OSError:
            pass
    return {"files": len(uploaded), "size": sum(e["size"] or 0 for e in entries.values())}


def find(store, run_id, relative):
    return next((e for e in store.load_run_files(run_id) if e["path"] == relative), None)


def link(store, run_id, relative, seconds=LINK_SECONDS):
    """Signed (private) link to an uploaded file, or None when it is not in Spaces."""
    if not enabled():
        return None
    entry = find(store, run_id, relative)
    if entry is None:
        return None
    return client().generate_presigned_url("get_object", Params={"Bucket": BUCKET, "Key": entry["key"]},
                                           ExpiresIn=seconds)


def remote_size(store, run_id):
    return sum(e.get("size") or 0 for e in store.load_run_files(run_id))


def delete(store, run_id):
    """Remove a run's uploaded objects and their index entries."""
    entries = store.load_run_files(run_id)
    if entries and enabled():
        s3 = client()
        for start in range(0, len(entries), 1000):
            chunk = entries[start:start + 1000]
            s3.delete_objects(Bucket=BUCKET, Delete={"Objects": [{"Key": e["key"]} for e in chunk], "Quiet": True})
    store.delete_run_files(run_id)
    return len(entries)
