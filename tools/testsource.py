"""Static discovery of TestNG test classes and methods from Java sources (no compilation needed).

A test class is a non-abstract class in a registered system package that declares @Test methods
(or inherits them from an abstract parent in the same source tree). File names are not assumed:
Zulfi uses *Test.java, ZakaaDash also uses *Tests.java.
"""

import re

from systems import BASE, SYSTEMS, TEST_SOURCE_ROOT, system_for_class

_PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
_CLASS = re.compile(r"^\s*public\s+((?:abstract|final)\s+)*class\s+(\w+)(?:\s+extends\s+(\w+))?", re.M)
_METHOD = re.compile(r"\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:public|protected)?\s*(?:static\s+)?void\s+(\w+)\s*\(", re.S)
_TEST_ID = re.compile(r"\b[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+\b")
_CONSTANT_DECL = re.compile(r'static\s+final\s+String\s+(\w+)\s*=\s*"((?:[^"\\]|\\.)*)"\s*;')
_EXPR_TOKEN = re.compile(r'"((?:[^"\\]|\\.)*)"|([A-Za-z_][\w.]*)')


def _balanced(text, start):
    """Return the text inside parentheses starting at text[start] == '('."""
    depth = 0
    for index in range(start, len(text)):
        char = text[index]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:index], index + 1
    return text[start + 1:], len(text)


def _annotation_args(text, name):
    match = re.search(r"@" + name + r"\s*\(", text)
    if not match:
        return None
    args, _ = _balanced(text, match.end() - 1)
    return args


def _strip_comments(source):
    source = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), source, flags=re.S)
    return re.sub(r"//[^\n]*", "", source)


def _test_id(description, tms_link, method):
    if tms_link:
        return tms_link
    match = _TEST_ID.search(description or "")
    if match:
        return match.group(0)
    return method


def _evaluate(expression, constants):
    """Concatenate string literals and resolvable String constants of a description expression."""
    parts = []
    for literal, identifier in _EXPR_TOKEN.findall(expression):
        if identifier:
            key = identifier.split(".")[-1]
            parts.append(constants.get(identifier, constants.get(key, "")))
        else:
            parts.append(literal)
    return "".join(parts)


def _parse_methods(source, constants):
    methods = []
    for match in re.finditer(r"@Test\b", source):
        cursor = match.end()
        args = ""
        rest = source[cursor:]
        if rest.lstrip().startswith("("):
            open_at = cursor + (len(rest) - len(rest.lstrip()))
            args, cursor = _balanced(source, open_at)
        declaration = _METHOD.match(source, cursor)
        if not declaration:
            continue  # class-level @Test or unusual layout
        name = declaration.group(1)
        trailer = source[cursor:declaration.end()]
        description_expr = ""
        described = re.search(r"description\s*=\s*", args)
        if described:
            description_expr = args[described.end():]
            description_expr = re.split(r",\s*\w+\s*=", description_expr)[0]
        description = _evaluate(description_expr, constants).strip()
        tms = re.search(r'@TmsLink\(\s*"([^"]+)"', trailer)
        groups_match = re.search(r"groups\s*=\s*(\{[^}]*\}|[\w.\"]+)", args)
        groups = []
        if groups_match:
            groups = [g.strip().strip('"').split(".")[-1].lower().replace("_", "-")
                      for g in groups_match.group(1).strip("{}").split(",") if g.strip()]
        depends = re.search(r"dependsOnMethods\s*=\s*(\{[^}]*\}|\"[^\"]*\")", args)
        invocations = re.search(r"invocationCount\s*=\s*(\d+)", args)
        methods.append({
            "name": name,
            "id": _test_id(description, tms.group(1) if tms else None, name),
            "description": description.lstrip(" —:-") if description else "",
            "groups": groups,
            "enabled": not re.search(r"enabled\s*=\s*false", args),
            "issue": bool(re.search(r"@Issue\(", trailer)),
            "dependsOn": [d.strip().strip('"') for d in depends.group(1).strip("{}").split(",") if d.strip().strip('"')]
            if depends else [],
            "invocations": int(invocations.group(1)) if invocations else 1,
            "dataProvider": bool(re.search(r"dataProvider\s*=", args)),
        })
    return methods


def _read_sources():
    sources = {path: _strip_comments(path.read_text(encoding="utf-8")) for path in TEST_SOURCE_ROOT.rglob("*.java")}
    main_root = TEST_SOURCE_ROOT.parent.parent / "main" / "java"
    constant_sources = list(sources.values()) + [
        _strip_comments(path.read_text(encoding="utf-8")) for path in main_root.rglob("*.java")]
    constants = {}
    for source in constant_sources:
        declared = _CLASS.search(source)
        for name, value in _CONSTANT_DECL.findall(source):
            constants.setdefault(name, value)
            if declared:
                constants[declared.group(2) + "." + name] = value
    classes = {}
    for path, source in sources.items():
        package = _PACKAGE.search(source)
        declared = _CLASS.search(source)
        if not package or not declared:
            continue
        name = package.group(1) + "." + declared.group(2)
        classes[declared.group(2)] = {
            "class": name,
            "simple": declared.group(2),
            "abstract": "abstract" in (declared.group(1) or ""),
            "parent": declared.group(3),
            "methods": _parse_methods(source, constants),
            "path": path.relative_to(TEST_SOURCE_ROOT.parent.parent.parent).as_posix(),
        }
    return classes


def _inherited_methods(classes, entry, seen=None):
    seen = seen or set()
    methods = list(entry["methods"])
    parent = classes.get(entry["parent"]) if entry["parent"] else None
    if parent and parent["simple"] not in seen:
        seen.add(parent["simple"])
        known = {m["name"] for m in methods}
        methods += [m for m in _inherited_methods(classes, parent, seen) if m["name"] not in known]
    return methods


def discover():
    """{system_key: [class entries sorted by name]} for every registered system."""
    classes = _read_sources()
    found = {key: [] for key in SYSTEMS}
    for entry in classes.values():
        if entry["abstract"]:
            continue
        system = system_for_class(entry["class"])
        if system is None:
            continue
        methods = _inherited_methods(classes, entry)
        if not methods:
            continue
        found[system].append({**entry, "methods": methods, "module": module_of(system, entry["class"])})
    for entries in found.values():
        entries.sort(key=lambda e: e["class"])
    return found


def _entry(discovered, class_name):
    """Class entry by fully qualified or simple name from a discover() result."""
    for entries in discovered.values():
        for entry in entries:
            if class_name in (entry["class"], entry["simple"]):
                return entry
    return None


def class_tests(discovered, class_name, enabled_only=True):
    """Test method names of a class (inherited ones included)."""
    entry = _entry(discovered, class_name)
    return [m["name"] for m in (entry or {}).get("methods", []) if m["enabled"] or not enabled_only]


def class_groups(discovered, class_name):
    """{method: [groups]} of a class."""
    entry = _entry(discovered, class_name)
    return {m["name"]: list(m["groups"]) for m in (entry or {}).get("methods", [])}


def class_dependencies(discovered, class_name):
    """{method: [dependsOnMethods]} of a class (only methods that declare dependencies)."""
    entry = _entry(discovered, class_name)
    return {m["name"]: list(m["dependsOn"]) for m in (entry or {}).get("methods", []) if m.get("dependsOn")}


def class_invocations(discovered, class_name):
    """{method: invocationCount} of a class (data-provider rows cannot be known statically: counted once)."""
    entry = _entry(discovered, class_name)
    return {m["name"]: m.get("invocations", 1) for m in (entry or {}).get("methods", [])}


def source(class_name):
    """Java source text of a test class (fully qualified or simple name), or None."""
    simple = class_name.rsplit(".", 1)[-1]
    for path in TEST_SOURCE_ROOT.rglob(simple + ".java"):
        text = path.read_text(encoding="utf-8")
        if "." not in class_name or class_name.rsplit(".", 1)[0] in text:
            return text
    return None


def superclass(class_name):
    """Simple name of the declared parent class, or None."""
    text = source(class_name)
    declared = _CLASS.search(_strip_comments(text)) if text else None
    return declared.group(3) if declared else None


def with_dependencies(discovered, class_name, methods):
    """Methods plus their dependsOnMethods, transitively, in declaration order (TestNG needs them in the run)."""
    dependencies = class_dependencies(discovered, class_name)
    wanted, stack = set(), list(methods)
    while stack:
        method = stack.pop()
        if method in wanted:
            continue
        wanted.add(method)
        stack.extend(dependencies.get(method, []))
    order = class_tests(discovered, class_name, enabled_only=False)
    return [m for m in order if m in wanted] + sorted(m for m in wanted if m not in order)


def module_of(system, class_name):
    """Feature module: package below the system root, e.g. 'features.login' or 'tests.preprocessing.entry'."""
    for package in SYSTEMS[system]["packages"]:
        prefix = BASE + package + "."
        if class_name.startswith(prefix):
            parts = class_name[len(prefix):].split(".")[:-1]
            return ".".join(parts) or "root"
    return "root"
