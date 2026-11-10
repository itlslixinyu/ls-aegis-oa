"""在无端口映射、无持久卷的独立 MySQL 容器验证唯一安装 SQL。"""
import argparse
from pathlib import Path
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[2]


def docker(*args, check=True):
    return subprocess.run(['docker', *args], check=check, text=True,
                          encoding='utf-8', errors='replace', capture_output=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image', default='mysql:8.0.42')
    parser.add_argument('--timeout', type=int, default=240)
    args = parser.parse_args()
    name = 'itls-init-test-' + uuid.uuid4().hex[:12]
    sql = ROOT / 'ls-aegis-server/sql/init.sql'
    container_id = None
    try:
        run = docker('run', '--detach', '--name', name, '--network', 'none',
                     '--tmpfs', '/var/lib/mysql:rw,size=768m',
                     '--env', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', '--env', 'MYSQL_DATABASE=ls_aegis',
                     '--mount', f'type=bind,source={sql},target=/docker-entrypoint-initdb.d/init.sql,readonly',
                     args.image, '--character-set-server=utf8mb4', '--collation-server=utf8mb4_0900_ai_ci')
        container_id = run.stdout.strip()
        deadline = time.monotonic() + args.timeout
        while time.monotonic() < deadline:
            state = docker('inspect', '--format', '{{.State.Running}}', container_id)
            logs = docker('logs', container_id)
            log = logs.stdout + logs.stderr
            if state.stdout.strip() != 'true' or 'ERROR ' in log:
                print('FAIL 全新安装容器未正常初始化：\n' + log[-5000:])
                return 1
            if 'MySQL init process done. Ready for start up.' in log:
                query = docker('exec', container_id, 'mysql', '-uroot', '--default-character-set=utf8mb4',
                               '-N', '-e', "SELECT COUNT(*) FROM ls_aegis._itls_init_done WHERE id=1; "
                               "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='ls_aegis' "
                               "AND TABLE_COLLATION<>'utf8mb4_general_ci'; "
                               "SELECT COUNT(*) FROM ls_aegis.sys_user WHERE id=1 AND username='admin'; "
                               "SELECT COUNT(*) FROM ls_aegis.sys_menu WHERE id=30100 AND sort=4; "
                               "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ls_aegis' "
                               "AND TABLE_NAME='psi_goods_category' AND COLUMN_NAME='code'; "
                               "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='ls_aegis' AND TABLE_NAME IN ('sys_app','gen_config','gen_field_config'); "
                               "SELECT COUNT(*) FROM ls_aegis.sys_menu WHERE id IN (7000,7010,7011,7012,7013,7014,7015,7016,7017,7018,9000,9010,9011,9012,9013,9014); "
                               "SELECT COUNT(*) FROM ls_aegis.sys_option WHERE code='MODULE_OPEN_ENABLED';", check=False)
                if query.returncode == 0:
                    actual = query.stdout.strip().splitlines()
                    passed = actual == ['1', '0', '1', '1', '1', '0', '0', '0']
                    print(f'{"PASS" if passed else "FAIL"} 全新安装：完成标志/非标准排序表/管理员/菜单/分类编码/下线表/下线菜单/下线开关 = {actual}')
                    return 0 if passed else 1
            time.sleep(2)
        print('FAIL 全新安装验证超时')
        return 1
    except subprocess.CalledProcessError as exc:
        print('FAIL Docker 操作失败：' + (exc.stderr or '')[-2000:])
        return 1
    finally:
        if container_id:
            cleanup = docker('rm', '--force', container_id, check=False)
            if cleanup.returncode:
                print(f'清理失败，请检查临时容器 {name}')


if __name__ == '__main__':
    sys.exit(main())
