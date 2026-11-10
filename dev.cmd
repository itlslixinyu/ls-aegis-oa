@echo off
setlocal EnableExtensions
REM ITLS development entry; version comes from the repository VERSION file.
cd /d "%~dp0" || exit /b 1
if not exist "%~dp0VERSION" (
  echo [ERROR] VERSION not found.
  exit /b 1
)
if not exist "%~dp0docker\dev-all.cmd" (
  echo [ERROR] docker\dev-all.cmd not found.
  exit /b 1
)
set "ITLS_DEV_VERSION="
set /p ITLS_DEV_VERSION=<"%~dp0VERSION"
if not defined ITLS_DEV_VERSION exit /b 1
echo ITLS V%ITLS_DEV_VERSION% development
call "%~dp0docker\dev-all.cmd"
exit /b %errorlevel%
