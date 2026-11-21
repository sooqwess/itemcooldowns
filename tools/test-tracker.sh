#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn test-compile
repo="${M2_REPO:-$HOME/.m2/repository}"
cp="target/classes:target/test-classes:$(find "$repo" -name '*.jar' -printf '%p:' | sed 's/:$//')"
java -cp "$cp" com.sooqwess.itemcooldowns.TrackerSelfTest
