#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z ${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}} && ! -f local.properties ]]; then
    echo 'Set ANDROID_HOME to your Android SDK directory. See docs/BUILD.md.' >&2
    exit 1
fi
variant=Debug
if [[ ${1:-} == release ]]; then
    variant=Release
    shift
elif [[ ${1:-} == debug ]]; then
    shift
fi
./gradlew :Mokagram:validateMokagramApi ":Mokagram:assemble$variant" --console=plain "$@"
