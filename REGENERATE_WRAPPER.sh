#!/usr/bin/env bash
set -euo pipefail
# Requires a system Gradle installed (e.g., via sdkman or package manager)
gradle wrapper --gradle-version 8.7 --distribution-type bin
echo "Wrapper jar created at gradle/wrapper/gradle-wrapper.jar"
