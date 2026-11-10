#Requires -Version 5.1
<#
.SYNOPSIS
  LS・Aegis 本地预构建脚本：Maven 打 boot jar + 前端 dist（可选）

.DESCRIPTION
  供本地开发及 pack.ps1 模式3预验证使用。生产源码包不携带这些产物，服务器仍执行 Docker 多阶段构建。

.EXAMPLE
  .\scripts\build-artifacts.ps1
  .\scripts\build-artifacts.ps1 -SkipWeb -SkipH5
#>
[CmdletBinding()]
param(
  [switch]$SkipServer,
  [switch]$SkipWeb,
  [switch]$SkipH5
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

function Write-Step($m) {
  Write-Host ""
  Write-Host "=== $m ===" -ForegroundColor Cyan
}

if (-not $SkipServer) {
  Write-Step "Maven 打包 ls-aegis-boot"
  $mvn = Get-Command mvn -ErrorAction SilentlyContinue
  if (-not $mvn) { throw "未找到 mvn，请安装 Maven 并加入 PATH" }
  Push-Location (Join-Path $Root 'ls-aegis-server')
  try {
    & mvn -B -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败 exit=$LASTEXITCODE" }
    $jar = Get-ChildItem -Path 'ls-aegis-boot\target' -Filter 'ls-aegis-server*.jar' |
      Where-Object { $_.Name -notlike '*.original' } |
      Select-Object -First 1
    if (-not $jar) { throw "未找到 ls-aegis-boot/target/ls-aegis-server*.jar" }
    $binDir = Join-Path $Root 'docker\ls-aegis-server\bin'
    New-Item -ItemType Directory -Force -Path $binDir | Out-Null
    Copy-Item $jar.FullName (Join-Path $binDir 'ls-aegis-server.jar') -Force
    Write-Host "  [OK] $($jar.FullName) -> docker\ls-aegis-server\bin\ls-aegis-server.jar" -ForegroundColor Green
  } finally {
    Pop-Location
  }
}

if (-not $SkipWeb) {
  Write-Step "pnpm build ls-aegis-web"
  $pnpm = Get-Command pnpm -ErrorAction SilentlyContinue
  if (-not $pnpm) { throw "未找到 pnpm，请先安装 Node/pnpm" }
  Push-Location (Join-Path $Root 'ls-aegis-web')
  try {
    if (-not (Test-Path 'node_modules')) {
      & pnpm install
      if ($LASTEXITCODE -ne 0) { throw "pnpm install 失败" }
    }
    & pnpm build
    if ($LASTEXITCODE -ne 0) { throw "ls-aegis-web 构建失败" }
    if (-not (Test-Path 'dist\index.html')) { throw "缺少 ls-aegis-web/dist/index.html" }
    Write-Host "  [OK] ls-aegis-web/dist" -ForegroundColor Green
  } finally {
    Pop-Location
  }
}

if (-not $SkipH5) {
  Write-Step "npm build ls-aegis-h5 (customer + engineer)"
  $npm = Get-Command npm -ErrorAction SilentlyContinue
  if (-not $npm) { throw "未找到 npm" }
  Push-Location (Join-Path $Root 'ls-aegis-h5')
  try {
    if (-not (Test-Path 'node_modules')) {
      & npm install --registry=https://registry.npmmirror.com
      if ($LASTEXITCODE -ne 0) { throw "npm install 失败" }
    }
    & npm run build -- --mode customer
    if ($LASTEXITCODE -ne 0) { throw "H5 customer 构建失败" }
    & npm run build -- --mode engineer
    if ($LASTEXITCODE -ne 0) { throw "H5 engineer 构建失败" }
    Write-Host "  [OK] ls-aegis-h5 dist / dist-engineer" -ForegroundColor Green
  } finally {
    Pop-Location
  }
}

Write-Host ""
Write-Host "构建完成。" -ForegroundColor Green
