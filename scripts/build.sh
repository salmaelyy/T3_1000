#!/usr/bin/env bash
set -euo pipefail

# immer ins Projekt-Root (eine Ebene über scripts/) wechseln
cd "$(dirname "$0")/.."

INPUT_DIR="${1:-asyncapi}"
MAIN_CLASS="com.example.asyncapigenerator.Main"
JAR_GLOB="target/*-jar-with-dependencies.jar"

echo ">> Building project (Maven, skip tests) …"
mvn -q -DskipTests clean package

echo ">> Running generator on folder: ${INPUT_DIR}/"
export ASYNCAPI_INPUT_DIR="${INPUT_DIR}"  # falls du später per Env lesen willst

# Versuch 1: Maven Exec Plugin
set +e
mvn -q -DskipTests exec:java -Dexec.mainClass="$MAIN_CLASS"
STATUS=$?
set -e

if [ $STATUS -ne 0 ]; then
  # Versuch 2: Fat/Shaded JAR (falls vorhanden)
  FAT_JAR=$(ls $JAR_GLOB 2>/dev/null | head -n1 || true)
  if [ -n "${FAT_JAR}" ]; then
    echo ">> Falling back to shaded jar: ${FAT_JAR}"
    java -jar "${FAT_JAR}"
  else
    echo "!! Hinweis: Konnte den Generator nicht automatisch starten."
    echo "   Entweder exec:java Plugin fehlt ODER kein shaded JAR erzeugt."
    echo "   Starte manuell eine der folgenden Varianten:"
    echo "   1) mvn -q exec:java -Dexec.mainClass=${MAIN_CLASS}"
    echo "   2) (falls shaded jar konfiguriert) java -jar target/<name>-jar-with-dependencies.jar"
    echo "   3) Oder: java -cp target/classes ${MAIN_CLASS} (Classpath inkl. Dependencies notwendig)"
    exit 1
  fi
fi

echo
echo ">> Outputs:"
echo "   Mermaid (.mmd):      generated-sources/mmd"
echo "   HTML (mit Toggles):  generated-sources/html"