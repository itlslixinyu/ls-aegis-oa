"""在用本机实例的串行只读入口检查；不携带登录凭据、不创建或清理业务数据。"""
import json
from datetime import datetime, timezone
from pathlib import Path
import time

from service_smoke import check

ROOT = Path(__file__).resolve().parents[2]
CASES = [
    ('platform', '/auth/gm/public-key', 'json'),
    ('rbac', '/auth/user/info', 'denied'),
    ('rbac', '/system/user', 'denied'),
    ('tenant', '/tenant/management', 'denied'),
    ('file', '/system/file', 'denied'),
    ('file', '/system/common/file/0', 'denied'),
    ('notice', '/user/message', 'denied'),
    ('schedule', '/schedule/job', 'denied'),
    ('psi', '/psi/unit', 'denied'),
    ('quote', '/quote', 'denied'),
    ('eoms', '/eoms/repair/order/list', 'denied'),
    ('sign', '/sign/execute/projects', 'denied'),
]


def main():
    results = []
    for module, path, expected in CASES:
        begin = time.monotonic()
        passed = check('http://127.0.0.1:8000', path, expected)
        results.append({'module': module, 'path': path, 'expected': expected,
                        'status': 'PASS' if passed else 'FAIL',
                        'elapsedMs': round((time.monotonic() - begin) * 1000, 2)})
    report = {'at': datetime.now(timezone.utc).isoformat(), 'target': 'http://127.0.0.1:8000',
              'kind': '匿名入口只读基线', 'concurrency': 1, 'results': results,
              'limitations': '鉴权拦截可能先于路由匹配；401/403不证明端点存在、权限完整或模块功能通过。耗时不是性能验收。',
              'createdEntities': [], 'cleanup': '未创建业务对象，保留正常访问审计'}
    output = ROOT / '.local/live-module-preflight.json'
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'匿名入口检查：{sum(r["status"] == "PASS" for r in results)}/{len(results)}；非模块完整验收')
    return 0 if all(r['status'] == 'PASS' for r in results) else 1


if __name__ == '__main__':
    raise SystemExit(main())
