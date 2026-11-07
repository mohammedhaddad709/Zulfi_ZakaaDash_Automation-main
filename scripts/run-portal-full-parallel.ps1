# Full portal regression in one TestNG suite (parallel=classes, thread-count=4).
param(
    [ValidateSet('fast', 'normal')]
    [string]$ExecutionPace = 'fast'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not $env:ZULFI_USER) {
    Write-Warning 'ZULFI_USER not set — UI tests may skip or fail.'
}

$mch010Marker = '-Dmch.e2e.description.marker=MCH-ANL-010 authorized E2E 20261017-171620'

$args = @(
    'test'
    '-Psuite'
    '-DsuiteXmlFile=src/test/resources/suites/zulfi/portal-full-parallel-suite.xml'
    ('-Dexecution.pace=' + $ExecutionPace)
    '-Danalysis.create.enabled=false'
    '-Dmatch.schedule.analysis.submit.enabled=false'
    '-Dreport.video=ON_FAILURE'
    '-Dhome.overview.phase=c'
    '-Dhome.current.results.phase=c'
    '-Dclubs.yellow.phase=c'
    '-Dclub.info.phase=b'
    '-Dmch.schedule.phase=c'
    '-Danl.sync.phase=c'
    '-Danl.sts.phase=c'
    '-Danl.lst.phase=c'
    '-Dmatch.schedule.analysis.phase=b'
    '-Danl.match.type.phase=d'
    '-Danl.friendly.phase=d'
    '-Dmatch.schedule.analysis.targetExternalEventId=16651987'
    $mch010Marker
)

Write-Host ('mvn ' + ($args -join ' '))
Write-Host 'Note: the MCH-ANL submit chain (010-016) runs separately via match-schedule-analysis-submit-suite.xml with explicit submit authorization.'
& mvn @args
exit $LASTEXITCODE
