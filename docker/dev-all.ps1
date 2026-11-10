# One-shot local stack: MySQL/Redis + backend, wait ready, then PC web + H5
# Usage: powershell -File docker/dev-all.ps1
#    or: docker\dev-all.cmd  /  repo-root: dev.cmd
$ErrorActionPreference = 'Stop'

$DockerDir = $PSScriptRoot
$RepoRoot = Split-Path -Parent $DockerDir
$WebDir = Join-Path $RepoRoot 'ls-aegis-web'
$H5Dir = Join-Path $RepoRoot 'ls-aegis-h5'
$ApiReadyUrl = 'http://127.0.0.1:8000/auth/gm/public-key'
$WaitTimeoutSec = 600
$PollIntervalSec = 3

function Test-BackendReady {
    try {
        $r = Invoke-WebRequest -Uri $ApiReadyUrl -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -eq 200)
    } catch {
        return $false
    }
}

function Wait-BackendReady {
    param([int]$TimeoutSec = 600, [int]$IntervalSec = 3)

    if (Test-BackendReady) {
        Write-Host "Backend already ready on :8000"
        return
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $n = 0
    Write-Host "Waiting for backend ready ($ApiReadyUrl), timeout ${TimeoutSec}s ..."
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds $IntervalSec
        $n++
        if (Test-BackendReady) {
            Write-Host "Backend ready after ~$($n * $IntervalSec)s"
            return
        }
        if (($n % 5) -eq 0) {
            Write-Host "  still waiting... ($($n * $IntervalSec)s)"
        }
    }
    Write-Error "Backend not ready within ${TimeoutSec}s. Check the backend window logs, then re-run or start frontends manually."
}

& (Join-Path $DockerDir 'dev-up.ps1')
if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) {
    # Write-Error already thrown by child when failed
}

$backendCmd = "powershell -NoProfile -ExecutionPolicy Bypass -File `"$DockerDir\dev-run-server.ps1`""
$webCmd = "cd /d `"$WebDir`" && npm run dev"
$h5CustomerCmd = "cd /d `"$H5Dir`" && npm run dev:customer"
$h5EngineerCmd = "cd /d `"$H5Dir`" && npm run dev:engineer"

if (-not (Test-BackendReady)) {
    Write-Host "Opening backend window (port 8000)..."
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $backendCmd -WorkingDirectory $RepoRoot
} else {
    Write-Host "Backend already up; skip starting a new backend window."
}

Wait-BackendReady -TimeoutSec $WaitTimeoutSec -IntervalSec $PollIntervalSec

Write-Host "Opening PC web window (port 5173)..."
Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $webCmd -WorkingDirectory $WebDir

Write-Host "Opening H5 customer window (port 5174)..."
Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $h5CustomerCmd -WorkingDirectory $H5Dir

Write-Host "Opening H5 engineer window (port 5175)..."
Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $h5EngineerCmd -WorkingDirectory $H5Dir

Write-Host ""
Write-Host "Done."
Write-Host "  PC Web:       http://localhost:5173"
Write-Host "  H5 customer:  http://localhost:5174"
Write-Host "  H5 engineer:  http://localhost:5175"
Write-Host "  API:          http://127.0.0.1:8000"
Write-Host "  MySQL:        127.0.0.1:3306"
Write-Host "  Redis:        127.0.0.1:6379"
Write-Host "Close the new windows to stop apps; middleware stays up."
