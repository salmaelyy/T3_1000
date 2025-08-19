#!/usr/bin/env bash
set -euo pipefail

# immer ins Projekt-Root (eine Ebene über scripts/) wechseln
cd "$(dirname "$0")/.."

echo ">> Cleaning Maven build artifacts …"
mvn -q clean

echo ">> Removing generated diagram outputs …"
rm -rf generated-sources/mmd generated-sources/html || true

# optional weitere Ordner:
# rm -rf build/ out/ logs/ || true

echo ">> Clean done."