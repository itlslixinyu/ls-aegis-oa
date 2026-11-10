"""使用独立、临时 Docker 栈执行业务 API 流程；不使用当前开发库。"""
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
LOCAL = ROOT / '.local'
MANIFEST = LOCAL / 'business-env.json'


def docker(*args, check=True):
    r = subprocess.run(['docker', *args], capture_output=True, text=True, encoding='utf-8', errors='replace')
    if check and r.returncode:
        raise RuntimeError(r.stderr[-1500:])
    return r.stdout.strip()


def cleanup(state):
    run_id = state['runId']
    # 访问失败时保留 manifest，避免丢失仍在运行的隔离环境记录。
    docker('info', '--format', '{{.ServerVersion}}')
    for cid in reversed(state.get('containers', [])):
        present = docker('ps', '-a', '-q', '--no-trunc', '--filter', f'id={cid}')
        if not present:
            continue
        label = docker('inspect', '--format', '{{index .Config.Labels "itls.quality.run"}}', cid)
        if label != run_id:
            raise RuntimeError('拒绝清理标签不匹配的容器')
        docker('rm', '--force', cid)
    if state.get('network'):
        present = docker('network', 'ls', '-q', '--filter', f'id={state["network"]}')
        if present:
            label = docker('network', 'inspect', '--format', '{{index .Labels "itls.quality.run"}}', state['network'])
            if label != run_id:
                raise RuntimeError('拒绝清理标签不匹配的网络')
            docker('network', 'rm', state['network'])
    MANIFEST.unlink(missing_ok=True)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--keep', action='store_true', help='保留隔离环境供复核，完成后用 --cleanup 清理')
    p.add_argument('--reuse', action='store_true')
    p.add_argument('--cleanup', action='store_true')
    p.add_argument('--jar-dir', type=Path, default=ROOT / 'ls-aegis-server/ls-aegis-boot/target',
                   help='候选 JAR 目录；允许先验证新包，再更新开发后端')
    args = p.parse_args()
    LOCAL.mkdir(exist_ok=True)
    if args.cleanup:
        cleanup(json.loads(MANIFEST.read_text(encoding='utf-8')))
        print('隔离业务测试环境已清理')
        return 0
    if args.reuse:
        state = json.loads(MANIFEST.read_text(encoding='utf-8'))
    else:
        if MANIFEST.exists():
            raise RuntimeError('已有隔离环境，请使用 --reuse 或先 --cleanup')
        jar_dir = args.jar_dir.resolve()
        if not (jar_dir / 'ls-aegis-server.jar').is_file():
            raise RuntimeError('候选目录缺少 ls-aegis-server.jar')
        state = {'runId': secrets.token_hex(5), 'containers': [], 'password': secrets.token_hex(20)}
        run_id = state['runId']
        state['network'] = docker('network', 'create', '--label', f'itls.quality.run={run_id}', f'itls-test-{run_id}')
        def start(alias, *options):
            cid = docker('run', '-d', '--name', f'itls-test-{alias}-{run_id}', '--network', state['network'],
                         '--network-alias', alias, '--label', f'itls.quality.run={run_id}', *options)
            state['containers'].append(cid)
            MANIFEST.write_text(json.dumps(state), encoding='utf-8')
            return cid
        state['mysql'] = start('mysql', '--tmpfs', '/var/lib/mysql:rw,size=768m',
                               '-e', f'MYSQL_ROOT_PASSWORD={state["password"]}', '-e', 'MYSQL_DATABASE=ls_aegis',
                               '--mount', f'type=bind,source={ROOT / "ls-aegis-server/sql/init.sql"},target=/docker-entrypoint-initdb.d/init.sql,readonly',
                               'mysql:8.0.42', '--character-set-server=utf8mb4', '--collation-server=utf8mb4_general_ci')
        state['redis'] = start('redis', 'redis:7.2.8', 'redis-server', '--requirepass', state['password'])
        deadline = time.monotonic() + 240
        while time.monotonic() < deadline:
            result = docker('exec', '-e', f'MYSQL_PWD={state["password"]}', state['mysql'], 'mysql', '-uroot', '-N', '-e',
                            'SELECT COUNT(*) FROM ls_aegis._itls_init_done WHERE id=1', check=False)
            if result == '1':
                break
            time.sleep(2)
        else:
            raise RuntimeError('隔离 MySQL 初始化超时')
        env = {**os.environ, 'ITLS_TEST_STATE': str(MANIFEST)}
        MANIFEST.write_text(json.dumps(state), encoding='utf-8')
        bootstrap = subprocess.run(['node', str(ROOT / 'scripts/quality/business_flows.mjs'), '--bootstrap'], env=env, cwd=ROOT)
        if bootstrap.returncode:
            raise RuntimeError('测试身份初始化失败')
        font = next((p for p in [ROOT / 'docker/ls-aegis-server/fonts/simhei.ttf', Path('C:/Windows/Fonts/simhei.ttf')] if p.is_file()), None)
        font_mount = ['--mount', f'type=bind,source={font},target=/app/fonts/simhei.ttf,readonly'] if font else []
        state['backend'] = start('backend', '-p', '127.0.0.1::8000', '--tmpfs', '/app/data:rw,size=128m', '-w', '/app',
            *font_mount,
            '--mount', f'type=bind,source={jar_dir},target=/app/jars,readonly',
            '-e', 'SPRING_PROFILES_ACTIVE=dev', '-e', 'SPRING_LIQUIBASE_ENABLED=false',
            '-e', 'DB_HOST=mysql', '-e', 'DB_USER=root', '-e', 'DB_NAME=ls_aegis', '-e', f'DB_PWD={state["password"]}',
            '-e', 'REDIS_HOST=redis', '-e', f'REDIS_PWD={state["password"]}',
            '-e', f'JWT_SECRET_KEY={secrets.token_hex(48)}', '-e', f'TOKEN_SECRET={secrets.token_hex(48)}',
            '-e', 'SNAIL_JOB_ENABLED=false', '-e', 'GM_KEY_STORE_PATH=/app/data/gm/keys.properties',
            '-e', 'TZ=Asia/Shanghai', 'eclipse-temurin:21-jre-jammy',
            'java', '-Xms256m', '-Xmx768m', '-jar', '/app/jars/ls-aegis-server.jar')
        MANIFEST.write_text(json.dumps(state), encoding='utf-8')
        port = docker('port', state['backend'], '8000/tcp').rsplit(':', 1)[1]
        state['url'] = f'http://127.0.0.1:{port}'
        MANIFEST.write_text(json.dumps(state), encoding='utf-8')
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            try:
                with opener.open(state['url'] + '/auth/gm/public-key', timeout=2) as r:
                    if json.load(r).get('success'):
                        break
            except (OSError, ValueError):
                pass
            time.sleep(2)
        else:
            raise RuntimeError('隔离后端启动超时')
    print('开始隔离业务 API 测试：' + state['url'], flush=True)
    try:
        return subprocess.run(['node', str(ROOT / 'scripts/quality/business_flows.mjs')],
                              env={**os.environ, 'ITLS_TEST_STATE': str(MANIFEST)}, cwd=ROOT).returncode
    finally:
        if not args.keep:
            cleanup(state)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, KeyboardInterrupt) as exc:
        print('隔离测试未完成：' + str(exc))
        if '--keep' not in sys.argv and MANIFEST.exists():
            cleanup(json.loads(MANIFEST.read_text(encoding='utf-8')))
        sys.exit(1)
