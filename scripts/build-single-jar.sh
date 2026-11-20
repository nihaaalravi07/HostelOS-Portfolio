#!/usr/bin/env bash
# Builds exactly what the Docker image builds, without Docker: React app -> embedded into the Spring Boot jar.
# Result: backend/target/hostelos-backend-1.0.0.jar  (run with: java -jar ...). Needs JDK 21, Maven, Node 22+.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATIC="$ROOT/backend/src/main/resources/static"

( cd "$ROOT/frontend" && { [ -d node_modules ] || npm ci; } && npm run build )
rm -rf "$STATIC" && mkdir -p "$STATIC"
cp -R "$ROOT/frontend/dist/." "$STATIC/"
( cd "$ROOT/backend" && mvn -B -q -DskipTests package )
rm -rf "$STATIC"                      # keep the source tree clean: static/ is a build artefact only
echo "Built: $ROOT/backend/target/hostelos-backend-1.0.0.jar"
