#!/usr/bin/env bash
# Runs the backend (:8080) and the Vue dev server (:5173) together.
# Ctrl-C stops both. Override the portal with ETRADE_BASE_URL=... ./dev.sh
set -euo pipefail

# Job control, so each child below becomes its own process group and can be torn down
# with its whole subtree: `npm run dev` spawns vite as a child, and killing npm alone
# would leave vite holding port 5173.
set -m

cd "$(dirname "$0")"
base_url="${ETRADE_BASE_URL:-https://etrade.gov.et}"
pids=()

stop() {
  trap - INT TERM EXIT
  for pid in "${pids[@]:-}"; do
    [ -n "$pid" ] && kill -- "-$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap stop INT TERM EXIT

[ -d frontend/node_modules ] || (cd frontend && npm install)

echo "backend  -> http://localhost:8080  (portal: $base_url)"
(cd backend && exec mvn -q -B spring-boot:run \
  -Dspring-boot.run.arguments="--etrade.base-url=$base_url") &
pids+=($!)

echo "frontend -> http://localhost:5173"
(cd frontend && exec npm run dev --silent) &
pids+=($!)

# Return as soon as either side dies, so a crashed backend does not leave a half-running stack.
wait -n
