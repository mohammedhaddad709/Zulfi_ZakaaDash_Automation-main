# Installs Allure 2.29.1 CLI under tools/ (no admin, project-local).
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
# CLI 2.29.0 matches project Allure adapter BOM ~2.29.x (2.29.1 has no standalone zip on GitHub).
$version = '2.29.0'
$dest = Join-Path $projectRoot "tools\allure-$version"
$zip = Join-Path $projectRoot 'tools\allure.zip'
$url = "https://github.com/allure-framework/allure2/releases/download/$version/allure-$version.zip"

New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'tools') | Out-Null
if (Test-Path (Join-Path $dest 'bin\allure.bat')) {
    Write-Host "Allure already present at $dest"
    exit 0
}
Write-Host "Downloading Allure $version..."
Invoke-WebRequest -Uri $url -OutFile $zip
Expand-Archive -Path $zip -DestinationPath (Join-Path $projectRoot 'tools') -Force
Remove-Item $zip
$bat = Join-Path $dest 'bin\allure.bat'
& $bat --version
Write-Host "Set for this session: `$env:ALLURE_CLI = '$bat'"
