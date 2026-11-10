# Daily dev: start MySQL + Redis only
# Usage: powershell -File docker/dev-up.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

& (Join-Path $PSScriptRoot 'ensure-env.ps1')

Write-Host "Starting MySQL / Redis (dev mode, no server/web/h5)..."
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d ls-aegis-mysql ls-aegis-redis
if ($LASTEXITCODE -ne 0) {
    Write-Error "docker compose up failed (exit $LASTEXITCODE)"
}

Write-Host ""
Write-Host "Middleware ready:"
Write-Host "  MySQL  127.0.0.1:3306"
Write-Host "  Redis  127.0.0.1:6379"
Write-Host ""
Write-Host "Backend:  powershell -File docker/dev-run-server.ps1"
Write-Host "Frontend: cd ls-aegis-web; npm run dev  (5173 -> 8000)"
Write-Host "H5:       cd ls-aegis-h5; npm run dev:customer / dev:engineer  (5174/5175)"
Write-Host "All-in-one:  dev.cmd  (or docker\dev-all.cmd)"
