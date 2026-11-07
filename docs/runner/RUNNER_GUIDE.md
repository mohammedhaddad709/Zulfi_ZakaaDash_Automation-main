# Automation Control Center — operator guide

The embedded runner of this TAF: runs the Zulfi and ZakaaDash TestNG suites, keeps results and defects, schedules
daily runs, works with Claude Code and shows release readiness. Built from the NAWAH runner specification
(`RUNNER_REBUILD_PLAN.md` lists every adaptation and owner decision).

## Start

```powershell
python -m pip install -r runner/requirements.txt     # optional packages; the runner starts without them
python runner/server.py --port 8080                   # http://localhost:8080
python runner/server.py --stop                        # stop it (its active runs end as "interrupted")
```

Requirements: Python 3.10+, Java 21, Maven, Google Chrome. Settings come from environment variables or the git-ignored
`runner/.env` (template: `runner/.env.example`); a variable already in the environment wins.

## Layout

| Path | What |
|------|------|
| `runner/server.py` | HTTP server, run lifecycle, snapshots, catalogue, dashboard, quality metrics, API |
| `runner/store.py` | `FileStore` (JSON, default) or `MySqlStore` (`RUNNER_DB_HOST`) |
| `runner/config.py` | System specs (seeds `environments.json`, `modules.json`; Systems-page edits in the store) |
| `runner/schedules.py` | Daily schedules + Gmail notifications |
| `runner/reliability.py` | Failure kinds, flaky tests, quarantine, env-down retry settings, cut-off runs |
| `runner/reports.py` | Report files: local or DigitalOcean Spaces (upload, signed links, clean-up) |
| `runner/vault.py` | AES-256-GCM `enc:v1:` values (Java: `SecretVault`) |
| `runner/bugreport.py` | Defect signature, bug suggestions, Word / Excel reports |
| `runner/agent.py` | Claude Code requests in a project copy, diff / merge / undo, sign-in |
| `runner/orchestrator.py` | QA jobs: sprint, reported bug, regression, defects retest |
| `runner/archive.py` | Read-only index of Maven/CLI report history (`test-execution-reports/history`) |
| `runner/static/index.html` | The whole UI (no build step) |
| `RUNNER_DATA_DIR` (default `runner/`) | `runs/<id>/`, `bugs.json`, `schedules.json`, `config-store.json`, `quarantine.json`, `uptime.jsonl`, `agent/`, `jobs/` |
| `runner/.build/<id>` | Compiled code snapshots (last 3 kept, plus any used by an active run) |

## Pages

* **Runs** — new run form, runs list (select to delete runs or only their reports), run detail with KPIs, failures
  (screenshot, kind, severity + stakeholder sentence, retest, quarantine, not a bug), setup failures, tests (CSV),
  live console, preflight, details; Allure report, console log, bug report (Word / Excel), Ask Claude, Heal.
* **Dashboard** (`#dashboard`, `#reliability`) — per-system release status, pass-rate trend, open defects (retest one /
  all), fixed in the period, environment availability, module map, most failing / slowest tests, reliability, report
  history of Maven runs, storage clean-up.
* **Schedules** — daily runs (time + weekdays), run now, enable/disable, test e-mail.
* **Claude** — Claude Code status and sign-in, new request (code or read-only, files), live log, answer, changes with diff,
  merge / force / undo / discard / delete, reply in the same session.
* **Orchestrator** (`#jobs`, `#job=<id>`) — workflow, optional steps, ask after every step, e-mails, editable prompts;
  job detail with step outputs, Approve / Redo with note / Skip / Cancel / Retry, run again from a step, Word per step.
* **Systems** — URL fields (→ `-D` properties), environments, login fields (environment variables; secret defaults never
  stored), bug ID prefix, parallel limit, serial groups, test types, module map; reset to the seed files.

## Rules that protect the project

* Logins typed in the form reach the test JVM only as environment variables of that run; they are never written to
  `run.json`, logs, schedules or the command line (AGENTS.md). Schedules use the credentials the runner inherits.
* ZakaaDash runs serially (`serialOnly`, one shared account). Zulfi can run up to 4 classes in parallel; the analysis
  package is one serial group (fixture manifest order).
* "Retry timing failures once" adds `com.taf.core.listeners.TimingRetry`: only timeouts / stale / intercepted elements,
  at most once, never assertions, and never in a run with an open create/submit/lifecycle gate.
* Fixture (setup/teardown) results are reported as setup failures, never counted as tests.
* Claude works in a copy (`RUNNER_AGENT_WORK`, default `..\.runner-agent`) without `config/credentials.local.properties`
  and without secret environment variables; nothing reaches the project before you merge, and undo restores it.
* Historical reports under `test-execution-reports/` are only read.

## Success criteria (dashboard)

* Computed from the latest executed result of every test in the period (runs with *Count in success criteria* on).
* Pass rate = passed / (passed + failed + broken). Open defects = latest failed/broken, not "not a bug", not quarantined,
  test still in the sources. Reopen rate = Reopened / open (failed earlier, passed, fails again on a later day).
  Flaky rate = tests that passed only on retry. Availability = uptime checks (every `RUNNER_UPTIME_EVERY` s) + run time.
* Release status: 🔴 فيه Critical (any Critical) · 🟡 فيه مخاطر (other open defects or pass rate < 95 %) · 🟢 جاهز للإصدار;
  ⚪ when there is no executed test in the period.

## Settings

| Variable | Default | |
|---|---|---|
| `RUNNER_HOST` / `RUNNER_PORT` | `127.0.0.1` / `8080` | bind address / port |
| `RUNNER_USER` / `RUNNER_PASSWORD` | `admin` / — | Basic auth when a password is set (required off localhost) |
| `RUNNER_MAX_PARALLEL` | `4` | runs at once |
| `RUNNER_EXECUTION` | `LocalHeadless` | default browser mode |
| `RUNNER_DATA_DIR` | `runner/` | results and state folder |
| `RUNNER_ALLOWED_HOSTS` | — | extra `Host` names (comma separated) |
| `RUNNER_JVM_ARGS` | — | extra JVM options for test runs |
| `MVN`, `JAVA_HOME`, `ALLURE_CLI`, `RUNNER_MVN_ARGS` | PATH | tools |
| `RUNNER_DB_HOST`, `_PORT`, `_USER`, `_PASSWORD`, `_NAME`, `_SSL_CA`, `_SSL` | — | MySQL (first start imports the JSON files) |
| `RUNNER_SECRET_KEY` | — | vault key (`python runner/vault.py --new-key`) for `enc:v1:` values |
| `RUNNER_SPACES_KEY`, `_SECRET`, `_BUCKET`, `_REGION`, `_PREFIX`, `_ENDPOINT` | — | DigitalOcean Spaces |
| `RUNNER_GMAIL_USER`, `RUNNER_GMAIL_APP_PASSWORD`, `RUNNER_PUBLIC_URL` | — | e-mail notifications |
| `RUNNER_CLAUDE`, `RUNNER_AGENT_MODEL`, `RUNNER_AGENT_BUDGET_USD`, `RUNNER_AGENT_WORK`, `RUNNER_AGENT_TOOLS`, `ANTHROPIC_API_KEY` | `claude` | Claude Code |
| `RUNNER_GITHUB_REPO`, `_BRANCH`, `_TOKEN`, `RUNNER_ALLOW_UPDATE` | — / `main` | update from GitHub (off until a repo is set) |
| `RUNNER_FLAKY_AUTO`, `_FLIPS`, `_WINDOW`, `_STABLE` | `1`, `3`, `8`, `5` | auto-quarantine |
| `RUNNER_ENV_RETRIES`, `RUNNER_ENV_RETRY_MINUTES` | `3`, `10` | environment-down retries |
| `RUNNER_UPTIME`, `RUNNER_UPTIME_EVERY` | `1`, `300` | availability monitor |

### Encrypted credentials (optional)

```powershell
python runner/vault.py --new-key            # put the output in RUNNER_SECRET_KEY (environment or runner/.env)
echo my-password | python runner/vault.py --encrypt
```

Use the printed `enc:v1:...` value in `config/credentials.local.properties`, an environment variable or the run form;
`LocalCredentials` decrypts it with `RUNNER_SECRET_KEY` (JVM property or environment).

## API

Every response is JSON (`Cache-Control: no-store`) except downloads. Every POST needs the header `X-Runner: 1`.

| GET | |
|---|---|
| `/api/catalog` | systems, URLs, environments, logins (no secrets), classes, E2E, regression, modules, test types, presets |
| `/api/runs` · `/api/runs/<id>` · `/api/runs/<id>/console?offset=` · `/api/runs/<id>/bugs` | runs list, detail (log tail), live console, bug suggestions |
| `/api/build` · `/api/build/log` | snapshot state (stale flag, update state), build log |
| `/api/dashboard?days=30&system=` | quality, release status, trend, defects, fixed, modules, slowest, most failing |
| `/api/retest/<system>` | failing tests and their retest plan (with `dependsOnMethods`) |
| `/api/config` · `/api/schedules` · `/api/storage` · `/api/reliability?system=` | configuration, schedules, storage, reliability |
| `/api/agent` · `/api/agent/auth` · `/api/agent/<id>?after=N` · `/api/agent/<id>/diff` | Claude |
| `/api/jobs` · `/api/jobs/<id>` · `/api/jobs/prompt?type=&step=` · `/api/jobs/<id>/steps/<n>/prompt` · `/api/jobs/<id>/steps/<n>/report` · `/api/jobs/<id>/file/<name>` | orchestrator |
| `/runs/<id>/report.html` · `/runs/<id>/console.log` · `/runs/<id>/shot/<file>` | reports (local or redirect to Spaces) |
| `/api/archive?system=` · `/api/health?system=` · `/archive/<cli\|dashboard>/<id>/<path>` | Maven report history (read-only) |

| POST | Body |
|---|---|
| `/api/runs` | run request (see below) |
| `/api/runs/all` | `{env, execution, parallelSystems, ...}` |
| `/api/runs/<id>/stop` · `/rerun` · `/rerun-failed` | — |
| `/api/runs/<id>/fullrun` · `/metrics` | `{count}` |
| `/api/runs/<id>/bugreport` | `{format: docx\|xlsx, bugs}` → file |
| `/api/runs/<id>/ask-claude` · `/heal` | `{autoRun}` |
| `/api/runs/delete` | `{ids, mode: runs\|reports}` |
| `/api/build` · `/api/update` | — |
| `/api/schedules` · `/api/schedules/<id>/delete\|run\|toggle\|test` | schedule |
| `/api/config/<system>` · `/api/config/<system>/reset` | configuration |
| `/api/bugs/rate` | `{run, test, severity, note, notBug}` |
| `/api/defects/retest` · `/api/defects/retest-all` | `{run, test}` · `{defects: [...]}` |
| `/api/storage/clean` | `{systems, keep, mode}` |
| `/api/agent` · `/api/agent/<id>/reply\|stop\|merge\|undo-merge\|discard\|delete` | `{system, text, files, mode, autoRun}` · `{text}` · `{force}` |
| `/api/agent/auth/login\|code\|cancel\|logout` | `{method}` · `{code}` |
| `/api/jobs` · `/api/jobs/<id>/decide\|retry\|delete` · `/api/jobs/<id>/steps/<n>/prompt\|rerun` | job · `{decision, note}` · `{prompt}` |
| `/api/reliability/quarantine` · `/api/reliability/ask-claude` | `{system, test, on, reason}` · `{system}` |

Run request: `{system, env, urls{label:url}, logins{ENV:value}, scope: full|e2e|e2e-only|classes|modules|retest|retest-tests|retest-one,
classes[], modules[], tests{Class:[methods]}, group, threads, execution: LocalHeadless|Local|Remote, browser, pace,
gridHost, gridPort, preflight, retry, metrics, fullRun, extraProperties, parallelStart, after[]}`.

```powershell
curl -X POST http://localhost:8080/api/runs -H "X-Runner: 1" -H "Content-Type: application/json" `
  -d '{"system":"zulfi","scope":"classes","classes":["LoginRegressionTest"],"pace":"fast"}'
```

## Deployment (Docker)

```bash
RUNNER_PASSWORD=change-me docker compose -f runner/docker-compose.yml up -d --build
```

The image (`maven:3.9-eclipse-temurin-21`) adds Chrome, Python, git and Claude Code and pre-compiles the project.
Volumes: `runner-runs` (`/data` = `RUNNER_DATA_DIR`), `maven-repo`, `claude-home`; `shm_size: 2gb`;
`host.docker.internal` reaches a MySQL on the host. Credentials for the tests go in the container environment
(`ZULFI_USER`, `ZULFI_PASSWORD`, `ZAKAADASH_EMAIL`, `ZAKAADASH_PASSWORD`) or `runner/.env`.

## Tests

```powershell
python -m unittest discover -s runner/tests -v      # 87 runner tests, no network
mvn test "-Dsurefire.suiteXmlFiles=suites/shared-core.xml"   # includes SecretVault and TimingRetry contracts
```
