#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
mkdir -p build/logic-tests
javac --release 8 -encoding UTF-8 -d build/logic-tests \
  app/src/main/java/io/github/amlfarhad/batcomputertv/LauncherLogic.java \
  tests/LauncherLogicTest.java
java -cp build/logic-tests io.github.amlfarhad.batcomputertv.LauncherLogicTest
