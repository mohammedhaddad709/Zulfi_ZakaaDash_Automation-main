"""Regression (known-bug check) classification used by the generated suites and the runner.

A class is a regression check when:
  * it is listed in KNOWN for its system (reviewed, reproducible bug checks), or
  * one of its enabled test methods carries Allure @Issue("...") linking a reported defect.

The runner additionally treats classes that failed in retained run history as regression candidates
for its "E2E + regression" scope; that history is never used for the checked-in suites, so generated
files stay reproducible. Classification only orders/selects tests: it never changes pass/fail.
"""

from systems import fqcn

# Reviewed known-bug check classes per system (relative to systems.BASE). Empty until defects are triaged.
KNOWN = {
    "zulfi": [],
    "zakaadash": [],
}


def regression_classes(system, entries):
    known = {fqcn(name) for name in KNOWN.get(system, [])}
    result = []
    for entry in entries:
        if entry["class"] in known or any(m["issue"] and m["enabled"] for m in entry["methods"]):
            result.append(entry["class"])
    return result
