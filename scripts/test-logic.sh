#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" TMessagesProj/src/main/java/org/telegram/messenger/mokagram/GhostPolicy.java tests/GhostPolicyTest.java
java -cp "$classes" GhostPolicyTest
