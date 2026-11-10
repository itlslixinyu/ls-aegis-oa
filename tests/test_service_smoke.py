"""验证冒烟不会将登录页重定向、HTTP 200 业务失败或非平台 JSON 判为成功。"""
import contextlib
import importlib.util
import io
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import threading
import unittest

spec = importlib.util.spec_from_file_location('service_smoke', Path(__file__).resolve().parents[1] / 'scripts/quality/service_smoke.py')
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        cases = {
            '/ok': (200, b'{"success":true,"code":"0"}'),
            '/business-failure': (200, b'{"success":false,"code":"500"}'),
            '/denied': (200, b'{"success":false,"code":"401"}'),
            '/invalid': (200, b'[]'),
            '/html': (200, b'<html>login</html>'),
            '/redirect': (302, b''),
        }
        status, body = cases[self.path]
        self.send_response(status)
        if status == 302:
            self.send_header('Location', '/ok')
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


class SmokeResponseTest(unittest.TestCase):
    def test_observable_responses(self):
        with ThreadingHTTPServer(('127.0.0.1', 0), Handler) as server:
            worker = threading.Thread(target=server.serve_forever, daemon=True)
            worker.start()
            try:
                base = f'http://127.0.0.1:{server.server_port}'
                for path, kind, expected in [
                    ('/ok', 'json', True),
                    ('/business-failure', 'json', False),
                    ('/denied', 'denied', True),
                    ('/ok', 'denied', False),
                    ('/invalid', 'json', False),
                    ('/html', 'json', False),
                    ('/redirect', 'json', False),
                ]:
                    with self.subTest(path=path, kind=kind), contextlib.redirect_stdout(io.StringIO()):
                        self.assertEqual(smoke.check(base, path, kind), expected)
            finally:
                server.shutdown()
                worker.join()


if __name__ == '__main__':
    unittest.main()
