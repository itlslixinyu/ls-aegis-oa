"""conservative清理阶段1：只读锁定源码/工具/启动基线，不扫描或修复业务代码。"""
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
import shutil
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
SKILLS = ['java-unused-scan', 'vue-unused-scan', 'config-resource-clean', 'dead-branch-scan',
          'dependency-clean', 'validate-and-rollback', 'doc-comment-clean']
SECRET_SUFFIXES = {'.key', '.pem', '.p12', '.pfx', '.jks', '.keystore', '.crt', '.cer'}


def run(args, timeout=8):
    """仅运行本文件固定的只读命令；不输出环境变量或配置正文。"""
    try:
        p = subprocess.run(args, cwd=ROOT, capture_output=True, timeout=timeout)
        text = (p.stdout + p.stderr).decode('utf-8', errors='replace')
        return {'command': args, 'exitCode': p.returncode, 'output': text[:8000],
                'status': 'PASS' if p.returncode == 0 else 'FAIL'}
    except subprocess.TimeoutExpired:
        return {'command': args, 'status': 'TIMEOUT'}
    except OSError as error:
        return {'command': args, 'status': 'UNAVAILABLE', 'errorType': type(error).__name__}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    head = run(['git', 'rev-parse', 'HEAD'])
    if head['status'] != 'PASS':
        raise RuntimeError('无法锁定Git HEAD；不继续阶段1')
    run_id = 'R54-' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex[:8]
    output = ROOT / '.local/conservative-cleanup' / run_id
    output.mkdir(parents=True, exist_ok=False)
    files = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode('utf-8').split('\0')
    manifest, excluded = {}, []
    for name in filter(None, files):
        path = ROOT / name
        if path.name.startswith('.env') or path.suffix.lower() in SECRET_SUFFIXES or name.startswith('docker/data/'):
            excluded.append({'path': name, 'reason': '敏感/数据文件不读取内容，且禁止自动清理'})
        elif path.is_symlink():
            excluded.append({'path': name, 'reason': '链接只登记，不跟随'})
        elif path.is_file():
            manifest[name] = digest(path)
        else:
            excluded.append({'path': name, 'reason': '工作区缺失，不自动恢复或删除'})
    loaded = []
    for name in SKILLS:
        path = ROOT / 'skills' / name / 'SKILL.md'
        if not path.is_file():
            raise RuntimeError('缺少要求加载的Skill：' + name)
        # 这里只验证加载内容与哈希，不执行该Skill的阶段2流水线。
        path.read_text(encoding='utf-8')
        loaded.append({'name': name, 'path': path.relative_to(ROOT).as_posix(),
                       'sha256': digest(path), 'load': 'READ', 'pipelineExecution': 'NOT_RUN_PHASE1'})
    versions = {}
    for tool, args in [('java', ['-version']), ('javac', ['-version']), ('mvn', ['-version']),
                       ('node', ['--version']), ('npm', ['--version']), ('pnpm', ['--version']),
                       ('jdeprscan', ['--version']), ('pmd', ['--version']), ('sonar-scanner', ['--version'])]:
        executable = shutil.which(tool)
        versions[tool] = run([executable, *args]) if executable else {'status': 'UNAVAILABLE'}
    docker_exe = shutil.which('docker')
    docker = run([docker_exe, 'version', '--format', '{{.Server.Version}}']) if docker_exe else {'status': 'UNAVAILABLE'}
    smoke = run([sys.executable, '-X', 'utf8', 'scripts/quality/service_smoke.py'], timeout=30)
    (output / 'service-smoke.log').write_text(smoke.get('output', smoke['status']), encoding='utf-8')
    app_diff = run(['git', 'diff', '--name-only', 'HEAD', '--', 'ls-aegis-server', 'ls-aegis-web', 'ls-aegis-h5'])
    baseline = {
        'runId': run_id, 'atUtc': datetime.now(timezone.utc).isoformat(), 'userTimezone': 'Asia/Shanghai',
        'mode': 'conservative', 'phase': 1, 'head': head['output'].strip(),
        'branch': run(['git', 'branch', '--show-current'])['output'].strip(),
        'worktree': run(['git', 'status', '--porcelain=v1']),
        'applicationDiffFromHead': app_diff,
        'trackedHashes': len(manifest), 'excludedFiles': excluded, 'skills': loaded,
        'tools': versions, 'docker': docker,
        'startupBaseline': smoke,
        'startupMeaning': '只读服务基线，不是本批候选新启动或全部健康检查',
        'cleanupGate': 'BASELINE_RECORDED_REQUIRES_REVIEW' if smoke['status'] == 'PASS' else 'BLOCKED_FOR_CLEANUP',
        'scanResults': 'NOT_RUN_PHASE2', 'businessCodeChanged': False, 'cleanupBatchApplied': False,
        'rollback': 'NOT_APPLICABLE_NO_CLEANUP_BATCH',
        'compileAndUnitTests': 'NOT_RUN_PHASE1',
        'expectedToolchain': {'backend': 'JDK21/Maven，Java17 release', 'pc': 'Node与pnpm9匹配锁文件', 'h5': 'npm ci按既有锁文件'},
    }
    (output / 'tracked-sha256.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    (output / 'baseline.json').write_text(json.dumps(baseline, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'runId': run_id, 'head': baseline['head'], 'trackedHashes': len(manifest),
                      'skillsLoaded': len(loaded), 'startupStatus': smoke['status'],
                      'cleanupGate': baseline['cleanupGate'], 'evidence': str(output.relative_to(ROOT))}, ensure_ascii=False))


if __name__ == '__main__':
    main()
