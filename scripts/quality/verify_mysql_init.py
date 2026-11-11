"""在无端口映射、无持久卷的独立 MySQL 容器验证唯一安装 SQL。"""
import argparse
from pathlib import Path
import subprocess
import sys
import time
import uuid
import json
import re

ROOT = Path(__file__).resolve().parents[2]

# Only structural configuration may contain rows in a fresh installation.
SEED_TABLES = {
    '_itls_init_done', 'sys_menu', 'sys_dept', 'sys_role', 'sys_user',
    'sys_option', 'sys_dict', 'sys_dict_item', 'sys_user_role',
    'sys_storage', 'sys_client', 'sys_role_menu', 'psi_print_template',
}


def verify_clean_data(container_id):
    names = docker('exec', container_id, 'mysql', '-uroot', '-N', '-e',
                   "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='ls_aegis' ORDER BY TABLE_NAME;").stdout.splitlines()
    # Names come from information_schema, not input or runtime business records.
    query = ' UNION ALL '.join(f"SELECT '{name}',COUNT(*) FROM ls_aegis.`{name}`" for name in names)
    rows = docker('exec', container_id, 'mysql', '-uroot', '-N', '-e', query).stdout.splitlines()
    counts = {name: int(count) for name, count in (row.split('\t') for row in rows)}
    unexpected = {name: count for name, count in counts.items() if name not in SEED_TABLES and count}
    checks = docker('exec', container_id, 'mysql', '-uroot', '-N', '-e',
                    "SELECT COUNT(*) FROM ls_aegis.sys_user WHERE id<>1 OR username<>'admin'; "
                    "SELECT COUNT(*) FROM ls_aegis.sys_user_role ur LEFT JOIN ls_aegis.sys_user u ON u.id=ur.user_id LEFT JOIN ls_aegis.sys_role r ON r.id=ur.role_id WHERE u.id IS NULL OR r.id IS NULL; "
                    "SELECT COUNT(*) FROM ls_aegis.sys_user u LEFT JOIN ls_aegis.sys_dept d ON d.id=u.dept_id WHERE d.id IS NULL; "
                    "SELECT COUNT(*) FROM ls_aegis.sys_option WHERE code IN ('SITE_LOGO','SITE_FAVICON') AND (value IS NOT NULL AND value<>''); "
                    "SELECT COUNT(*) FROM ls_aegis.sys_option WHERE (code='SITE_LOGO' AND default_value='/brand-logo.png') OR (code='SITE_FAVICON' AND default_value='/favicon.png'); "
                    "SELECT COUNT(*) FROM ls_aegis.sys_option WHERE CONCAT(COALESCE(value,''),COALESCE(default_value,'')) LIKE '%/api/file/%';").stdout.strip().splitlines()
    patch = (ROOT / 'ls-aegis-server/ls-aegis-boot/src/main/resources/db/changelog/mysql/patch_clean_install_defaults.sql').read_text(encoding='utf-8')
    guard = re.search(r'^-- precondition-sql-check expectedResult:0 (.+)$', patch, re.M).group(1)
    guard_empty = docker('exec', container_id, 'mysql', '-uroot', '-N', 'ls_aegis', '-e', guard).stdout.strip()
    docker('exec', container_id, 'mysql', '-uroot', 'ls_aegis', '-e',
           "INSERT INTO sys_notice(id,title,content,type,create_user,create_time) VALUES(999999999,'ITLS-TEST-CLEAN-GUARD','temporary fixture','1',1,NOW());")
    guard_with_notice = docker('exec', container_id, 'mysql', '-uroot', '-N', 'ls_aegis', '-e', guard).stdout.strip()
    docker('exec', container_id, 'mysql', '-uroot', 'ls_aegis', '-e', 'DELETE FROM sys_notice WHERE id=999999999;')
    guarded = guard_empty == '0' and guard_with_notice == '1'
    passed = not unexpected and counts.get('sys_user') == 1 and counts.get('sys_dept') == 1 and checks == ['0', '0', '0', '0', '2', '0'] and guarded
    return {'passed': passed, 'table_counts': counts, 'unexpected_nonempty_tables': unexpected,
            'ownership_defaults_checks': checks, 'install_only_guard_refuses_custom_notice': guarded}


def docker(*args, check=True):
    return subprocess.run(['docker', *args], check=check, text=True,
                          encoding='utf-8', errors='replace', capture_output=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image', default='mysql:8.0.42')
    parser.add_argument('--timeout', type=int, default=240)
    parser.add_argument('--report', type=Path)
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
                    clean = verify_clean_data(container_id)
                    passed = actual == ['1', '0', '1', '1', '1', '0', '0', '0'] and clean['passed']
                    if args.report:
                        args.report.parent.mkdir(parents=True, exist_ok=True)
                        args.report.write_text(json.dumps({'passed': passed, 'schema_checks': actual, **clean}, ensure_ascii=False, indent=2), encoding='utf-8')
                    print(f'{"PASS" if passed else "FAIL"} 全新安装：完成标志/非标准排序表/管理员/菜单/分类编码/下线表/下线菜单/下线开关 = {actual}')
                    print(f"{'PASS' if clean['passed'] else 'FAIL'} 干净安装：逐表核对{len(clean['table_counts'])}张表，非白名单非空表={clean['unexpected_nonempty_tables']}，管理员/关联/默认品牌检查={clean['ownership_defaults_checks']}")
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
