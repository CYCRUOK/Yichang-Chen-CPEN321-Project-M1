#!/usr/bin/env pwsh
# Hits every M1 backend endpoint on a deployed server and checks the responses.
# Usage: .\scripts\smoke-test-backend.ps1 http://<ip-or-domain>:3000   (or https://<domain>)
param([Parameter(Mandatory = $true)][string]$BaseUrl)
$BaseUrl = $BaseUrl.TrimEnd('/')
$fail = $false
function Check($name, $url, $regex) {
    $body = curl.exe -sS --max-time 10 $url 2>&1 | Out-String
    $body = $body.Trim()
    if ($LASTEXITCODE -eq 0 -and $body -match $regex) { Write-Host ("PASS  {0,-12} {1}" -f $name, $body) }
    else { Write-Host ("FAIL  {0,-12} {1}" -f $name, $body) -ForegroundColor Red; $script:fail = $true }
}
Check 'health'      "$BaseUrl/health"          '^\{"status":"ok"\}$'
Check 'server-ip'   "$BaseUrl/api/server-ip"   '^\{"ip":"[0-9a-fA-F.:]+"\}$'
Check 'server-time' "$BaseUrl/api/server-time" '^\{"time":"\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}"\}$'
Check 'name'        "$BaseUrl/api/name"        '^\{"first":"[^"]+","last":"[^"]+"\}$'
Check 'not-found'   "$BaseUrl/api/nope"        '^\{"error":"Not Found"\}$'
if ($fail) { Write-Host 'SOME FAILED' -ForegroundColor Red; exit 1 } else { Write-Host 'ALL PASS' -ForegroundColor Green }
