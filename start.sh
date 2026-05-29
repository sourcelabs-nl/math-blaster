#!/usr/bin/env bash
# Launch Math Blaster on the JVM (fast dev loop).
# KorGE 6 needs JDK 21-24, but this machine defaults to JDK 25 (via jenv), which the bundled
# Kotlin compiler cannot parse. Pin a supported JDK here, overriding any inherited JAVA_HOME.
# Override by exporting GAME_JAVA_HOME before running.
set -euo pipefail

JAVA_HOME="${GAME_JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "No JDK at $JAVA_HOME. Set GAME_JAVA_HOME to a JDK 21-24." >&2
  exit 1
fi

cd "$(dirname "$0")"
exec ./gradlew runJvm "$@"
