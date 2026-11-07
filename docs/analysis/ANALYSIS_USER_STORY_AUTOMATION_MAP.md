# Analysis — User Story → Automation Map

Closure checklist for Create Analysis automation (Home + Schedule entry).

| User Story | Test ID prefix | Suite(s) | Default safe? |
|------------|----------------|----------|-------------|
| US-FAP-ANL-MATCH-TYPE | ANL-TYPE-001…013 | `match-type-official-suite.xml`, `match-type-official-b-suite.xml` | Yes (phases a–d) |
| US-FAP-ANL-FRIENDLY | ANL-FRD-001…014 | `friendly-match-analysis-suite.xml` | Yes (phases a–d) |
| US-FAP-MCH-ANALYSIS | MCH-ANL-001…016 | `match-schedule-analysis-suite.xml` | Yes (phase none) |
| Official HP / selection | ANL-OFF-HP-001, ANL-OFF-DIS/SEL | `analysis-official-hp-suite.xml`, fixture selection suites | DIS/SEL yes; HP gated |

**Principles:** phased gates, no submit without explicit properties, one submit per test, POM + Flow + registry per feature, reuse TAF reporting (`TestStepReporter`, Allure, defect classification).
