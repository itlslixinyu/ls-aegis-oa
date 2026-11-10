# Ensure docker/.env exists; fill empty required secrets with random values (never overwrite).
# Called by dev-up.ps1 / dev-run-server.ps1; or: powershell -File docker/ensure-env.ps1
$ErrorActionPreference = 'Stop'

$DockerDir = $PSScriptRoot
$EnvFile = Join-Path $DockerDir '.env'
$ExampleFile = Join-Path $DockerDir '.env.example'

function New-RandomSecret {
    param([int]$ByteLength = 32)
    $bytes = New-Object byte[] $ByteLength
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Get-EnvMap {
    param([string]$Path)
    $map = [ordered]@{}
    if (-not (Test-Path $Path)) { return $map }
    Get-Content -Path $Path -Encoding UTF8 | ForEach-Object {
        $trim = $_.Trim()
        if (-not $trim -or $trim.StartsWith('#')) { return }
        $idx = $trim.IndexOf('=')
        if ($idx -lt 1) { return }
        $key = $trim.Substring(0, $idx).Trim()
        $val = $trim.Substring($idx + 1).Trim()
        if ($val.StartsWith('"') -and $val.EndsWith('"') -and $val.Length -ge 2) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        $map[$key] = $val
    }
    return $map
}

function Set-EnvFileValue {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )
    $lines = @(Get-Content -Path $Path -Encoding UTF8)
    $found = $false
    $out = foreach ($line in $lines) {
        $trim = $line.Trim()
        if ($trim -and -not $trim.StartsWith('#') -and $trim.StartsWith("$Key=")) {
            $found = $true
            "$Key=$Value"
        } else {
            $line
        }
    }
    if (-not $found) {
        $out += "$Key=$Value"
    }
    $text = ($out -join "`r`n") + "`r`n"
    [System.IO.File]::WriteAllText($Path, $text, [System.Text.UTF8Encoding]::new($false))
}

if (-not (Test-Path $ExampleFile)) {
    Write-Error "Missing docker/.env.example"
}

if (-not (Test-Path $EnvFile)) {
    Copy-Item -Path $ExampleFile -Destination $EnvFile
    Write-Host "Created docker/.env from .env.example"
}

$map = Get-EnvMap -Path $EnvFile
$filled = @()

# MySQL root / app DB password: generate if both empty; sync if only one side set
$root = if ($map.Contains('MYSQL_ROOT_PASSWORD')) { $map['MYSQL_ROOT_PASSWORD'] } else { '' }
$db = if ($map.Contains('DB_PWD')) { $map['DB_PWD'] } else { '' }
if ([string]::IsNullOrWhiteSpace($root) -and [string]::IsNullOrWhiteSpace($db)) {
    $gen = New-RandomSecret -ByteLength 24
    Set-EnvFileValue -Path $EnvFile -Key 'MYSQL_ROOT_PASSWORD' -Value $gen
    Set-EnvFileValue -Path $EnvFile -Key 'DB_PWD' -Value $gen
    $filled += 'MYSQL_ROOT_PASSWORD', 'DB_PWD'
} elseif ([string]::IsNullOrWhiteSpace($root) -and -not [string]::IsNullOrWhiteSpace($db)) {
    Set-EnvFileValue -Path $EnvFile -Key 'MYSQL_ROOT_PASSWORD' -Value $db
    $filled += 'MYSQL_ROOT_PASSWORD'
} elseif (-not [string]::IsNullOrWhiteSpace($root) -and [string]::IsNullOrWhiteSpace($db)) {
    Set-EnvFileValue -Path $EnvFile -Key 'DB_PWD' -Value $root
    $filled += 'DB_PWD'
}

$map = Get-EnvMap -Path $EnvFile

$required = @(
    @{ Key = 'REDIS_PWD'; Bytes = 24 },
    @{ Key = 'JWT_SECRET_KEY'; Bytes = 48 },
    @{ Key = 'TOKEN_SECRET'; Bytes = 48 }
)

foreach ($item in $required) {
    $key = $item.Key
    $cur = if ($map.Contains($key)) { $map[$key] } else { '' }
    # Only fill empty values; never overwrite (protect MySQL/Redis volumes and sign ciphertext)
    if ([string]::IsNullOrWhiteSpace($cur)) {
        $gen = New-RandomSecret -ByteLength $item.Bytes
        Set-EnvFileValue -Path $EnvFile -Key $key -Value $gen
        $filled += $key
        $map[$key] = $gen
    }
}

if ($filled.Count -gt 0) {
    Write-Host ("Filled empty secrets in docker/.env: " + (($filled | Select-Object -Unique) -join ', '))
    Write-Host "  (values not printed; file is gitignored)"
}
