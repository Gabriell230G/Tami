#!/bin/sh
# Compila a Tami em build/ (Java 17 ou mais novo).
cd "$(dirname "$0")/.." || exit 1
rm -rf build && mkdir -p build/classes
javac --release 17 -encoding UTF-8 -cp "lib/*" -d build/classes $(find src/main/java -name '*.java') || exit 1
cp -r src/main/resources/* build/classes/
mkdir -p build/classes/sql && cp -r sql/* build/classes/sql/
echo "Compilado em build/classes"
