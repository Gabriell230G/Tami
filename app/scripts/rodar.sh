#!/bin/sh
# Compila (se preciso) e sobe a Tami. Uso: ./scripts/rodar.sh [validar|simular 300|zerar]
cd "$(dirname "$0")/.." || exit 1
[ -d build/classes ] || ./scripts/compilar.sh || exit 1
[ -f tami.properties ] || cp tami.properties.exemplo tami.properties
exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "build/classes:lib/*" br.com.fiap.tami.app.App "$@"
