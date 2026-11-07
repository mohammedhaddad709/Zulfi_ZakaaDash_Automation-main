"""Defect identity and bug-report export (Word .docx / Excel .xlsx).

* signature(test): the test's fullName + a hash of its normalised failure message. The same check failing in
  different runs gets the same signature, hence the same defect ID in the registry.
* suggestion(...): one failed test as an editable bug (title, severity, priority, status, steps, expected, actual,
  screenshot), using the project's bug-report layout (BugReportWriter: feature, test case ID, classification,
  expected, actual, URL, exact error).
* write_docx / write_xlsx: python-docx / openpyxl (optional packages; a clear error when missing).
"""

import hashlib
import io
import re
from datetime import datetime

SEVERITIES = ("Critical", "High", "Medium", "Low")
PRIORITY_OF = {"Critical": "P1", "High": "P2", "Medium": "P3", "Low": "P4"}
STATUSES = ("New", "Reopened", "Open", "Fixed", "Not a bug")


class ExportError(Exception):
    pass


def normalise(message):
    """Stable form of a failure message: first line, lower case, numbers/ids/times masked, spaces collapsed."""
    text = (message or "").strip().splitlines()[0] if (message or "").strip() else ""
    text = text.lower()
    text = re.sub(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "<uuid>", text)
    text = re.sub(r"\d+", "#", text)
    text = re.sub(r"\s+", " ", text)
    return text[:300]


def signature(test):
    full_name = test.get("fullName") or test.get("name") or ""
    digest = hashlib.sha256(normalise(test.get("message")).encode("utf-8")).hexdigest()[:16]
    return f"{full_name}#{digest}"


_ZULFI_STEP = re.compile(r"Step failed:\s*(?P<step>.*?)\s+—\s+expected:\s*(?P<expected>.*?)\s+—\s+actual:\s*(?P<actual>.*)", re.S)
_TESTNG = re.compile(r"expected \[(?P<expected>.*?)\] but found \[(?P<actual>.*?)\]", re.S)
_LABELLED = re.compile(r"expected[:=]\s*(?P<expected>.+?)[\s,;|]+(?:but\s+)?(?:actual|was|got|found)[:=]?\s*(?P<actual>.+)", re.I | re.S)


def expected_actual(message, log_lines=()):
    """(expected, actual) from a failure message; falls back to 'Expected:'/'Actual:' lines in the console log."""
    message = (message or "").strip()
    for pattern in (_ZULFI_STEP, _LABELLED):
        match = pattern.search(message)
        if match:
            actual = match.group("actual").strip()
            actual = re.sub(r"\s*expected \[.*?\] but found \[.*?\]\s*$", "", actual, flags=re.S).strip()
            return match.group("expected").strip()[:1000], (actual or message)[:1500]
    match = _TESTNG.search(message)
    if match:
        return match.group("expected").strip()[:1000], match.group("actual").strip()[:1500]
    expected = next((l.split(":", 1)[1].strip() for l in log_lines if re.match(r"\s*expected\s*:", l, re.I)), "")
    actual = next((l.split(":", 1)[1].strip() for l in log_lines if re.match(r"\s*actual\s*:", l, re.I)), "")
    return (expected or "The check passes")[:1000], (actual or message or "The check failed")[:1500]


def default_severity(test, kind):
    if test.get("soft"):
        return "Medium"
    return {"environment": "High", "assertion": "Medium", "data": "Medium", "locator": "Low", "timeout": "Low"}.get(kind, "Medium")


def suggestion(run, test, *, kind, steps, log_lines, registry_entry, seen_before):
    """One failed test as an editable bug for the report modal."""
    expected, actual = expected_actual(test.get("message"), log_lines)
    entry = registry_entry or {}
    severity = entry.get("severity") or default_severity(test, kind)
    title = entry.get("note") or test.get("title") or test.get("name") or ""
    test_id = test.get("testId") or ""
    return {
        "test": test.get("fullName"),
        "name": test.get("name"),
        "include": not entry.get("notBug"),
        "id": entry.get("id") or "",
        "title": (f"{test_id} — {title}" if test_id and not title.startswith(test_id) else title)[:300],
        "severity": severity,
        "priority": PRIORITY_OF.get(severity, "P3"),
        "status": "Reopened" if seen_before else "New",
        "feature": test.get("feature") or "",
        "testId": test_id,
        "classification": test.get("category") or kind,
        "steps": [s for s in steps if s][:30] or [f"Run {test.get('name')}"],
        "expected": expected,
        "actual": actual,
        "error": (test.get("message") or "")[:2000],
        "shot": test.get("shot"),
        "softCheck": bool(test.get("soft")),
        "note": entry.get("note") or "",
    }


def clean_bug(raw):
    """Validated copy of a bug edited in the modal."""
    bug = dict(raw or {})
    severity = bug.get("severity") if bug.get("severity") in SEVERITIES else "Medium"
    steps = bug.get("steps")
    if isinstance(steps, str):
        steps = [s.strip() for s in steps.splitlines() if s.strip()]
    return {
        "test": str(bug.get("test") or "")[:500],
        "id": str(bug.get("id") or "")[:40],
        "title": str(bug.get("title") or bug.get("test") or "Bug")[:300],
        "severity": severity,
        "priority": str(bug.get("priority") or PRIORITY_OF[severity])[:10],
        "status": str(bug.get("status") or "New")[:20],
        "feature": str(bug.get("feature") or "")[:200],
        "testId": str(bug.get("testId") or "")[:60],
        "classification": str(bug.get("classification") or "")[:60],
        "steps": [str(s)[:500] for s in (steps or [])][:40],
        "expected": str(bug.get("expected") or "")[:3000],
        "actual": str(bug.get("actual") or "")[:3000],
        "error": str(bug.get("error") or "")[:4000],
        "shot": bug.get("shot") if isinstance(bug.get("shot"), str) else None,
    }


def _environment_rows(meta):
    rows = [("System", meta.get("systemTitle", "")), ("Environment", meta.get("envName", "")),
            ("Run", meta.get("runId", "")), ("Executed", meta.get("finished") or meta.get("started") or ""),
            ("Browser mode", meta.get("execution", "")), ("Result", meta.get("status", ""))]
    rows += [(label, url) for label, url in (meta.get("urls") or {}).items()]
    counts = meta.get("counts") or {}
    if counts:
        rows.append(("Tests", ", ".join(f"{k} {v}" for k, v in counts.items() if v)))
    return rows


def write_docx(meta, bugs, load_image=None):
    """Word bug report as bytes. load_image(bug) -> png bytes or None."""
    try:
        from docx import Document
        from docx.enum.text import WD_ALIGN_PARAGRAPH
        from docx.shared import Inches, Pt, RGBColor
    except ImportError as error:
        raise ExportError("Word export needs python-docx (pip install -r runner/requirements.txt)") from error
    document = Document()
    style = document.styles["Normal"]
    style.font.name = "Segoe UI"
    style.font.size = Pt(10)

    title = document.add_heading(f"Bug Report — {meta.get('systemTitle', '')}", 0)
    title.alignment = WD_ALIGN_PARAGRAPH.LEFT
    document.add_paragraph(f"{len(bugs)} defect(s) · generated {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    document.add_heading("Environment", level=1)
    table = document.add_table(rows=0, cols=2)
    table.style = "Light Grid Accent 1"
    for key, value in _environment_rows(meta):
        cells = table.add_row().cells
        cells[0].text, cells[1].text = key, str(value)

    document.add_heading("Summary", level=1)
    summary = document.add_table(rows=1, cols=4)
    summary.style = "Light Grid Accent 1"
    for cell, text in zip(summary.rows[0].cells, ("ID", "Title", "Severity", "Status")):
        cell.text = text
    for bug in bugs:
        cells = summary.add_row().cells
        cells[0].text, cells[1].text, cells[2].text, cells[3].text = bug["id"] or "—", bug["title"], bug["severity"], bug["status"]

    colours = {"Critical": RGBColor(0xB0, 0x1E, 0x2B), "High": RGBColor(0xC2, 0x5B, 0x00),
               "Medium": RGBColor(0x8A, 0x6D, 0x00), "Low": RGBColor(0x2E, 0x6B, 0x3A)}
    for index, bug in enumerate(bugs, 1):
        document.add_page_break()
        document.add_heading(f"{bug['id'] or 'BUG-%02d' % index} — {bug['title']}", level=1)
        executive = document.add_paragraph()
        executive.add_run("Executive summary: ").bold = True
        executive.add_run(bug["actual"][:400])
        fields = document.add_table(rows=0, cols=2)
        fields.style = "Light Grid Accent 1"
        for key, value in (("Severity", bug["severity"]), ("Priority", bug["priority"]), ("Status", bug["status"]),
                           ("Feature", bug["feature"]), ("Test Case ID", bug["testId"]),
                           ("Classification", bug["classification"]), ("Automated test", bug["test"])):
            cells = fields.add_row().cells
            cells[0].text, cells[1].text = key, value or "—"
            if key == "Severity" and value in colours:
                for run in cells[1].paragraphs[0].runs:
                    run.font.color.rgb = colours[value]
                    run.bold = True
        document.add_heading("Steps to reproduce", level=2)
        for step in bug["steps"]:
            document.add_paragraph(step, style="List Number")
        document.add_heading("Expected result", level=2)
        document.add_paragraph(bug["expected"] or "—")
        document.add_heading("Actual result", level=2)
        document.add_paragraph(bug["actual"] or "—")
        if bug.get("error"):
            document.add_heading("Exact error", level=2)
            error = document.add_paragraph()
            run = error.add_run(bug["error"][:1500])
            run.font.name = "Consolas"
            run.font.size = Pt(8.5)
        image = load_image(bug) if load_image else None
        if image:
            document.add_heading("Screenshot", level=2)
            try:
                document.add_picture(io.BytesIO(image), width=Inches(6.2))
            except Exception:
                document.add_paragraph("(screenshot could not be embedded)")
    output = io.BytesIO()
    document.save(output)
    return output.getvalue()


def write_xlsx(meta, bugs, load_image=None):
    """Excel bug report as bytes: one row per bug, screenshot column."""
    try:
        from openpyxl import Workbook
        from openpyxl.styles import Alignment, Font, PatternFill
        from openpyxl.utils import get_column_letter
    except ImportError as error:
        raise ExportError("Excel export needs openpyxl (pip install -r runner/requirements.txt)") from error
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "Bugs"
    headers = ["ID", "Title", "Severity", "Priority", "Status", "Feature", "Test Case ID", "Classification",
               "Steps to reproduce", "Expected result", "Actual result", "Automated test", "Run", "Environment", "Screenshot"]
    widths = [12, 48, 11, 9, 11, 26, 16, 18, 48, 40, 48, 50, 22, 14, 46]
    sheet.append(headers)
    fill = PatternFill("solid", fgColor="1F3A8A")
    for column, width in enumerate(widths, 1):
        cell = sheet.cell(row=1, column=column)
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = fill
        cell.alignment = Alignment(vertical="center", wrap_text=True)
        sheet.column_dimensions[get_column_letter(column)].width = width
    sheet.freeze_panes = "A2"
    severity_fill = {"Critical": "F8D7DA", "High": "FDE2C8", "Medium": "FFF3CD", "Low": "D1E7DD"}
    image_support = True
    try:
        from openpyxl.drawing.image import Image as SheetImage
    except ImportError:
        image_support = False
    for row, bug in enumerate(bugs, 2):
        sheet.append([bug["id"], bug["title"], bug["severity"], bug["priority"], bug["status"], bug["feature"],
                      bug["testId"], bug["classification"],
                      "\n".join(f"{i}. {s}" for i, s in enumerate(bug["steps"], 1)), bug["expected"], bug["actual"],
                      bug["test"], meta.get("runId", ""), meta.get("envName", ""), ""])
        for column in range(1, len(headers) + 1):
            sheet.cell(row=row, column=column).alignment = Alignment(vertical="top", wrap_text=True)
        if bug["severity"] in severity_fill:
            sheet.cell(row=row, column=3).fill = PatternFill("solid", fgColor=severity_fill[bug["severity"]])
        image = load_image(bug) if load_image else None
        sheet.row_dimensions[row].height = 150 if image else 90
        if image and image_support:
            try:
                picture = SheetImage(io.BytesIO(image))
                ratio = min(330 / max(picture.width, 1), 190 / max(picture.height, 1))
                picture.width, picture.height = int(picture.width * ratio), int(picture.height * ratio)
                sheet.add_image(picture, f"O{row}")
            except Exception:
                sheet.cell(row=row, column=15).value = bug.get("shot") or ""
        elif bug.get("shot"):
            sheet.cell(row=row, column=15).value = bug["shot"]
    info = workbook.create_sheet("Environment")
    for key, value in _environment_rows(meta):
        info.append([key, str(value)])
    info.column_dimensions["A"].width = 18
    info.column_dimensions["B"].width = 80
    output = io.BytesIO()
    workbook.save(output)
    return output.getvalue()


def markdown_docx(title, markdown_text):
    """Small Markdown -> Word conversion for orchestrator step reports (headings, lists, tables, code, paragraphs)."""
    try:
        from docx import Document
        from docx.shared import Pt
    except ImportError as error:
        raise ExportError("Word export needs python-docx (pip install -r runner/requirements.txt)") from error
    document = Document()
    document.styles["Normal"].font.name = "Segoe UI"
    document.add_heading(title, 0)
    lines = (markdown_text or "").splitlines()
    index, in_code, code = 0, False, []
    while index < len(lines):
        line = lines[index]
        if line.strip().startswith("```"):
            if in_code:
                paragraph = document.add_paragraph()
                run = paragraph.add_run("\n".join(code))
                run.font.name, run.font.size = "Consolas", Pt(9)
                code = []
            in_code = not in_code
            index += 1
            continue
        if in_code:
            code.append(line)
            index += 1
            continue
        stripped = line.strip()
        if stripped.startswith("|") and index + 1 < len(lines) and re.match(r"^\s*\|?\s*:?-{2,}", lines[index + 1]):
            header = [c.strip() for c in stripped.strip("|").split("|")]
            rows = []
            index += 2
            while index < len(lines) and lines[index].strip().startswith("|"):
                rows.append([c.strip() for c in lines[index].strip().strip("|").split("|")])
                index += 1
            table = document.add_table(rows=1, cols=len(header))
            table.style = "Light Grid Accent 1"
            for cell, text in zip(table.rows[0].cells, header):
                cell.text = _plain(text)
            for row in rows:
                cells = table.add_row().cells
                for cell, text in zip(cells, row + [""] * (len(header) - len(row))):
                    cell.text = _plain(text)
            continue
        heading = re.match(r"^(#{1,4})\s+(.*)", stripped)
        if heading:
            document.add_heading(_plain(heading.group(2)), level=min(len(heading.group(1)), 4))
        elif re.match(r"^[-*]\s+", stripped):
            document.add_paragraph(_plain(re.sub(r"^[-*]\s+", "", stripped)), style="List Bullet")
        elif re.match(r"^\d+[.)]\s+", stripped):
            document.add_paragraph(_plain(re.sub(r"^\d+[.)]\s+", "", stripped)), style="List Number")
        elif stripped:
            document.add_paragraph(_plain(stripped))
        index += 1
    output = io.BytesIO()
    document.save(output)
    return output.getvalue()


def _plain(text):
    return re.sub(r"(\*\*|__|`)", "", text or "")
