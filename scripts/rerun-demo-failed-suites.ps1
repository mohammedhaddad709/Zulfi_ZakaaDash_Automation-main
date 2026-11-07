# Re-run demo-safe suites that failed in a prior full-demo-safe master log (post automation fixes).
param(
    [string]$MasterLog = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if ([string]::IsNullOrWhiteSpace($MasterLog)) {
    $latest = Get-ChildItem (Join-Path $projectRoot 'test-execution-reports\full-demo-safe\full-demo-safe-*.log') |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $latest) { throw 'No full-demo-safe master log found.' }
    $MasterLog = $latest.FullName
}

$failed = @()
Get-Content -LiteralPath $MasterLog | ForEach-Object {
    if ($_ -match '=== FAIL (\S+) exit=') { $failed += $Matches[1] }
}
$failed = $failed | Select-Object -Unique

Write-Host "Re-running failed shards from: $MasterLog" -ForegroundColor Cyan
Write-Host ("Failed: " + ($failed -join ', ')) -ForegroundColor Yellow

& (Join-Path $projectRoot 'scripts\run-portal-full-demo-safe.ps1') -OnlyNames $failed 2>$null
if ($LASTEXITCODE -ne 0 -and -not (Test-Path (Join-Path $projectRoot 'scripts\run-portal-full-demo-safe.ps1'))) {
    # Fallback: invoke mvn per shard using same map as full script (subset).
    $map = @{
        'HOME-001' = @{ Suite = 'home-overview-suite.xml'; Phase = @('-Dhome.overview.phase=c') }
        'HOME-RES' = @{ Suite = 'home-current-results-suite.xml'; Phase = @('-Dhome.current.results.phase=c') }
        'CLUBS' = @{ Suite = 'clubs-yellow-league-suite.xml'; Phase = @('-Dclubs.yellow.phase=c') }
        'CLUB-INF' = @{ Suite = 'club-information-suite.xml'; Phase = @('-Dclub.info.phase=b') }
        'MCH-SCHED' = @{ Suite = 'mch-schedule-suite.xml'; Phase = @('-Dmch.schedule.phase=c') }
        'ANL-SYNC' = @{ Suite = 'anl-sync-suite.xml'; Phase = @('-Danl.sync.phase=c') }
        'ANL-STS' = @{ Suite = 'anl-sts-suite.xml'; Phase = @('-Danl.sts.phase=c') }
        'ANL-LST' = @{ Suite = 'anl-lst-suite.xml'; Phase = @('-Danl.lst.phase=c') }
        'ANL-TYPE' = @{ Suite = 'match-type-official-suite.xml'; Phase = @('-Danl.match.type.phase=d') }
        'ANL-TYPE-B' = @{ Suite = 'match-type-official-b-suite.xml'; Phase = @('-Danl.match.type.phase=d') }
        'ANL-FRD' = @{ Suite = 'friendly-match-analysis-suite.xml'; Phase = @('-Danl.friendly.phase=d') }
    }
    $common = @('test', '-Dexecution.pace=demo', '-Danalysis.create.enabled=false', '-Dmatch.schedule.analysis.submit.enabled=false')
    foreach ($name in $failed) {
        if (-not $map.ContainsKey($name)) { continue }
        $entry = $map[$name]
        Write-Host "=== RERUN $name ===" -ForegroundColor Cyan
        & mvn @common "-DsuiteXmlFile=src/test/resources/suites/zulfi/$($entry.Suite)" @($entry.Phase)
    }
}

exit 0
