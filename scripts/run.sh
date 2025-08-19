#!/usr/bin/env bash
set -euo pipefail

# Projekt-Root ermitteln (eine Ebene über scripts/)
cd "$(dirname "$0")/.."

INPUT_DIR="${1:-asyncapi}"   # Default: asyncapi-Ordner im Projekt
MAIN_CLASS="com.example.asyncapigenerator.Main"

echo ">> Building project (Maven, skip tests) …"
mvn -q -DskipTests clean package

echo ">> Running generator on folder: ${INPUT_DIR}/"
mvn -q -DskipTests exec:java -Dexec.mainClass="$MAIN_CLASS" -Dexec.args="$INPUT_DIR"

echo
echo ">> Outputs:"
echo "   Mermaid (.mmd):      generated-sources/mmd"
echo "   HTML (mit Toggles):  generated-sources/html"