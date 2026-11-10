@echo off
setlocal
set "SD=%~dp0"
cd /d "%SD%.."
PowerShell -NoProfile -NoLogo -ExecutionPolicy Bypass -File "%SD%build-artifacts.ps1" %*
exit /b %ERRORLEVEL%
