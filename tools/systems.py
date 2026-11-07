"""System catalog shared by suite generation, test documentation and the embedded runner.

Each entry names the Java packages (relative to BASE) whose test classes belong to that system.
Exactly two systems are registered: Zulfi Portal and ZakaaDash.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = "com.taf.systems."
TEST_SOURCE_ROOT = ROOT / "src" / "test" / "java"
LEGACY_SUITE_ROOT = ROOT / "src" / "test" / "resources" / "suites"
GENERATED_SUITE_ROOT = ROOT / "suites"
CATALOG_DOC_ROOT = ROOT / "docs" / "test-catalog"

SYSTEMS = {
    "zulfi": {
        "title": "Zulfi Portal",
        "packages": ["zulfi"],
        # Fixture discovery seeds the manifest other analysis tests read, so this suite fixes the order.
        "order_from": ["zulfi/portal-full-parallel-suite.xml"],
        # Cross-page journeys (Home ↔ Match Schedule ↔ Standings ↔ Club page), read-only.
        "e2e": [
            "zulfi.features.clubdata.ZulfiDataConsistencyE2ETest",
        ],
        # Excluded from the generated whole suite (still selectable as single classes).
        # CLUB-INF retired 2026-10-22 (page moved into the club page; covered by CLUB-DET) — docs/testing/REBASELINE_2026-10-22.md
        "exclude": ["zulfi.features.clubinfo.ClubInformationAcceptanceTest"],
    },
    "zakaadash": {
        "title": "ZakaaDash",
        "packages": ["zakaadash"],
        "order_from": ["zakaadash/testng-regression.xml"],
        "e2e": [
            "zakaadash.tests.e2e.DataUploadJourneyE2ETest",
            "zakaadash.tests.e2e.AdvancedPreprocessingJourneyE2ETest",
            "zakaadash.tests.e2e.FootballPreprocessingJourneyE2ETest",
        ],
        # Framework demo against the-internet.herokuapp.com; not ZakaaDash product coverage.
        "exclude": ["zakaadash.tests.heroku.HerokuLoginTest"],
    },
}


def fqcn(relative_class):
    """'zakaadash.tests.e2e.X' -> 'com.taf.systems.zakaadash.tests.e2e.X'."""
    return BASE + relative_class


def system_for_class(class_name):
    """Return the system key owning a fully qualified class name, or None."""
    for key, system in SYSTEMS.items():
        for package in system["packages"]:
            if class_name.startswith(BASE + package + "."):
                return key
    return None
