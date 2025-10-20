#!/usr/bin/env bash
# Compile and run Network Simulator (Unix / Git Bash / WSL)
# Usage: ./run.sh

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$ROOT_DIR/Network Simulator/src"
OUT_DIR="$ROOT_DIR/out"

mkdir -p "$OUT_DIR"

echo "Compiling..."
TMPFILE="$(mktemp /tmp/javafiles.XXXXXX)"
find "$SRC_DIR" -name "*.java" > "$TMPFILE"
javac -d "$OUT_DIR" @"$TMPFILE"
RC=$?
rm -f "$TMPFILE"
if [ $RC -ne 0 ]; then
  echo "Compilation failed"
  exit 1
fi

echo "Running..."

# If on Windows under MSYS/MinGW/Git-Bash, try to call PowerShell which handles spaces better
if command -v powershell.exe >/dev/null 2>&1; then
  echo "Detected Powershell; delegating to run.ps1"
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$PWD/run.ps1")"
  exit $?
fi

# Ensure java exists
if ! command -v java >/dev/null 2>&1; then
  echo "java not found on PATH. Please install JDK and ensure 'java' is available." >&2
  exit 1
fi

java -cp "$OUT_DIR" UI.NetworkEditor
