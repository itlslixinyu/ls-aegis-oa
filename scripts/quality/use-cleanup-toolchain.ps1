# Dot-source this script to use the verified project-local JDK21 and pnpm9 in this process.
$ErrorActionPreference = 'Stop'
$CleanupRepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$CleanupTools = Join-Path $CleanupRepoRoot '.local\toolchains'
$CleanupJdk = (Get-Content -LiteralPath (Join-Path $CleanupTools 'jdk21-home.txt') -Raw).Trim()
$CleanupPnpm = (Get-Content -LiteralPath (Join-Path $CleanupTools 'pnpm9-cli.txt') -Raw).Trim()
$CleanupNodeHomeFile = Join-Path $CleanupTools 'node22-home.txt'
if (Test-Path -LiteralPath $CleanupNodeHomeFile) {
    $CleanupNodeHome = (Get-Content -LiteralPath $CleanupNodeHomeFile -Raw).Trim()
    $CleanupNodeCandidate = Join-Path $CleanupNodeHome 'node.exe'
    if (-not (Test-Path -LiteralPath $CleanupNodeCandidate) -or (& $CleanupNodeCandidate --version) -notmatch '^v22\.') {
        throw '项目本地Node22指针无效；未使用其他版本替代'
    }
    $env:PATH = $CleanupNodeHome + ';' + $env:PATH
}
$CleanupNodeExecutable = (Get-Command node -ErrorAction Stop).Source
if (-not (Test-Path -LiteralPath (Join-Path $CleanupJdk 'bin\javac.exe')) -or -not (Test-Path -LiteralPath $CleanupPnpm)) {
    throw '项目本地工具链不完整，请先安装并校验JDK21与pnpm9；未更改当前环境'
}
$CleanupJavaVersion = (& (Join-Path $CleanupJdk 'bin\java.exe') -version 2>&1 | Out-String)
$CleanupPnpmVersion = (& $CleanupNodeExecutable $CleanupPnpm --version | Out-String).Trim()
if ($CleanupJavaVersion -notmatch 'version "21\.' -or $CleanupPnpmVersion -notmatch '^9\.') {
    throw '工具链版本不匹配；未更改当前环境'
}
$env:JAVA_HOME = $CleanupJdk
$env:PATH = (Join-Path $CleanupJdk 'bin') + ';' + (Join-Path $CleanupTools 'bin') + ';' + $env:PATH
# 旧本机pnpm.cmd可能固定系统Node；当前PowerShell入口使用已选择的Node。
function pnpm { & $CleanupNodeExecutable $CleanupPnpm @args }
Write-Output "当前进程已使用JDK21 / Node $(& $CleanupNodeExecutable --version) / pnpm $CleanupPnpmVersion；未修改系统或用户全局环境"
