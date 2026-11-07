param(
    [switch]$FormOnly,
    [int]$FixtureIndex = 2,
    [switch]$NonInteractive
)

# ANL-OFF-HP-001 — Create Analysis Happy Path (DEMO pace, visible form).
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not $env:ZULFI_USER -or -not $env:ZULFI_PASSWORD) {
    if ($NonInteractive) {
        Write-Error 'Set ZULFI_USER and ZULFI_PASSWORD in this shell before running, or run without -NonInteractive to prompt.'
        exit 2
    }
    if (-not $env:ZULFI_USER) {
        $env:ZULFI_USER = Read-Host 'Login email'
    }
    if (-not $env:ZULFI_PASSWORD) {
        $secure = Read-Host 'Login password' -AsSecureString
        $env:ZULFI_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
        Remove-Variable secure
    }
}

$createFlag = if ($FormOnly) { 'false' } else { 'true' }
$modeLabel = if ($FormOnly) { 'form only (no submit)' } else { 'full Happy Path with submit' }

Write-Host "Running ANL-OFF-HP-001 | $modeLabel | DEMO | fixture index $FixtureIndex" -ForegroundColor Cyan

$suite = 'src/test/resources/suites/zulfi/analysis-official-hp-suite.xml'
& mvn test `
    '-DbrowserType=Chrome' `
    '-DexecutionType=Local' `
    '-Dexecution.pace=demo' `
    '-Dreport.video=ON_FAILURE' `
    '-Dreport.open=true' `
    "-DsuiteXmlFile=$suite" `
    "-Danalysis.create.enabled=$createFlag" `
    "-Danalysis.fixture.index=$FixtureIndex"

exit $LASTEXITCODE
