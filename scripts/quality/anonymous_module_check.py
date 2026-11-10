"""Read-only local authentication boundary check; never creates test business data."""
import json
import urllib.request
from pathlib import Path
from datetime import datetime, timezone

PATHS = ['/auth/user/info', '/psi/unit', '/psi/goods', '/psi/warehouse',
         '/psi/stock-in', '/psi/stock-out', '/psi/receipt', '/psi/payment',
         '/psi/workbench/summary', '/quote', '/quote/summary',
         '/eoms/repair/order/list', '/eoms/dashboard/overview',
         '/sign/dashboard/stats', '/system/file', '/eoms/admin/h5/auth/online',
         '/eoms/h5/repair/list', '/eoms/engineer/tasks']

def main():
    rows = []
    for path in PATHS:
        try:
            with urllib.request.urlopen('http://127.0.0.1:8000' + path, timeout=10) as response:
                body = json.load(response)
                code = str(body.get('code'))
                passed = body.get('success') is False and code in ('401', '10007')
                rows.append(dict(path=path, http=response.status, code=code, passed=passed))
        except Exception as error:
            rows.append(dict(path=path, passed=False, error=type(error).__name__))
    result = dict(at=datetime.now(timezone.utc).isoformat(),
                  scope='anonymous GET only; authentication rejection, not authenticated functionality',
                  results=rows, passed=sum(row['passed'] for row in rows), total=len(rows),
                  businessWrites=0, cleanup='No business objects or sessions created')
    target = Path('.local/anonymous-module-check.json')
    target.parent.mkdir(exist_ok=True)
    target.write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False))
    return 0 if result['passed'] == result['total'] else 1

if __name__ == '__main__':
    raise SystemExit(main())
