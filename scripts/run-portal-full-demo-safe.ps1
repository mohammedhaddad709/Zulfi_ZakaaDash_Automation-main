# Full portal regression in DEMO pace - read-only / safe gates (no authorized submit).
# Excludes Tier-3 submit suites (official HP, MCH-ANL-010 submit, friendly phase-e create).
# Requires ZULFI_USER / ZULFI_PASSWORD for UI suites.
param(
    [string[]]$OnlyNames = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$logDir = Join-Path $projectRoot 'test-execution-reports\full-demo-safe'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$masterLog = Join-Path $logDir ('full-demo-safe-' + $stamp + '.log')

function Write-Log([string]$msg) {
    $line = '[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ' + $msg
    Add-Content -LiteralPath $masterLog -Value $line
    Write-Host $line
}

if (-not $env:ZULFI_USER) {
    Write-Log 'WARN: ZULFI_USER not set - UI suites may Skip or fail.'
}

$commonArgs = @(
    'test'
    '-Dexecution.pace=demo'
    '-Danalysis.create.enabled=false'
    '-Dmatch.schedule.analysis.submit.enabled=false'
    '-Dreport.video=ON_FAILURE'
)


$runs = @(
    @{ Name = 'UNIT'; Suite = 'portal-unit-tests-suite.xml'; Phase = @() }
    @{ Name = 'LOGIN'; Suite = 'login-regression-suite.xml'; Phase = @() }
    @{ Name = 'LOGIN-UI-003'; Suite = 'login-ui-003-suite.xml'; Phase = @() }
    @{ Name = 'LOGIN-UI-004'; Suite = 'login-ui-004-suite.xml'; Phase = @() }
    @{ Name = 'HOME-001'; Suite = 'home-overview-suite.xml'; Phase = @('-Dhome.overview.phase=c') }
    @{ Name = 'HOME-RES'; Suite = 'home-current-results-suite.xml'; Phase = @('-Dhome.current.results.phase=c') }
    @{ Name = 'CLUBS'; Suite = 'clubs-yellow-league-suite.xml'; Phase = @('-Dclubs.yellow.phase=c') }
    @{ Name = 'CLUB-INF'; Suite = 'club-information-suite.xml'; Phase = @('-Dclub.info.phase=b') }
    @{ Name = 'MCH-SCHED'; Suite = 'mch-schedule-suite.xml'; Phase = @('-Dmch.schedule.phase=c') }
    @{ Name = 'ANL-SYNC'; Suite = 'anl-sync-suite.xml'; Phase = @('-Danl.sync.phase=c') }
    @{ Name = 'ANL-STS'; Suite = 'anl-sts-suite.xml'; Phase = @('-Danl.sts.phase=c') }
    @{ Name = 'ANL-LST'; Suite = 'anl-lst-suite.xml'; Phase = @('-Danl.lst.phase=c') }
    @{ Name = 'MCH-ANL'; Suite = 'match-schedule-analysis-suite.xml'; Phase = @('-Dmatch.schedule.analysis.phase=b') }
    @{ Name = 'ANL-TYPE'; Suite = 'match-type-official-suite.xml'; Phase = @('-Danl.match.type.phase=d') }
    @{ Name = 'ANL-TYPE-B'; Suite = 'match-type-official-b-suite.xml'; Phase = @('-Danl.match.type.phase=d') }
    @{ Name = 'ANL-FRD'; Suite = 'friendly-match-analysis-suite.xml'; Phase = @('-Danl.friendly.phase=d') }
    @{ Name = 'ANL-DISCOVERY'; Suite = 'analysis-discovery-suite.xml'; Phase = @() }
    @{ Name = 'ANL-FIXTURE-SEL'; Suite = 'analysis-fixture-selection-smoke-suite.xml'; Phase = @() }
    @{ Name = 'ANL-PREFLIGHT'; Suite = 'official-fixture-preflight-suite.xml'; Phase = @() }
    @{ Name = 'ANL-HP-READY'; Suite = 'analysis-hp-readiness-suite.xml'; Phase = @() }
)

Write-Log ('Master log: ' + $masterLog)
Write-Log 'Excluded submit Tier 3: analysis-official-hp-suite, mch-anl-010-submit-suite, friendly-match-analysis-e-suite'

$failed = @()
$skipped = @()
foreach ($r in $runs) {
    if ($OnlyNames.Count -gt 0 -and ($OnlyNames -notcontains $r.Name)) {
        continue
    }
    $path = Join-Path $projectRoot ('src\test\resources\suites\zulfi\' + $r.Suite)
    if (-not (Test-Path -LiteralPath $path)) {
        Write-Log ('SKIP ' + $r.Name + ' missing suite')
        $skipped += $r.Name
        continue
    }
    $suiteLog = Join-Path $logDir ($stamp + '-' + $r.Name + '.log')
    Write-Log ('=== START ' + $r.Name + ' ' + $r.Suite + ' ===')
    $mvnArgs = $commonArgs + @('-DsuiteXmlFile=src/test/resources/suites/zulfi/' + $r.Suite) + $r.Phase
    & mvn @mvnArgs *>&1 | Tee-Object -FilePath $suiteLog
    if ($LASTEXITCODE -ne 0) {
        Write-Log ('=== FAIL ' + $r.Name + ' exit=' + $LASTEXITCODE + ' ===')
        $failed += $r.Name
    } else {
        Write-Log ('=== PASS ' + $r.Name + ' ===')
    }
}

Write-Log ('Finished. Failed: ' + ($failed -join ',') + '; Skipped: ' + ($skipped -join ','))
if ($failed.Count -gt 0) { exit 1 }
exit 0
