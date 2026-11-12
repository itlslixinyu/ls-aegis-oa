#requires -RunAsAdministrator
param(
    [Parameter(Mandatory=$true)]
    [string]$BindIp,
    [Parameter(Mandatory=$true)]
    [string]$RemoteSubnet
)
$ErrorActionPreference = 'Stop'
$parsed = [System.Net.IPAddress]::Parse($BindIp)
if ($parsed.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork) { throw 'Only IPv4 is supported.' }
$octets = $parsed.GetAddressBytes()
$expectedSubnet = '{0}.{1}.{2}.0/24' -f $octets[0],$octets[1],$octets[2]
if ($RemoteSubnet -ne $expectedSubnet) { throw 'Only the selected interface /24 subnet may be allowed.' }
$nic = Get-NetIPAddress -AddressFamily IPv4 -IPAddress $BindIp | Select-Object -First 1
if (-not $nic -or $nic.PrefixLength -ne 24) { throw 'The selected address must belong to a local /24 interface.' }
$name = 'LS-Aegis-OA-Mobile-LAN'
$rule = Get-NetFirewallRule -Name $name -ErrorAction SilentlyContinue
if ($rule) {
    $ports = @($rule | Get-NetFirewallPortFilter | Select-Object -ExpandProperty LocalPort)
    $addresses = $rule | Get-NetFirewallAddressFilter
    if (($ports -join ',') -ne '8443,8444' -or $addresses.RemoteAddress -notcontains $RemoteSubnet -or $addresses.LocalAddress -notcontains $BindIp) {
        throw 'An existing rule differs; review it instead of widening access.'
    }
} else {
    $rule = New-NetFirewallRule -Name $name -DisplayName 'LS Aegis OA Mobile LAN HTTPS' `
        -Direction Inbound -Action Allow -Protocol TCP -LocalAddress $BindIp -LocalPort 8443,8444 `
        -RemoteAddress $RemoteSubnet -Profile Any -InterfaceAlias $nic.InterfaceAlias
}
$out = Join-Path (Split-Path $PSScriptRoot -Parent) '.local/mobile-lan/firewall.json'
[ordered]@{status='PASS';rule=$name;localAddress=$BindIp;ports=@(8443,8444);remoteSubnet=$RemoteSubnet;interface=$nic.InterfaceAlias} |
    ConvertTo-Json | Set-Content -LiteralPath $out -Encoding utf8
