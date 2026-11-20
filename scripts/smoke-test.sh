#!/usr/bin/env bash
# Checks a running HostelOS (local or hosted) end to end through its real API.
# Usage: scripts/smoke-test.sh https://hostelos.onrender.com [SITE_PASSCODE]
set -uo pipefail
BASE="${1:?usage: smoke-test.sh BASE_URL [SITE_PASSCODE]}"; BASE="${BASE%/}"
PASS="${2:-}"; JAR="$(mktemp)"; fail=0
ok()  { echo "  ok   $1"; }
bad() { echo "  FAIL $1"; fail=1; }
check() { [ "$2" = "$3" ] && ok "$1" || bad "$1 (expected $3, got $2)"; }
code() { curl -s -o /dev/null -w '%{http_code}' -b "$JAR" -c "$JAR" "$@"; }

echo "Smoke test: $BASE"
check "health check" "$(code "$BASE/healthz")" 200
if [ -n "$PASS" ]; then
  check "API is locked without passcode" "$(code "$BASE/api/auth/demo-accounts")" 401
  code -X POST "$BASE/gate" --data-urlencode "passcode=$PASS" >/dev/null
fi
check "UI loads" "$(code "$BASE/")" 200
check "client-side route falls back to the app" "$(curl -s -b "$JAR" "$BASE/meals" | grep -c '<div id="root">')" 1
login() { curl -s -b "$JAR" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"hostelos123\"}" "$BASE/api/auth/login" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))'; }
S="$(login aarav.sharma@iiita.ac.in)"; A="$(login admin@hostelos.in)"; M="$(login mess@hostelos.in)"
[ -n "$S" ] && ok "student login" || bad "student login"
[ -n "$A" ] && ok "admin login" || bad "admin login"
[ -n "$M" ] && ok "mess staff login" || bad "mess staff login"
check "student dashboard (database works)" "$(code -H "Authorization: Bearer $S" "$BASE/api/dashboard/student")" 200
check "admin dashboard" "$(code -H "Authorization: Bearer $A" "$BASE/api/dashboard/admin")" 200
check "student is refused admin data" "$(code -H "Authorization: Bearer $S" "$BASE/api/dashboard/admin")" 403
check "mess board" "$(code -H "Authorization: Bearer $M" "$BASE/api/mess/board")" 200
rm -f "$JAR"
[ $fail -eq 0 ] && echo "ALL CHECKS PASSED" || { echo "SOME CHECKS FAILED"; exit 1; }
