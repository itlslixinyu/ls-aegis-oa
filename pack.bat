@echo off
setlocal EnableExtensions
set "SD=%~dp0"
cd /d "%SD%"

set "PACK_INTERACTIVE=1"
set "PACK_SHOW_HELP=0"
:parse
if "%~1"=="" goto :done
if /I "%~1"=="-batch" set "PACK_INTERACTIVE=0"
if /I "%~1"=="-help" (set "PACK_SHOW_HELP=1" & set "PACK_INTERACTIVE=0")
if "%~1"=="-?" (set "PACK_SHOW_HELP=1" & set "PACK_INTERACTIVE=0")
shift
goto :parse
:done

if not exist "pack.ps1" (
  echo [ERROR] pack.ps1 not found:
  echo   %SD%
  if "%PACK_INTERACTIVE%"=="1" pause
  exit /b 1
)

if "%PACK_SHOW_HELP%"=="1" goto :help
PowerShell -NoProfile -NoLogo -ExecutionPolicy Bypass -File "%SD%pack.ps1" %*
goto :result
:help
PowerShell -NoProfile -NoLogo -ExecutionPolicy Bypass -File "%SD%pack.ps1" -?
:result
set "EC=%ERRORLEVEL%"

echo.
if "%EC%"=="0" (echo === Pack OK ===) else (echo === Pack FAILED exit=%EC% ===)
echo.
if "%PACK_INTERACTIVE%"=="1" pause
exit /b %EC%
