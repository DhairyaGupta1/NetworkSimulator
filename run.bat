@echo off
REM Delegate to PowerShell script (handles paths with spaces)
REM Usage: run.bat

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
if errorlevel 1 (
  echo PowerShell script failed
  exit /b 1
)
