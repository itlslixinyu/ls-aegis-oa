"""Prepare private same-LAN TLS material and a narrowly scoped Android debug trust resource."""
import argparse
from pathlib import Path
from datetime import datetime, timedelta, timezone
import ipaddress
import json
import os
import re
import subprocess
from cryptography import x509
from cryptography.x509.oid import NameOID, ExtendedKeyUsageOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ip', required=True)
    args = parser.parse_args()
    ip = ipaddress.ip_address(args.ip)
    assert ip.version == 4 and any(ip in ipaddress.ip_network(n) for n in ['10.0.0.0/8', '172.16.0.0/12', '192.168.0.0/16']), 'Only an explicit private LAN IPv4 address is allowed'
    root = Path(__file__).resolve().parents[1]
    out = root / '.local/mobile-lan'; tls = out / 'tls'; resources = out / 'android-res'
    tls.mkdir(parents=True, exist_ok=True)
    if os.name == 'nt':
        identity = subprocess.check_output(['whoami', '/user', '/fo', 'csv', '/nh'], encoding='utf-8', errors='replace')
        sid = re.search(r'S-1-5-21-(?:\d+-){2}\d+-\d+', identity)
        assert sid, 'Cannot resolve current owner SID; private TLS material was not generated'
        acl = subprocess.run(['icacls', str(tls), '/inheritance:r', '/grant:r',
                              f'*{sid[0]}:(OI)(CI)F', '*S-1-5-18:(OI)(CI)F', '*S-1-5-32-544:(OI)(CI)F'],
                             capture_output=True)
        assert acl.returncode == 0, 'Cannot restrict private TLS directory permissions'
        for existing in tls.iterdir():
            if existing.is_file():
                acl = subprocess.run(['icacls',str(existing),'/inheritance:r','/grant:r',
                                      f'*{sid[0]}:F','*S-1-5-18:F','*S-1-5-32-544:F'],capture_output=True)
                assert acl.returncode == 0, 'Cannot restrict existing TLS file permissions'
    for name in ['raw', 'xml']: (resources / name).mkdir(parents=True, exist_ok=True)
    now = datetime.now(timezone.utc)
    if (tls / 'ca.key').exists():
        key = serialization.load_pem_private_key((tls / 'ca.key').read_bytes(), password=None)
        ca = x509.load_pem_x509_certificate((tls / 'ca.crt').read_bytes())
        assert ca.not_valid_after_utc > now + timedelta(days=30), 'LAN CA renewal required'
    else:
        assert not (tls / 'ca.crt').exists(), 'Existing CA certificate has no key; do not silently replace trust'
        key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'LS Aegis OA local mobile CA')])
        ca = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
              .serial_number(x509.random_serial_number()).not_valid_before(now-timedelta(minutes=5))
              .not_valid_after(now+timedelta(days=1825)).add_extension(x509.BasicConstraints(ca=True,path_length=0),critical=True)
              .add_extension(x509.KeyUsage(False,False,False,False,False,True,True,False,False),critical=True)
              .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()),critical=False).sign(key,hashes.SHA256()))
        (tls / 'ca.key').write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        (tls / 'ca.crt').write_bytes(ca.public_bytes(serialization.Encoding.PEM))
    if (tls / 'mobile.crt').exists():
        leaf=x509.load_pem_x509_certificate((tls/'mobile.crt').read_bytes())
        assert ip in leaf.extensions.get_extension_for_class(x509.SubjectAlternativeName).value.get_values_for_type(x509.IPAddress), 'Existing certificate is for another address; preserve it and plan address migration'
        assert leaf.not_valid_after_utc > now+timedelta(days=7), 'Server certificate renewal required'
        assert (tls/'mobile.key').exists(), 'Missing existing server key'
    else:
        server_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        leaf=(x509.CertificateBuilder().subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,str(ip))]))
              .issuer_name(ca.subject).public_key(server_key.public_key()).serial_number(x509.random_serial_number())
              .not_valid_before(now-timedelta(minutes=5)).not_valid_after(now+timedelta(days=365))
              .add_extension(x509.BasicConstraints(ca=False,path_length=None),critical=True)
              .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ip)]),critical=False)
              .add_extension(x509.SubjectKeyIdentifier.from_public_key(server_key.public_key()),critical=False)
              .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(key.public_key()),critical=False)
              .add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]),critical=False)
              .add_extension(x509.KeyUsage(True,False,True,False,False,False,False,False,False),critical=True)
              .sign(key,hashes.SHA256()))
        (tls/'mobile.key').write_bytes(server_key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        (tls/'mobile.crt').write_bytes(leaf.public_bytes(serialization.Encoding.PEM))
    (resources/'raw/mobile_lan_ca.pem').write_bytes(ca.public_bytes(serialization.Encoding.PEM))
    (resources/'xml/mobile_lan_network_security_config.xml').write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors><certificates src="system" /></trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="false">{ip}</domain>
        <trust-anchors><certificates src="@raw/mobile_lan_ca" /></trust-anchors>
    </domain-config>
</network-security-config>
''',encoding='utf-8')
    config={'bindIp':str(ip),'tlsDir':str(tls),'androidResources':str(resources),
            'customerUrl':f'https://{ip}:8443/customer/auth','engineerUrl':f'https://{ip}:8444/engineer/login'}
    (out/'profile.json').write_text(json.dumps(config,indent=2),encoding='utf-8')
    print('LAN HTTPS certificate and host-scoped App trust resources ready; private keys remain under .local/mobile-lan/tls')

if __name__=='__main__': main()
