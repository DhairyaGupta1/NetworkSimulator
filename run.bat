@echo off
REM 
REM 

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
if errorlevel 1 (
  echo PowerShell script failed
  exit /b 1
)
