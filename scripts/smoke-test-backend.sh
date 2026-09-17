#!/usr/bin/env bash
# Hits every M1 backend endpoint on a deployed server and checks the responses.
# Usage: ./scripts/smoke-test-backend.sh http://<ip-or-domain>:3000   (or https://<domain>)
set -u
BASE="${1:?usage: $0 <base-url>}"
BASE="${BASE%/}"
fail=0
check() { # name url regex
  body="$(curl -sS --max-time 10 "$2" 2>&1)"; code=$?
  if [ $code -eq 0 ] && printf '%s' "$body" | grep -Eq "$3"; then
    printf 'PASS  %-12s %s\n' "$1" "$body"
  else
    printf 'FAIL  %-12s %s\n' "$1" "$body"; fail=1
  fi
}
check health      "$BASE/health"          '^\{"status":"ok"\}$'
check server-ip   "$BASE/api/server-ip"   '^\{"ip":"[0-9a-fA-F.:]+"\}$'
check server-time "$BASE/api/server-time" '^\{"time":"[0-9]{2}:[0-9]{2}:[0-9]{2} GMT[+-][0-9]{2}:[0-9]{2}"\}$'
check name        "$BASE/api/name"        '^\{"first":"[^"]+","last":"[^"]+"\}$'
check not-found   "$BASE/api/nope"        '^\{"error":"Not Found"\}$'
[ $fail -eq 0 ] && echo "ALL PASS" || { echo "SOME FAILED"; exit 1; }
