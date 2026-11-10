"""临时 Nginx 实际 HTTP 检查：压缩、缓存、安全头与缺失资源；不接入开发后端。"""
import argparse
import gzip
import json
from pathlib import Path
import secrets
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]


def docker(*args):
    result = subprocess.run(['docker', *args], capture_output=True, text=True, encoding='utf-8', errors='replace')
    if result.returncode:
        raise RuntimeError(result.stderr[-1500:])
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dist', type=Path, default=ROOT / 'ls-aegis-web/dist', help='只读候选前端产物目录')
    args = parser.parse_args()
    dist = args.dist.resolve()
    if not (dist / 'index.html').is_file() or not (dist / '.vite/manifest.json').is_file():
        parser.error('候选目录必须包含 index.html 和 .vite/manifest.json')
    manifest = json.loads((dist / '.vite/manifest.json').read_text(encoding='utf-8'))
    js = manifest['index.html']['file']
    worker = next(asset for item in manifest.values() for asset in item.get('assets', []) if asset.endswith('.mjs'))
    run_id = secrets.token_hex(5)
    cid = docker('run', '-d', '--name', f'itls-nginx-check-{run_id}', '--label', f'itls.quality.run={run_id}',
                 '--add-host', 'ls-aegis-server:127.0.0.1', '-p', '127.0.0.1::80',
                 '--mount', f'type=bind,source={ROOT / "docker/nginx/conf/nginx.conf"},target=/etc/nginx/nginx.conf,readonly',
                 '--mount', f'type=bind,source={dist},target=/usr/share/nginx/html,readonly', 'nginx:1.27.0')
    results = []
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        docker('exec', cid, 'nginx', '-t')
        results.append('配置语法')
        port = docker('port', cid, '80/tcp').rsplit(':', 1)[1]
        base = f'http://127.0.0.1:{port}'

        def get(path, compressed=False, method='GET'):
            req = urllib.request.Request(base + path, method=method,
                                         headers={'Accept-Encoding': 'gzip' if compressed else 'identity'})
            try:
                response = opener.open(req, timeout=5)
            except urllib.error.HTTPError as exc:
                response = exc
            with response:
                return response.status, response.headers, response.read()

        deadline = time.monotonic() + 30
        while True:
            try:
                status, headers, body = get('/')
                if status == 200:
                    break
            except OSError:
                pass
            if time.monotonic() >= deadline:
                raise RuntimeError('临时 Nginx 启动超时')
            time.sleep(0.2)
        assert b'<html' in body.lower()
        assert headers['Cache-Control'] == 'no-cache'
        assert headers['X-Frame-Options'] == 'SAMEORIGIN'
        assert headers['X-Content-Type-Options'] == 'nosniff'
        results.append('HTML 协商缓存与安全头')

        status, headers, compressed = get('/' + js, compressed=True)
        assert status == 200 and headers['Content-Encoding'] == 'gzip'
        original = (dist / js).read_bytes()
        assert gzip.decompress(compressed) == original
        assert 'Accept-Encoding' in headers['Vary']
        assert 'immutable' in headers['Cache-Control'] and 'max-age=31536000' in headers['Cache-Control']
        assert headers['X-Frame-Options'] == 'SAMEORIGIN'
        assert headers['X-Content-Type-Options'] == 'nosniff'
        results.append('JS 实际 gzip、Vary、哈希缓存与安全头')

        status, headers, _ = get('/' + worker)
        assert status == 200 and 'javascript' in headers['Content-Type']
        assert 'immutable' in headers['Cache-Control']
        results.append('PDF worker 模块 MIME 与缓存')
        status, headers, _ = get('/static/js/itls-absent-resource.js')
        assert status == 404 and 'immutable' not in headers.get('Cache-Control', '')
        results.append('缺失资源 404 且不长期缓存错误页')
        status, headers, body = get('/quote/index')
        assert status == 200 and b'<html' in body.lower() and headers['Cache-Control'] == 'no-cache'
        results.append('SPA 路由回退且入口不长期缓存')
        status, headers, body = get('/', method='HEAD')
        assert status == 200 and not body and headers['ETag'] and headers['Cache-Control'] == 'no-cache'
        results.append('版本探测 HEAD 与 ETag')
        report = {'checks': results, 'rawJsBytes': len(original), 'gzipJsBytes': len(compressed),
                  'environment': '临时 nginx:1.27.0；只读生产 dist；未接入开发或生产后端'}
        (ROOT / '.local/nginx-check-results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
        for result in results:
            print('PASS ' + result)
        print(f'Nginx HTTP：{len(results)}/{len(results)}；JS {len(original)} → {len(compressed)} bytes')
    finally:
        label = docker('inspect', '--format', '{{index .Config.Labels "itls.quality.run"}}', cid)
        if label != run_id:
            raise RuntimeError('拒绝清理标签不匹配的 Nginx 容器')
        docker('rm', '--force', cid)


if __name__ == '__main__':
    main()
