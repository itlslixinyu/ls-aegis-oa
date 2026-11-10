"""L0 只读冒烟：HTTP、平台响应和未登录拦截，不登录、不写业务。"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def check(base, path, kind):
    url = base.rstrip('/') + path
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    try:
        try:
            response = opener.open(url, timeout=10)
        except urllib.error.HTTPError as exc:
            response = exc
        with response:
            status, raw = response.code, response.read(2_000_000)
        if kind == 'html':
            passed = status == 200 and b'<html' in raw.lower()
        elif kind == 'denied' and status in (401, 403):
            passed = True
        else:
            body = json.loads(raw)
            if kind == 'denied':
                passed = (status == 200 and body.get('success') is False
                          and str(body.get('code')) in ('401', '403'))
            else:
                passed = status == 200 and body.get('success') is True and str(body.get('code')) == '0'
        print(f'{"PASS" if passed else "FAIL"} {url} HTTP={status} 预期={kind}')
        return passed
    except (OSError, ValueError, TypeError, AttributeError) as exc:
        print(f'FAIL {url} {type(exc).__name__}')
        return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api', default='http://127.0.0.1:8000')
    parser.add_argument('--pc', default='http://127.0.0.1:5173')
    parser.add_argument('--customer', default='http://127.0.0.1:5174')
    parser.add_argument('--engineer', default='http://127.0.0.1:5175')
    args = parser.parse_args()
    for base in vars(args).values():
        parsed = urllib.parse.urlparse(base)
        if parsed.scheme != 'http' or parsed.hostname not in ('localhost', '127.0.0.1', '::1'):
            parser.error('此脚本仅接受本机 HTTP 地址')
    cases = [
        (args.api, '/auth/gm/public-key', 'json'),
        (args.api, '/sign/execute/projects', 'denied'),
        (args.pc, '/', 'html'),
        (args.pc, '/api/auth/gm/public-key', 'json'),
        (args.pc, '/api/system/common/dict/option/site', 'json'),
        (args.customer, '/', 'html'),
        (args.customer, '/auth/gm/public-key', 'json'),
        (args.engineer, '/', 'html'),
        (args.engineer, '/auth/gm/public-key', 'json'),
    ]
    results = [check(*case) for case in cases]
    print(f'L0 结果：{sum(results)}/{len(results)}；未执行登录后的业务闭环。')
    return 0 if all(results) else 1


if __name__ == '__main__':
    sys.exit(main())
