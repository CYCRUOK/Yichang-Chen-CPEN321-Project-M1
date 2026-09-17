#!/usr/bin/env pwsh
<#
Assumes config (e.g., frontend\local.properties) is already setup:
  1. Starts the emulator named $env:AVD_NAME (default: "Pixel_9")
  2. Builds, installs, and launches the app

Override the emulator name if needed:
  $env:AVD_NAME = "EMULATOR_NAME"; .\scripts\run-frontend.ps1

NOTE ON EXECUTION POLICY:
  Windows blocks script execution by default. If running this script fails
  with a message about execution policies, either:
    - run once: Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
    - or launch with: pwsh -ExecutionPolicy Bypass -File .\scripts\run-frontend.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Root = (Resolve-Path (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..')).Path
Set-Location $Root

$FrontendDir = if ($env:FRONTEND_DIR) { $env:FRONTEND_DIR } else { 'frontend' }
$AvdName     = if ($env:AVD_NAME)     { $env:AVD_NAME }     else { 'Pixel_9' }

function Info($msg) { Write-Host "==> $msg" -ForegroundColor Blue }
function Die($msg)  { Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------

if (-not (Get-Command java -ErrorAction SilentlyContinue))  { Die "Java not found." }

if (-not (Test-Path (Join-Path $FrontendDir 'local.properties'))) { Die "Missing $FrontendDir\local.properties." }

$gradlew = Join-Path $FrontendDir 'gradlew.bat'
if (-not (Test-Path $gradlew)) { Die "Missing $gradlew." }

$localProperties = Join-Path $FrontendDir 'local.properties'

if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    # already set, use it
}
else {
    $sdkLine = Get-Content $localProperties | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
    $sdkDir = $null
    if ($sdkLine) {
        $sdkDir = ($sdkLine -replace '^\s*sdk\.dir\s*=\s*', '').Trim(' ', '"')
        $sdkDir = $sdkDir -replace '\\:', ':'
        $sdkDir = $sdkDir -replace '\\\\', '\'
    }
    if ($sdkDir -and (Test-Path $sdkDir)) {
        $env:ANDROID_HOME = $sdkDir
    }
    else {
        Die "Set ANDROID_HOME or sdk.dir in $FrontendDir\local.properties."
    }
}

$env:Path = "$($env:ANDROID_HOME)\platform-tools;$($env:ANDROID_HOME)\emulator;$($env:Path)"
$Emulator = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$Adb      = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'

if (-not (Test-Path $Emulator)) { Die "Android emulator not installed." }
if (-not (Test-Path $Adb))      { Die "adb not found." }

# ---------------------------------------------------------------------------
# Emulator
# ---------------------------------------------------------------------------

$devicesOutput = & $Adb devices
$emulatorRunning = $devicesOutput | Where-Object { $_ -match '^emulator-\d+\s+device$' }

if ($emulatorRunning) {
    Info "Emulator already running."
}
else {
    $avdList = & $Emulator -list-avds 2>$null
    if (-not ($avdList -contains $AvdName)) {
        Die "No AVD named '$AvdName'. Create it in Android Studio (Device Manager), or run: `$env:AVD_NAME = 'YourAvdName'; .\scripts\run-frontend.ps1"
    }

    $emulatorLog = Join-Path $env:TEMP "emulator-$AvdName.log"
    Info "Starting emulator '$AvdName' (log: $emulatorLog) ..."
    $emulatorProcess = Start-Process -FilePath $Emulator `
        -ArgumentList @('-avd', $AvdName, '-no-snapshot-save') `
        -WindowStyle Hidden `
        -RedirectStandardOutput $emulatorLog `
        -RedirectStandardError "$emulatorLog.err" `
        -PassThru

    # Poll for device readiness instead of blocking on wait-for-device,
    # so a crashed emulator process is detected and reported rather than
    # hanging silently.
    $deviceUp = $false
    for ($i = 0; $i -lt 120; $i++) {
        if ($emulatorProcess.HasExited) {
            Die "Emulator process exited unexpectedly (exit code $($emulatorProcess.ExitCode)). Check log: $emulatorLog and $emulatorLog.err"
        }
        $devicesNow = & $Adb devices
        if ($devicesNow | Where-Object { $_ -match '^emulator-\d+\s+device$' }) { $deviceUp = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $deviceUp) { Die "Emulator did not come online. Check log: $emulatorLog and $emulatorLog.err" }

    $booted = $false
    for ($i = 0; $i -lt 120; $i++) {
        if ($emulatorProcess.HasExited) {
            Die "Emulator process exited unexpectedly while booting (exit code $($emulatorProcess.ExitCode)). Check log: $emulatorLog and $emulatorLog.err"
        }
        $state = (& $Adb shell getprop sys.boot_completed 2>$null) -replace "`r", ''
        if ($state -eq '1') { $booted = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $booted) { Die "Emulator did not finish booting. Check log: $emulatorLog and $emulatorLog.err" }
    Info "Emulator ready."
}

# ---------------------------------------------------------------------------
# Frontend
# ---------------------------------------------------------------------------

Info "Building and installing app..."
Push-Location $FrontendDir
try {
    & .\gradlew.bat installDebug
    if ($LASTEXITCODE -ne 0) { Die "Gradle build/install failed." }
}
finally {
    Pop-Location
}

$buildFile = Join-Path $FrontendDir 'app\build.gradle.kts'
$appIdLine = Get-Content $buildFile | Where-Object { $_ -match '\sapplicationId\s*=' } | Select-Object -First 1
$ApplicationId = $null
if ($appIdLine -and ($appIdLine -match '"([^"]+)"')) {
    $ApplicationId = $Matches[1]
}
if (-not $ApplicationId) { Die "Could not determine applicationId from $buildFile." }

Info "Launching app..."
& $Adb shell monkey -p $ApplicationId -c android.intent.category.LAUNCHER 1 | Out-Null

Write-Host ""
Info "Done. Sign in on the emulator to verify."
