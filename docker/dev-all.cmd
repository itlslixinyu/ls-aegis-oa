@echo off
REM One command: MySQL/Redis + backend + frontend (two new CMD windows)
REM From repo root:  docker\dev-all.cmd
REM Absolute:        d:\itls\docker\dev-all.cmd
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev-all.ps1"
