# Stop MySQL/Redis for local dev (keep volumes)
# Usage: powershell -File docker/dev-down.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

docker compose -f docker-compose.yml -f docker-compose.dev.yml stop
Write-Host "Stopped MySQL / Redis (volumes kept). Restart: powershell -File docker/dev-up.ps1"
