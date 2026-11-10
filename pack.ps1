#Requires -Version 5.1
<#
.SYNOPSIS
  LS·Aegis OA 一键打包工具，版本取自根目录 VERSION。

.DESCRIPTION
  向导询问服务器连接、域名、构建模式等；产物 ls-aegis-deploy.tar.gz。
  回答缓存到 pack.config.json（勿提交 Git）。

.PARAMETER Batch
  跳过向导，沿用 pack.config.json 或命令行参数。

.PARAMETER Rebuild
  先本地验证构建再打源码包；服务器仍从源码构建镜像。

.PARAMETER NoBuild
  源码直打（服务器端 Docker 构建），跳过本地 Maven/npm。

.EXAMPLE
  .\pack.ps1
  .\pack.ps1 -Batch -NoBuild
#>
[CmdletBinding()]
param(
  [switch]$Batch,
  [switch]$Rebuild,
  [switch]$NoBuild,
  [string]$Server,
  [string]$SshUser,
  [int]$SshPort = 0,
  [string]$Domain,
  [string]$DomainH5,
  [string]$DomainH5Engineer,
  [string]$DeployDir,
  [string]$Output,
  [switch]$CopyClipboard,
  [switch]$AutoUpload
)

$ErrorActionPreference = 'Stop'

try {
  [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
  $OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

try {

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = [IO.Path]::GetFullPath($ScriptDir)
$ParentDir  = Split-Path -Parent $ProjectDir
$FolderName = Split-Path -Leaf   $ProjectDir
$ConfigFile = Join-Path $ProjectDir 'pack.config.json'
$VersionFile = Join-Path $ProjectDir 'VERSION'
if (-not (Test-Path -LiteralPath $VersionFile -PathType Leaf)) {
  throw '缺少根目录 VERSION，拒绝生成无版本部署包。'
}
$ProjectVersion = [IO.File]::ReadAllText($VersionFile).Trim()
if ($ProjectVersion -notmatch '^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$') {
  throw 'VERSION 必须是有效的三位产品版本号。'
}

function Write-Heading($t) {
  Write-Host ""
  Write-Host "============================================================" -ForegroundColor Cyan
  Write-Host ("  " + $t) -ForegroundColor Cyan
  Write-Host "============================================================" -ForegroundColor Cyan
}
function Write-Line($c, $label, $value) {
  Write-Host "  " -NoNewline
  Write-Host ("{0,-22}" -f ($label + ':')) -ForegroundColor White -NoNewline
  Write-Host (" $value") -ForegroundColor $c
}
function Read-Default($prompt, $default) {
  if ([string]::IsNullOrWhiteSpace($default)) { $d = '' } else { $d = " [$default]" }
  $input = Read-Host ("  {0}{1}" -f $prompt, $d)
  if ([string]::IsNullOrWhiteSpace($input)) { return $default }
  return $input.ToString().Trim()
}
function Read-YesNo($prompt, [bool]$default) {
  $dchar = if ($default) { 'Y/n' } else { 'y/N' }
  while ($true) {
    $input = Read-Host ("  {0} ({1})" -f $prompt, $dchar)
    if ([string]::IsNullOrWhiteSpace($input)) { return $default }
    switch -Wildcard ($input.Trim().ToLowerInvariant()) {
      'y*' { return $true  }
      'n*' { return $false }
      default { Write-Host "    请输入 Y 或 N" -ForegroundColor DarkYellow }
    }
  }
}
function Read-IntDefault($prompt, [int]$default) {
  while ($true) {
    $s = Read-Default $prompt $default
    $v = 0
    if ([int]::TryParse($s, [ref]$v) -and $v -gt 0 -and $v -lt 65536) { return $v }
    Write-Host "    端口无效，请输入 1-65535 之间的数字" -ForegroundColor DarkYellow
  }
}
function Read-BuildMode([int]$default) {
  Write-Host "  构建模式："
  Write-Host "    1 - 源码直打（服务器端 Docker 构建）  <- 首次推荐"
  Write-Host "    3 - 先本地验证构建，再打源码包（服务器仍构建镜像）"
  while ($true) {
    $s = Read-Default "  请选择构建模式 (1/3)" $default
    if ($s -in @('1','3')) { return [int]$s }
    Write-Host "    请输入 1 或 3" -ForegroundColor DarkYellow
  }
}

Write-Heading "LS·Aegis OA V$ProjectVersion - 一键打包部署向导"
Write-Line DarkGray "项目目录" $ProjectDir
Write-Line DarkGray "项目名称" $FolderName
Write-Line DarkGray "配置文件" $(if (Test-Path $ConfigFile) { "已存在" } else { "将创建" })

$cfg = [ordered]@{
  Server            = '49.233.191.75'
  SshPort       = 22
  SshUser       = 'ubuntu'
  Domain            = 'oa.ls.hb.cn'
  DomainH5          = 'kh.ls.hb.cn'
  DomainH5Engineer  = 'gc.ls.hb.cn'
  DeployDir     = '/opt'
  BuildMode     = 1
  CopyClipboard = $true
  AutoUpload    = $false
}
if (Test-Path $ConfigFile) {
  try {
    $saved = Get-Content $ConfigFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $savedDict = @{}
    foreach ($p in $saved.PSObject.Properties) { $savedDict[$p.Name] = $p.Value }
    foreach ($k in @($cfg.Keys)) {
      if ($savedDict.ContainsKey($k) -and $null -ne $savedDict[$k]) {
        if ($cfg[$k] -is [bool])    { $cfg[$k] = [bool]$savedDict[$k] }
        elseif ($cfg[$k] -is [int]) { $cfg[$k] = [int]$savedDict[$k] }
        else                         { $cfg[$k] = [string]$savedDict[$k] }
      }
    }
  } catch {
    Write-Host "  [警告] 忽略损坏的 $ConfigFile : $($_.Exception.Message)" -ForegroundColor DarkYellow
  }
}

if (-not [string]::IsNullOrWhiteSpace($Server))    { $cfg.Server    = $Server }
if (-not [string]::IsNullOrWhiteSpace($SshUser))   { $cfg.SshUser   = $SshUser }
if ($SshPort -gt 0)                                { $cfg.SshPort   = $SshPort }
if (-not [string]::IsNullOrWhiteSpace($Domain))            { $cfg.Domain           = $Domain }
if (-not [string]::IsNullOrWhiteSpace($DomainH5))          { $cfg.DomainH5         = $DomainH5 }
if (-not [string]::IsNullOrWhiteSpace($DomainH5Engineer))  { $cfg.DomainH5Engineer = $DomainH5Engineer }
if (-not [string]::IsNullOrWhiteSpace($DeployDir)) { $cfg.DeployDir = $DeployDir }
if ($PSBoundParameters.ContainsKey('CopyClipboard')){ $cfg.CopyClipboard = [bool]$CopyClipboard }
if ($PSBoundParameters.ContainsKey('AutoUpload'))   { $cfg.AutoUpload    = [bool]$AutoUpload }

if     ($Rebuild)  { $cfg.BuildMode = 3 }
elseif ($NoBuild)  { $cfg.BuildMode = 1 }

# 旧模式2从未将本地产物交给生产Compose，迁移到实际采用的源码流程。
if ($cfg.BuildMode -eq 2) {
  Write-Host '  [兼容] 旧模式2已改为源码部署；服务器将构建镜像。' -ForegroundColor DarkYellow
  $cfg.BuildMode = 1
}
if ($cfg.BuildMode -notin @(1, 3)) { throw '构建模式仅支持1（源码）或3（先本地验证）。' }

if (-not $Batch) {
  Write-Heading "第 1/5 步 - 服务器连接信息"
  $cfg.Server    = Read-Default   "服务器 IP / 主机名" $cfg.Server
  $cfg.SshPort   = Read-IntDefault "SSH 端口"          $cfg.SshPort
  $cfg.SshUser   = Read-Default   "SSH 用户名"         $cfg.SshUser
  $cfg.DeployDir = Read-Default   "服务器部署目录"      $cfg.DeployDir

  Write-Heading "第 2/5 步 - 域名与 HTTPS"
  $cfg.Domain           = Read-Default "PC 后台域名" $cfg.Domain
  $cfg.DomainH5         = Read-Default "H5 客户域名" $cfg.DomainH5
  $cfg.DomainH5Engineer = Read-Default "H5 工程师域名" $cfg.DomainH5Engineer

  Write-Heading "第 3/5 步 - 构建模式"
  if (-not ($Rebuild -or $NoBuild)) {
    $cfg.BuildMode = Read-BuildMode $cfg.BuildMode
  } else {
    $label = @{1='源码直打（已设置 -NoBuild）'; 3='先本地验证再打源码包（已设置 -Rebuild）'}[[int]$cfg.BuildMode]
    Write-Host "  （已被命令行参数覆盖: $label）" -ForegroundColor DarkGray
  }

  Write-Heading "第 4/5 步 - 打包后操作"
  $cfg.CopyClipboard = Read-YesNo "复制服务器命令到剪贴板" ([bool]$cfg.CopyClipboard)
  $cfg.AutoUpload    = Read-YesNo "自动 SCP 上传到服务器" ([bool]$cfg.AutoUpload)
}

$ModeDesc = @{
  1 = '1 - 源码直打（服务器端 Docker 构建）'
  3 = '3 - 先本地验证构建，再打源码包'
}[[int]$cfg.BuildMode]

Write-Heading "第 5/5 步 - 确认信息"
Write-Line Green "服务器"       "$($cfg.SshUser)@$($cfg.Server):$($cfg.SshPort)"
Write-Line Green "部署目录"     $cfg.DeployDir
Write-Line Green "PC 域名"     ("https://" + $cfg.Domain)
Write-Line Green "H5 客户"     ("https://" + $cfg.DomainH5)
Write-Line Green "H5 工程师"   ("https://" + $cfg.DomainH5Engineer)
Write-Line Green "构建模式"     $ModeDesc
Write-Line Green "复制到剪贴板" $cfg.CopyClipboard
Write-Line Green "自动上传"     $cfg.AutoUpload
if ([string]::IsNullOrWhiteSpace($Output)) {
  $Output = Join-Path $ParentDir 'ls-aegis-deploy.tar.gz'
}
Write-Line Green "输出文件" $Output

if (-not $Batch) {
  if (-not (Read-YesNo "`n确认以上信息并开始打包?" $true)) {
    Write-Host "已取消。" -ForegroundColor Yellow
    exit 1
  }
}

try {
  $cfg | ConvertTo-Json -Depth 3 | Set-Content $ConfigFile -Encoding UTF8
} catch {
  Write-Host "  [警告] 无法保存 $ConfigFile : $($_.Exception.Message)" -ForegroundColor DarkYellow
}

Write-Heading "打包阶段 1/4 - 环境预检"
$tarExe = Get-Command tar.exe -ErrorAction SilentlyContinue
if (-not $tarExe) { throw "未找到 tar.exe，Windows 10/11 自带 bsdtar，请确认已启用" }
Write-Host "  [OK] $($tarExe.Source)" -ForegroundColor Green

if ($cfg.AutoUpload) {
  if (-not (Get-Command scp.exe -ErrorAction SilentlyContinue)) {
    throw "选择了自动上传但未找到 scp.exe，请安装 OpenSSH 客户端或改用 -AutoUpload:`$false"
  }
}

Write-Heading "打包阶段 2/4 - 构建产物 (模式=$($cfg.BuildMode))"
switch ([int]$cfg.BuildMode) {
  1 {
    Write-Host "  [跳过] 源码模式：服务器端 Docker 多阶段构建" -ForegroundColor DarkGray
  }
  3 {
    $build = Join-Path $ProjectDir 'scripts\build-artifacts.ps1'
    Write-Host "  正在本地验证构建（需数分钟）；服务器仍将从源码构建..." -ForegroundColor Yellow
    & $build
    if ($LASTEXITCODE -ne 0 -or -not $?) { throw "build-artifacts.ps1 构建失败" }
  }
}

$DeployInfoFile = Join-Path $ProjectDir 'scripts\deploy-info.json'
$deployInfo = [ordered]@{
  Version     = $ProjectVersion
  GeneratedAt = (Get-Date).ToString('s')
  Server      = $cfg.Server
  SshPort     = [int]$cfg.SshPort
  SshUser     = $cfg.SshUser
  Domain            = $cfg.Domain
  DomainH5          = $cfg.DomainH5
  DomainH5Engineer  = $cfg.DomainH5Engineer
  DeployDir   = $cfg.DeployDir
  BuildMode   = [int]$cfg.BuildMode
}
New-Item -ItemType Directory -Force -Path (Split-Path $DeployInfoFile) | Out-Null
$deployInfo | ConvertTo-Json -Depth 3 | Set-Content $DeployInfoFile -Encoding UTF8
Write-Host "  [OK] 已写入 scripts\deploy-info.json" -ForegroundColor DarkGray


# Normalize shell scripts to LF (Linux bash rejects CRLF)
function Normalize-ShellScripts([string]$root) {
  $dir = Join-Path $root 'scripts'
  if (-not (Test-Path $dir)) { return }
  Get-ChildItem $dir -Filter '*.sh' -File | ForEach-Object {
    $t = [IO.File]::ReadAllText($_.FullName)
    $n = $t -replace "`r`n", "`n" -replace "`r", "`n"
    if ($n -ne $t) {
      $utf8 = New-Object System.Text.UTF8Encoding $false
      [IO.File]::WriteAllText($_.FullName, $n, $utf8)
    }
  }
}
Normalize-ShellScripts $ProjectDir
Write-Heading "打包阶段 3/4 - 创建 tarball"
if (Test-Path $Output) { Remove-Item $Output -Force }

$excludeList = @(
  '.git', '.idea', '.vscode', '.trae', '.history', '.cursor',
  '.local', '.agents', 'agents', 'rules', 'skills', 'Docs', 'android-demo', '__pycache__', '*.pyc', '*.pyo',
  '.env', 'docker/.env', 'docker-compose.override.yml',
  '.env.local', '.env.*.local',
  'node_modules', 'target', 'dist', 'dist-engineer', 'dist-customer',
  'backups', 'temp', 'tmp', 'logs',
  'docker/data', 'docker/ls-aegis-server/bin',
  '*.iml', '*.log', '*.tmp', '*.pid', '*.hprof',
  'ls-aegis-deploy.tar.gz', 'esign-deploy.tar.gz',
  'pack.config.json'
)

$tarArgs = @('-czf', $Output)
foreach ($ex in $excludeList) { $tarArgs += "--exclude=$ex" }
$tarArgs += $FolderName

Write-Host "  正在执行 tar.exe -czf ...（请稍候）" -ForegroundColor Yellow
Push-Location $ParentDir
try {
  & $tarExe.Source @tarArgs
  if ($LASTEXITCODE -ne 0) { throw "tar.exe 执行失败 (exit=$LASTEXITCODE)" }
} finally {
  Pop-Location
}

Write-Heading "打包阶段 4/4 - 校验 tarball"
if (-not (Test-Path $Output)) { throw "输出文件不存在: $Output" }
$f    = Get-Item $Output
$szMB = [math]::Round($f.Length / 1MB, 2)
$all  = @(& $tarExe.Source -tzf $Output 2>$null)
$nF   = ($all | Where-Object { $_ -notmatch '/$' }).Count
$haveDc  = [bool]($all | Where-Object { $_ -match '[\\/]docker-compose\.yml$' } | Select-Object -First 1)
$haveIns = [bool]($all | Where-Object { $_ -match '[\\/]scripts[\\/]install\.sh$' } | Select-Object -First 1)
$haveInfo= [bool]($all | Where-Object { $_ -match '[\\/]scripts[\\/]deploy-info\.json$' } | Select-Object -First 1)
$haveVersion = [bool]($all | Where-Object { ($_ -replace '\\', '/') -eq "$FolderName/VERSION" } | Select-Object -First 1)

Write-Host "  文件名       : $($f.Name)"
Write-Host "  大小         : $($f.Length) 字节 ($szMB MB)" -ForegroundColor Green
Write-Host "  文件数       : $nF" -ForegroundColor Green
if ($haveDc)  { Write-Host "  [OK] docker-compose.yml 已包含" -ForegroundColor Green } else { throw "tarball 缺少 docker-compose.yml" }
if ($haveIns) { Write-Host "  [OK] scripts/install.sh 已包含" -ForegroundColor Green } else { throw "tarball 缺少 scripts/install.sh" }
if ($haveInfo){ Write-Host "  [OK] scripts/deploy-info.json 已包含" -ForegroundColor Green } else { throw 'tarball 缺少版本部署元数据' }
if ($haveVersion) { Write-Host "  [OK] VERSION ($ProjectVersion) 已包含" -ForegroundColor Green } else { throw 'tarball 缺少根目录 VERSION' }

$ScpTarget = "/tmp/ls-aegis-deploy.tar.gz"
$ScpCmd = "scp -P $($cfg.SshPort) `"$Output`" $($cfg.SshUser)@$($cfg.Server):$ScpTarget"
$UnpackDir = if ($cfg.DeployDir.TrimEnd('/') -eq '') { '/' } else { $cfg.DeployDir.TrimEnd('/') }
$CmdRoot = "mkdir -p $UnpackDir && mv /tmp/ls-aegis-deploy.tar.gz $UnpackDir/ && cd $UnpackDir && tar -xzf ls-aegis-deploy.tar.gz && cd $FolderName && bash scripts/install.sh"
$CmdSudo = "sudo mkdir -p $UnpackDir && sudo mv /tmp/ls-aegis-deploy.tar.gz $UnpackDir/ && cd $UnpackDir && sudo tar -xzf ls-aegis-deploy.tar.gz && cd $FolderName && sudo -E bash scripts/install.sh"
$ServerOneLiner = 'if [ "$(id -u)" = "0" ]; then ' + $CmdRoot + '; else ' + $CmdSudo + '; fi'

Write-Heading "部署说明"
Write-Host ""
Write-Host "  --- 第 1 步：上传到服务器 ---"
Write-Host "  方式 A (SCP):"
Write-Host "    $ScpCmd"
Write-Host "  方式 B (宝塔面板):"
Write-Host "    将 $(Split-Path -Leaf $Output) 上传到 /tmp/ 目录"
Write-Host ""
Write-Host "  --- 第 2 步：在服务器上执行 ---"
Write-Host "  $ServerOneLiner" -ForegroundColor Cyan
Write-Host ""
Write-Host "  PC:        https://$($cfg.Domain)"
Write-Host "  H5 客户:   https://$($cfg.DomainH5)"
Write-Host "  H5 工程师: https://$($cfg.DomainH5Engineer)"

if ([bool]$cfg.CopyClipboard -and (Get-Command Set-Clipboard -ErrorAction SilentlyContinue)) {
  try {
    Set-Clipboard -Value $ServerOneLiner
    Write-Host "  [提示] 服务器命令已复制到剪贴板" -ForegroundColor Green
  } catch {}
}

if ([bool]$cfg.AutoUpload) {
  Write-Heading "自动上传到 $($cfg.SshUser)@$($cfg.Server):$($cfg.SshPort)"
  Write-Host "  [1/2] 上传 tar.gz 到 /tmp/ ..." -ForegroundColor Yellow
  & scp.exe -P $cfg.SshPort -o StrictHostKeyChecking=accept-new $Output "$($cfg.SshUser)@$($cfg.Server):$ScpTarget"
  if ($LASTEXITCODE -ne 0) { throw "scp 上传失败" }
  Write-Host "  [OK] 上传完成" -ForegroundColor Green
  Write-Host "  [2/2] SSH 远程执行部署 ..." -ForegroundColor Yellow
  & ssh.exe -p $cfg.SshPort -o StrictHostKeyChecking=accept-new "$($cfg.SshUser)@$($cfg.Server)" $ServerOneLiner
  if ($LASTEXITCODE -ne 0) {
    Write-Host "  [警告] SSH 远程部署可能未完全成功 (exit=$LASTEXITCODE)" -ForegroundColor DarkYellow
  } else {
    Write-Host "  [OK] 远程部署命令已执行完毕" -ForegroundColor Green
  }
}

Write-Heading "打包完成"
if (-not $Batch) {
  Write-Host ""
  Write-Host "  按任意键退出..." -ForegroundColor DarkGray
  try { [void][Console]::ReadKey($true) } catch {}
}
exit 0

} catch {
  Write-Host ""
  Write-Host "============================================================" -ForegroundColor Red
  Write-Host "  [错误] $($_.Exception.Message)" -ForegroundColor Red
  Write-Host "============================================================" -ForegroundColor Red
  Write-Host ""
  if (-not $Batch) {
    Write-Host "  按任意键退出..." -ForegroundColor DarkGray
    try { [void][Console]::ReadKey($true) } catch {}
  }
  exit 1
}
