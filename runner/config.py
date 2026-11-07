"""System configuration: URL fields, environments, login fields, parallel rules, test types and module map.

Seeds: runner/environments.json and runner/modules.json (read-only). Systems-page edits go to the store
(system_configs, system_modules, test_data); a stored entry replaces the seed of that system.
Secret login defaults are never stored (AGENTS.md: the runner never persists credentials).
"""

import copy
import json
import re
import threading
from pathlib import Path
from urllib.parse import urlparse

RUNNER_DIR = Path(__file__).resolve().parent
SEED_ENVIRONMENTS = RUNNER_DIR / "environments.json"
SEED_MODULES = RUNNER_DIR / "modules.json"
LOGIN_DATA = "login-data"

PROPERTY_KEY = re.compile(r"^[A-Za-z0-9_.\-]{1,120}$")
ENV_VAR = re.compile(r"^[A-Z][A-Z0-9_]{1,60}$")
ENV_NAME = re.compile(r"^[a-z0-9][a-z0-9_\-]{0,30}$")
GROUP = re.compile(r"^[a-z0-9][a-z0-9_\-]{0,40}$")
SIMPLE_CLASS = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,120}$")
RESERVED_KEYS = {"output.dir", "allure.results.directory", "reports.dir", "active.system", "build.dir"}
MODULE_STATUSES = ("covered", "partial", "none", "out")

_lock = threading.RLock()
_state = {"store": None, "systems": {}, "modules": {}, "seedSystems": {}, "seedModules": {}}


class ConfigError(ValueError):
    pass


def _read_seed(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    return data.get("systems", {})


def load(store, known_systems=None):
    """Load seeds, then apply stored edits. With an empty MySQL store the seeds are written once (first start)."""
    seed_systems = _read_seed(SEED_ENVIRONMENTS)
    seed_modules = _read_seed(SEED_MODULES)
    if known_systems:
        seed_systems = {k: v for k, v in seed_systems.items() if k in known_systems}
    stored = store.load_configs() if store else {}
    stored_modules = store.load_modules() if store else {}
    test_data = store.load_test_data() if store else {}
    if store is not None and store.kind == "mysql" and not stored:
        for key, value in seed_systems.items():
            store.save_config(key, _without_login_defaults(value))
            store.save_test_data(key, LOGIN_DATA, _login_defaults(value))
        for key, value in seed_modules.items():
            store.save_modules(key, value)
        stored, stored_modules, test_data = store.load_configs(), store.load_modules(), store.load_test_data()
    systems = {}
    for key, seed in seed_systems.items():
        spec = copy.deepcopy(stored.get(key) or seed)
        defaults = (test_data.get(key) or {}).get(LOGIN_DATA)
        for login in spec.get("logins", []):
            if login.get("secret"):
                login["default"] = ""
            elif defaults is not None and login["env"] in defaults:
                login["default"] = defaults[login["env"]]
        systems[key] = spec
    modules = {key: copy.deepcopy(stored_modules.get(key) or seed_modules.get(key) or []) for key in systems}
    with _lock:
        _state.update(store=store, systems=systems, modules=modules,
                      seedSystems=seed_systems, seedModules=seed_modules)


def systems():
    with _lock:
        return copy.deepcopy(_state["systems"])


def spec(system):
    with _lock:
        value = _state["systems"].get(system)
        return copy.deepcopy(value) if value is not None else None


def modules(system):
    with _lock:
        return copy.deepcopy(_state["modules"].get(system, []))


def default_env(system):
    value = spec(system) or {}
    return next(iter(value.get("envs", {})), None)


def is_customised(system):
    with _lock:
        return _state["systems"].get(system) != _state["seedSystems"].get(system) or \
            _state["modules"].get(system) != _state["seedModules"].get(system)


def _login_defaults(value):
    return {l["env"]: l.get("default", "") for l in value.get("logins", []) if not l.get("secret")}


def _without_login_defaults(value):
    clean = copy.deepcopy(value)
    for login in clean.get("logins", []):
        login["default"] = ""
    return clean


def _text(value, label, limit, required=True):
    text = str(value if value is not None else "").strip()
    if required and not text:
        raise ConfigError(f"{label} is required")
    if len(text) > limit or any(c in text for c in "\r\n\x00"):
        raise ConfigError(f"{label} is too long or contains line breaks")
    return text


def check_url(label, value, require_https=False):
    parsed = urlparse(value)
    if parsed.scheme not in ("http", "https") or not parsed.netloc:
        raise ConfigError(f"{label} must be an absolute http(s) URL")
    if require_https and parsed.scheme != "https":
        raise ConfigError(f"{label} must use HTTPS")
    if any(c in value for c in "\r\n\"'<> \x00"):
        raise ConfigError(f"{label} contains invalid characters")
    return value


def validate(system, payload, known_classes=()):
    """Validated copy of a system spec (+ module map) from the Systems page."""
    current = spec(system)
    if current is None:
        raise ConfigError("Unknown system")
    if not isinstance(payload, dict):
        raise ConfigError("Configuration must be an object")
    result = {"activeSystem": current["activeSystem"], "bugPrefix": current.get("bugPrefix", system[:3].upper())}
    if current.get("serialOnly"):
        result["serialOnly"] = True
    prefix = _text(payload.get("bugPrefix", result["bugPrefix"]), "Bug ID prefix", 10)
    if not re.match(r"^[A-Z][A-Z0-9]{1,9}$", prefix):
        raise ConfigError("Bug ID prefix must be 2-10 capital letters/digits")
    result["bugPrefix"] = prefix

    urls = payload.get("urls")
    if not isinstance(urls, list) or not 1 <= len(urls) <= 10:
        raise ConfigError("Define 1 to 10 URL fields")
    seen = set()
    result["urls"] = []
    for entry in urls:
        label = _text((entry or {}).get("label"), "URL label", 40)
        if label in seen:
            raise ConfigError(f"URL label '{label}' is used twice")
        seen.add(label)
        targets = [t.strip() for t in (entry.get("targets") or []) if str(t).strip()]
        if not targets or len(targets) > 5:
            raise ConfigError(f"{label}: give 1 to 5 -D property names")
        for target in targets:
            if not PROPERTY_KEY.match(target) or target in RESERVED_KEYS:
                raise ConfigError(f"{label}: property '{target}' is not allowed")
        result["urls"].append({"label": label, "targets": targets, "requireHttps": bool(entry.get("requireHttps"))})

    envs = payload.get("envs")
    if not isinstance(envs, dict) or not 1 <= len(envs) <= 10:
        raise ConfigError("Define 1 to 10 environments")
    result["envs"] = {}
    for name, values in envs.items():
        if not ENV_NAME.match(str(name)):
            raise ConfigError(f"Environment name '{name}' must be lower-case letters, digits, - or _")
        values = values or {}
        row = {}
        for url in result["urls"]:
            value = str(values.get(url["label"], "") or "").strip()
            if value:
                check_url(f"{name} / {url['label']}", value, url["requireHttps"])
            row[url["label"]] = value
        result["envs"][name] = row

    logins = payload.get("logins") or []
    if not isinstance(logins, list) or len(logins) > 10:
        raise ConfigError("Define at most 10 login fields")
    result["logins"] = []
    names = set()
    for entry in logins:
        label = _text((entry or {}).get("label"), "Login label", 40)
        env = _text(entry.get("env"), f"{label}: environment variable", 61)
        if not ENV_VAR.match(env) or env in names:
            raise ConfigError(f"{label}: '{env}' must be a unique UPPER_CASE environment variable name")
        names.add(env)
        secret = bool(entry.get("secret"))
        default = "" if secret else _text(entry.get("default", ""), f"{label} default", 200, required=False)
        result["logins"].append({"label": label, "env": env, "default": default, **({"secret": True} if secret else {})})

    result["parallelNote"] = _text(payload.get("parallelNote", ""), "Parallel note", 200, required=False)
    try:
        threads = int(payload.get("maxThreads", 1))
    except (TypeError, ValueError):
        raise ConfigError("Max parallel classes must be a number")
    if not 1 <= threads <= 4:
        raise ConfigError("Max parallel classes must be 1-4")
    if current.get("serialOnly") and threads != 1:
        raise ConfigError("This system must run serially (1 class at a time)")
    result["maxThreads"] = threads

    result["serialGroups"] = []
    for group in payload.get("serialGroups") or []:
        name = _text((group or {}).get("name"), "Serial group name", 80)
        match = [_text(m, f"{name}: match", 160) for m in (group.get("match") or []) if str(m).strip()]
        if not match or len(match) > 10:
            raise ConfigError(f"{name}: give 1 to 10 class-name fragments")
        result["serialGroups"].append({"name": name, "match": match})

    result["testTypes"] = []
    for test_type in payload.get("testTypes") or []:
        group = _text((test_type or {}).get("group"), "Test type group", 41)
        if not GROUP.match(group):
            raise ConfigError(f"Test type group '{group}' must be lower-case")
        result["testTypes"].append({"group": group, "label": _text(test_type.get("label") or group, "Test type label", 60)})

    module_rows = payload.get("modules")
    module_result = None
    if module_rows is not None:
        if not isinstance(module_rows, list) or len(module_rows) > 200:
            raise ConfigError("Module map must be a list (max 200)")
        module_result = []
        known = set(known_classes)
        for row in module_rows:
            name = _text((row or {}).get("name"), "Module name", 80)
            status = row.get("status", "none")
            if status not in MODULE_STATUSES:
                raise ConfigError(f"{name}: status must be one of {', '.join(MODULE_STATUSES)}")
            classes = [str(c).strip() for c in (row.get("classes") or []) if str(c).strip()]
            for cls in classes:
                if not SIMPLE_CLASS.match(cls) or (known and cls not in known):
                    raise ConfigError(f"{name}: unknown test class '{cls}'")
            entry = {"name": name, "status": status, "classes": classes}
            note = _text(row.get("note", ""), f"{name} note", 300, required=False)
            if note:
                entry["note"] = note
            module_result.append(entry)
    return result, module_result


def save(system, payload, known_classes=()):
    value, module_rows = validate(system, payload, known_classes)
    with _lock:
        store = _state["store"]
        if store is not None:
            store.save_config(system, _without_login_defaults(value))
            store.save_test_data(system, LOGIN_DATA, _login_defaults(value))
            if module_rows is not None:
                store.save_modules(system, module_rows)
        _state["systems"][system] = value
        if module_rows is not None:
            _state["modules"][system] = module_rows
    return value


def reset(system):
    """Back to the seed files for one system (stored edits are replaced by the seed)."""
    with _lock:
        seed = copy.deepcopy(_state["seedSystems"].get(system))
        if seed is None:
            raise ConfigError("Unknown system")
        seed_modules = copy.deepcopy(_state["seedModules"].get(system, []))
        store = _state["store"]
        if store is not None:
            store.save_config(system, _without_login_defaults(seed))
            store.save_test_data(system, LOGIN_DATA, _login_defaults(seed))
            store.save_modules(system, seed_modules)
        _state["systems"][system] = seed
        _state["modules"][system] = seed_modules
    return seed
