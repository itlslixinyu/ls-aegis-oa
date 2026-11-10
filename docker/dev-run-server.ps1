# Run backend on host (dev + Spring DevTools), using Docker MySQL/Redis
# Working dir aligns with container /app so files + GM keys reuse docker/data/ls-aegis-server
# Usage: powershell -File docker/dev-run-server.ps1
$ErrorActionPreference = 'Stop'

$DockerDir = $PSScriptRoot
$RepoRoot = Split-Path -Parent $DockerDir
$ServerDir = Join-Path $RepoRoot 'ls-aegis-server'
$DataRoot = Join-Path $DockerDir 'data\ls-aegis-server'
$EnvFile = Join-Path $DockerDir '.env'

& (Join-Path $DockerDir 'ensure-env.ps1')
if (-not (Test-Path $EnvFile)) {
    Write-Error "Missing docker/.env after ensure-env.ps1"
}
if (-not (Test-Path $ServerDir)) {
    Write-Error "ls-aegis-server not found: $ServerDir"
}

@(
    (Join-Path $DataRoot 'data\file'),
    (Join-Path $DataRoot 'data\gm'),
    (Join-Path $DataRoot 'logs'),
    (Join-Path $DataRoot 'config')
) | ForEach-Object {
    if (-not (Test-Path $_)) {
        New-Item -ItemType Directory -Path $_ -Force | Out-Null
    }
}

Get-Content $EnvFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) { return }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()
    if ($val.StartsWith('"') -and $val.EndsWith('"')) {
        $val = $val.Substring(1, $val.Length - 2)
    }
    Set-Item -Path "Env:$key" -Value $val
}

$env:GM_KEY_STORE_PATH = (Join-Path $DataRoot 'data\gm\keys.properties')
$env:DB_HOST = '127.0.0.1'
$env:REDIS_HOST = '127.0.0.1'
if (-not $env:APPLICATION_URL -or $env:APPLICATION_URL -match '8080') {
    $env:APPLICATION_URL = 'http://localhost:5173'
}

# TOKEN_SECRET: ensure-env.ps1 fills empty .env; strength check only here
if (-not $env:TOKEN_SECRET -or $env:TOKEN_SECRET.Trim().Length -lt 32) {
    Write-Error "TOKEN_SECRET in docker/.env is missing or shorter than 32 chars"
}

Write-Host "Working directory (align /app): $DataRoot"
Write-Host "GM key store: $env:GM_KEY_STORE_PATH"
Write-Host "Backend port: 8000 (Vite proxy in .env.development)"
Write-Host "Starting (reactor: -pl ls-aegis-boot -am; plugin 3.3.12)..."
Write-Host ""

Set-Location $ServerDir

# Stay in reactor (-am) so ${revision} resolves; pin plugin 3.3.12; parent/other modules skip=true
mvn -pl ls-aegis-boot -am -DskipTests org.springframework.boot:spring-boot-maven-plugin:3.3.12:run "-Dspring-boot.run.workingDirectory=$DataRoot"
if ($LASTEXITCODE -ne 0) {
    Write-Error "spring-boot:run failed (exit $LASTEXITCODE)"
}
