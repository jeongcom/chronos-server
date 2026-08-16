#!/usr/bin/env bash
set -euo pipefail
GRADLE_VERSION=${GRADLE_VERSION:-9.1.0}
if ! command -v gradle >/dev/null 2>&1; then
  echo "Install Gradle ${GRADLE_VERSION}+ once, then run: gradle wrapper --gradle-version ${GRADLE_VERSION}" >&2
  exit 1
fi
gradle wrapper --gradle-version "$GRADLE_VERSION"
