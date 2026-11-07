# Tier 1 — phase-a read-only portal smoke (no analysis submit).
# Skips suites missing on disk. Requires ZULFI_USER / ZULFI_PASSWORD.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not $env:ZULFI_USER) {
    Write-Host 'WARN: ZULFI_USER not set — protected suites may Skip or fail.' -ForegroundColor Yellow
}

$commonArgs = @(
    'test'
    '-Dexecution.pace=fast'
    '-Danalysis.create.enabled=false'
)

$runs = @(
    @{ Name = 'HOME-001'; Suite = 'home-overview-suite.xml'; Phase = '-Dhome.overview.phase=a' }
    @{ Name = 'HOME-RES'; Suite = 'home-current-results-suite.xml'; Phase = '-Dhome.current.results.phase=a' }
    @{ Name = 'CLUBS'; Suite = 'clubs-yellow-league-suite.xml'; Phase = '-Dclubs.yellow.phase=a' }
    @{ Name = 'CLUB-DET'; Suite = 'club-details-suite.xml'; Phase = '-Dclub.details.phase=a' }
    @{ Name = 'STD'; Suite = 'standings-suite.xml'; Phase = '-Dstandings.phase=a' }
    @{ Name = 'MCH-SCHED'; Suite = 'mch-schedule-suite.xml'; Phase = '-Dmch.schedule.phase=a' }
    @{ Name = 'ANL-SYNC'; Suite = 'anl-sync-suite.xml'; Phase = '-Danl.sync.phase=a' }
    @{ Name = 'ANL-STS'; Suite = 'anl-sts-suite.xml'; Phase = '-Danl.sts.phase=a' }
    @{ Name = 'ANL-LST'; Suite = 'anl-lst-suite.xml'; Phase = '-Danl.lst.phase=a' }
)

$failed = @()
foreach ($r in $runs) {
    $path = Join-Path $projectRoot "src\test\resources\suites\zulfi\$($r.Suite)"
    if (-not (Test-Path -LiteralPath $path)) {
        Write-Host "SKIP $($r.Name) — missing $path" -ForegroundColor DarkYellow
        continue
    }
    Write-Host "=== Tier 1: $($r.Name) ($($r.Suite)) ===" -ForegroundColor Cyan
    & mvn @commonArgs "-DsuiteXmlFile=src/test/resources/suites/zulfi/$($r.Suite)" $r.Phase
    if ($LASTEXITCODE -ne 0) {
        $failed += $r.Name
    }
}

if ($failed.Count -gt 0) {
    Write-Host "Tier 1 finished with failures: $($failed -join ', ')" -ForegroundColor Red
    exit 1
}

Write-Host 'Tier 1 portal safe smoke completed.' -ForegroundColor Green
exit 0
