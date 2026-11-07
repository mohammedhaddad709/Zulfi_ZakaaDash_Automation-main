# Demo run: visible Chrome, human-paced DEMO, Login suite only (no analysis creation).
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not $env:ZULFI_USER) {
    $env:ZULFI_USER = Read-Host 'Login email'
}
if (-not $env:ZULFI_PASSWORD) {
    $secure = Read-Host 'Login password' -AsSecureString
    $env:ZULFI_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
    Remove-Variable secure
}

Write-Host 'Running login regression (DEMO pace, no analysis creation)...' -ForegroundColor Cyan

mvn test `
    "-DbrowserType=Chrome" `
    "-DexecutionType=Local" `
    "-Dexecution.pace=demo" `
    "-Dreport.video=ON_FAILURE" `
    "-Dreport.open=true" `
    "-DsuiteXmlFile=src/test/resources/suites/zulfi/login-regression-suite.xml"

exit $LASTEXITCODE
