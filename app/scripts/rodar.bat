@echo off
REM Compila e sobe a Tami no Windows. Uso: scripts\rodar.bat [validar ^| simular 300 ^| zerar]
chcp 65001 >nul
cd /d "%~dp0\.."

where javac >nul 2>nul
if errorlevel 1 (
  echo.
  echo Nao achei o Java ^(JDK^). Instale o JDK 17 ou mais novo e abra este arquivo de novo.
  echo Exemplo: https://adoptium.net
  pause
  exit /b 1
)

if not exist tami.properties (
  echo.
  echo Falta o arquivo tami.properties.
  echo Renomeie tami.properties.supabase para tami.properties e coloque a senha do banco.
  echo Para testar sem internet, renomeie tami.properties.exemplo ^(banco local H2^).
  pause
  exit /b 1
)

if not exist build\classes\br (
  echo Compilando a Tami...
  if exist build rmdir /s /q build
  mkdir build\classes
  REM caminhos relativos e com barra normal: a pasta pode ter espacos no nome
  powershell -NoProfile -Command "Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { (Resolve-Path -Relative $_.FullName) -replace '\\','/' } | Set-Content -Encoding ascii build\fontes.txt"
  javac --release 17 -encoding UTF-8 -cp "lib/*" -d build/classes @build/fontes.txt
  if errorlevel 1 ( pause & exit /b 1 )
  xcopy /e /i /q /y src\main\resources build\classes >nul
  xcopy /e /i /q /y sql build\classes\sql >nul
)

java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "build/classes;lib/*" br.com.fiap.tami.app.App %*
if errorlevel 1 pause
