# Merge Allure results from a demo day + build HTML report + CSV test matrix.
param(
    [string]$DatePrefix = (Get-Date -Format 'yyyyMMdd'),
    [string]$OutRoot = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutRoot)) {
    $OutRoot = Join-Path $projectRoot ("test-execution-reports\demo-consolidated\" + $DatePrefix)
}

$allureIn = Join-Path $OutRoot 'allure-results'
$htmlOut = Join-Path $OutRoot 'allure-html'
$csvOut = Join-Path $OutRoot 'PORTAL_TEST_CASE_EXECUTION_MATRIX.csv'
$mdOut = Join-Path $projectRoot ("docs\testing\DEMO_RUN_" + $DatePrefix + "_FINAL_QA_REPORT.md")

New-Item -ItemType Directory -Force -Path $allureIn | Out-Null
if (Test-Path $allureIn) {
    Get-ChildItem $allureIn -File | Remove-Item -Force
}

$history = Join-Path $projectRoot 'test-execution-reports\history'
$copied = 0
Get-ChildItem $history -Directory | Where-Object { $_.Name -like ($DatePrefix + '*') } | ForEach-Object {
    $src = Join-Path $_.FullName 'allure-results'
    if (-not (Test-Path $src)) { return }
    Get-ChildItem $src -File | ForEach-Object {
        Copy-Item $_.FullName (Join-Path $allureIn ($_.Name)) -Force
        $copied++
    }
}

$allureBat = Join-Path $projectRoot 'tools\allure-2.29.0\bin\allure.bat'
if (-not (Test-Path $allureBat)) {
    throw "Allure CLI not found: $allureBat"
}
& $allureBat generate $allureIn -o $htmlOut --clean
Write-Host "Allure HTML: $htmlOut ($copied files)" -ForegroundColor Green

# CSV matrix from surefire junitreports + full-demo-safe logs
$rows = New-Object System.Collections.Generic.List[object]
$junitDir = Join-Path $projectRoot 'target\surefire-reports\junitreports'
if (Test-Path $junitDir) {
    Get-ChildItem $junitDir -Filter 'TEST-*.xml' | ForEach-Object {
        [xml]$doc = Get-Content $_.FullName
        $cases = @($doc.testsuite.testcase)
        foreach ($case in $cases) {
            if (-not $case) { continue }
            $name = $case.name
            $class = $case.Classname
            $time = $case.time
            $status = 'PASS'
            $msg = ''
            if ($case.SelectSingleNode('failure')) { $status = 'FAIL'; $msg = $case.failure.message }
            elseif ($case.SelectSingleNode('error')) { $status = 'ERROR'; $msg = $case.error.message }
            elseif ($case.SelectSingleNode('skipped')) { $status = 'SKIP' }
            $rows.Add([pscustomobject]@{
                    TestClass = $class
                    TestMethod = $name
                    Status = $status
                    DurationSec = $time
                    Message = ($msg -replace '[\r\n]+', ' ').Substring(0, [Math]::Min(500, ($msg + '').Length))
                })
        }
    }
}

$logDir = Join-Path $projectRoot 'test-execution-reports\full-demo-safe'
Get-ChildItem $logDir -Filter ('full-demo-safe-' + $DatePrefix + '*.log') | Sort-Object LastWriteTime -Descending | Select-Object -First 1 | ForEach-Object {
    $master = $_
    Get-Content $_.FullName | ForEach-Object {
        if ($_ -match '=== (PASS|FAIL) (\S+)') {
            $script:LastShard = $Matches[2]
            $script:LastShardStatus = $Matches[1]
        }
    }
}

$rows | Export-Csv -LiteralPath $csvOut -NoTypeInformation -Encoding UTF8
Write-Host "CSV matrix: $csvOut ($($rows.Count) rows from latest surefire)" -ForegroundColor Green

$reportStub = @"
# Demo Run QA Report ($DatePrefix)

Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm')

- **Allure HTML:** ``$htmlOut/index.html``
- **Excel/CSV matrix:** ``$csvOut``
- **Full demo logs:** ``test-execution-reports/full-demo-safe/``

See automation fix changelog in git diff for page objects updated after the demo run.

"@
Set-Content -LiteralPath $mdOut -Value $reportStub -Encoding UTF8
Write-Host "Report stub: $mdOut" -ForegroundColor Green
