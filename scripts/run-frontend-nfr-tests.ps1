#!/usr/bin/env pwsh
<#
Run frontend NFR tests (app/src/androidTest/.../nfr/*.kt).

Prerequisites: sign in inside the app on the emulator, then press Enter when prompted.
Usage: .\scripts\run-frontend-nfr-tests.ps1

NOTE ON EXECUTION POLICY:
  Windows blocks script execution by default. If running this script fails
  with a message about execution policies, either:
    - run once: Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
    - or launch with: pwsh -ExecutionPolicy Bypass -File .\scripts\run-frontend-nfr-tests.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Root = (Resolve-Path (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..')).Path
$FrontendDir = if ($env:FRONTEND_DIR) { $env:FRONTEND_DIR } else { Join-Path $Root 'frontend' }
$env:FRONTEND_DIR = $FrontendDir

. (Join-Path $Root 'scripts\lib\frontend-instrumented-test-common.ps1')
Run-InstrumentedTests nfr
