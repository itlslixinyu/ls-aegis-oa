"""Read-only repository inventory; writes evidence only under .local/repo-organize.

No deletion, relocation, dependency installation, Docker mutation or secret reading.
Opaque runtime/dependency trees are represented by directory rows, not traversed.
"""
import argparse
import collections
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[2]
DOC_EXT = {'.md', '.mdc', '.txt', '.docx', '.pdf'}
OPAQUE_NAMES = {'.git', 'node_modules', 'target', 'dist', 'build', '.gradle',
                '.vite', '.pnpm-store', 'coverage', 'dist-engineer', 'dist-ssr'}
SECRET_EXT = {'.pem', '.key', '.crt', '.csr', '.p12', '.pfx', '.jks', '.keystore'}
FINDINGS = {}  # Findings must come from the current scan, not a previous maintenance run.


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT).decode('utf-8')


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def inventory(out):
    tracked = set(filter(None, git('ls-files', '-z').split('\0')))
    untracked = set(filter(None, git('ls-files', '--others', '--exclude-standard', '-z').split('\0')))
    rows, documents, links, errors = [], [], [], []

    def add(path, category, evidence, kind='file', opaque=False):
        name = path.relative_to(ROOT).as_posix()
        try:
            stat = path.lstat()
        except OSError as exc:
            errors.append({'path': name, 'error': type(exc).__name__})
            return
        state = 'tracked' if name in tracked else 'untracked' if name in untracked else 'ignored-or-local'
        target = name
        if category == '归档':
            target = '.local/archive/' + name
        elif category == '可删':
            target = '(candidate only; not deleted)'
        action = {'保留': '保留原位', '归档': '拟本地归档，先脱敏/核实引用',
                  '可删': '已识别可重建缓存，仅列候选', '待复核': '原位保留，定向复核'}[category]
        row = dict(source=name, target=target, category=category, evidence=evidence,
                   action=action, risk='protected' if opaque and category == '保留' else 'review' if category in {'待复核','归档'} else 'low',
                   status='DONE_INVENTORY_ONLY', validation='metadata/reference classification; no business validation',
                   rollback='未移动或删除；变更前必须建立本批回退记录',
                   kind=kind, scope='opaque-directory' if opaque else 'individual-entry',
                   git_state=state, bytes=stat.st_size if kind == 'file' else '',
                   mtime_ns=stat.st_mtime_ns, sha256='')
        safe_doc = (kind == 'file' and path.suffix.lower() in DOC_EXT and
                    not name.startswith(('.local/', 'Docs/20')) and not opaque)
        if safe_doc:
            raw = path.read_bytes()
            row['sha256'] = hashlib.sha256(raw).hexdigest()
            text = raw.decode('utf-8-sig', errors='replace') if path.suffix.lower() in {'.md', '.mdc', '.txt'} else ''
            headings = re.findall(r'^#{1,3}\s+(.+)$', text, re.M)
            documents.append(dict(path=name, category=category, bytes=len(raw), lines=len(text.splitlines()),
                                  sha256=row['sha256'], headings=headings, evidence=evidence,
                                  review_depth='metadata + headings + local-link check; targeted content review for findings'))
            for line_no, line in enumerate(text.splitlines(), 1):
                for match in re.finditer(r'\]\(([^)]+)\)', line):
                    href = match.group(1).split(' "', 1)[0].strip('<>')
                    if href.startswith(('#', 'http:', 'https:', 'mailto:', 'plugin:', 'app:', 'codex:')):
                        continue
                    target_link = unquote(href.split('#', 1)[0])
                    if not target_link:
                        continue
                    candidate = path.parent / target_link
                    exists = candidate.exists()
                    links.append(dict(source=name, line=line_no, href=href, exists=exists,
                                      check='path existence only; anchors/dynamic examples need review'))
        rows.append(row)

    def scan(folder):
        try:
            children = sorted(folder.iterdir(), key=lambda item: item.name.casefold())
        except OSError as exc:
            errors.append({'path': str(folder.relative_to(ROOT)), 'error': type(exc).__name__})
            return
        for path in children:
            name = path.relative_to(ROOT).as_posix()
            if path.is_symlink() or path.is_junction():
                add(path, '待复核', '符号链接/junction，不跟随扫描，防止越出工作区', 'directory' if path.is_dir() else 'file', True)
                continue
            if path.is_dir():
                if name == '.local':
                    add(path, '保留', '活跃工具/AVD/候选JAR/私有数据库备份与证据根目录，禁止整体移动', 'directory', True)
                    for item in sorted(path.iterdir(), key=lambda p: p.name):
                        rel = item.relative_to(ROOT).as_posix()
                        if item.name == 'repo-organize':
                            add(item, '保留', '本轮基线快照和清单根目录，自身不递归计入盘点', 'directory', True)
                        elif item.is_dir():
                            if item.name in {'android', 'toolchains', 'pylibs', 'unified-deploy', 'business-workflow', 'releases', 'restart-latest', 'conservative-cleanup'}:
                                add(item, '保留', '运行/构建工具或可能含凭据、数据库备份的私有证据；仅目录级登记', 'directory', True)
                            elif item.name == '__pycache__':
                                add(item, '可删', 'Python 自动生成缓存目录，可从源文件重建；不作为运行配置/数据', 'directory', True)
                            else:
                                add(item, '待复核', '历史构建/探针/测试/镜像仓库候选；未递归读取，需核对回退或脚本引用', 'directory', True)
                        else:
                            add(item, '待复核', '本机历史任务文件，可能含真实业务证据/凭据或回退用途；仅元数据，不读取内容')
                    continue
                if name == 'docker/data':
                    add(path, '保留', '后端真实附件/国密密钥/日志挂载源，禁止遍历、归档或删除', 'directory', True)
                    continue
                if name.startswith('Docs/') and re.match(r'^\d{8}_', path.name):
                    add(path, '归档', '历史任务工作目录，拟仅本地归档；先检查私有数据和现行引用，不发布原始证据', 'directory', True)
                    continue
                if path.name in OPAQUE_NAMES or path.name.startswith('dist-'):
                    reason = 'Git 元数据，保护历史和本地分支' if path.name == '.git' else '依赖/构建产物目录，保留当前开发与回归环境；仅目录级，不推导可删'
                    add(path, '保留', reason, 'directory', True)
                    continue
                if name == '.agents':
                    add(path, '保留', '工作区受保护元数据路径；当前无文件，不尝试删除', 'directory', True)
                    continue
                if name == '.cursor':
                    add(path, '待复核', '当前为空目录；根 rules 已生效，仍需确认编辑器用途；不按名称直接删', 'directory', True)
                    continue
                scan(path)
                continue
            if not path.is_file():
                continue
            if name == 'scripts/deploy-info.json' or path.suffix.lower() in SECRET_EXT or path.name == '.env' or path.name.endswith('.env') or path.name == 'local.properties' or (path.name.startswith('.env.') and not path.name.endswith('.example') and path.name not in {'.env.development', '.env.production', '.env.customer', '.env.engineer'}):
                add(path, '保留', '本地配置/签名/密钥敏感文件，仅元数据，不读取、不发布')
            elif name in FINDINGS:
                add(path, *FINDINGS[name])
            elif path.suffix.lower() in {'.pyc', '.pyo'} and '__pycache__' in path.parts:
                add(path, '可删', 'Python 字节码可重建，无源码/配置价值；仅列候选')
            elif path.parent == ROOT and path.suffix == '.log':
                add(path, '归档', '旧Vite日志：现由Nginx提供入口，进程检查无Vite；保留排障证据并先核实文件未持续写入')
            elif name in tracked or name in untracked:
                add(path, '保留', '当前Git跟踪/待提交源码、测试、配置、协议或工程规范；保留不等于已完成逐函数死代码审查')
            else:
                add(path, '待复核', '非跟踪的本地文件，未证实无用；禁止由未跟踪状态直接推导可删')

    scan(ROOT)
    indexed = {item['source'] for item in rows}
    for name in sorted(tracked - indexed):
        if (ROOT / name).is_file():
            add(ROOT/name, '保留', 'Git 跟踪路径位于聚合目录下，单独登记，不能因构建目录同名漏掉源码')
        elif not (ROOT / name).exists():
            rows.append(dict(source=name, target=name, category='待复核', evidence='Git跟踪但工作树缺失', action='检查原有删除，不自动还原',risk='review',status='DONE_INVENTORY_ONLY',validation='git vs filesystem',rollback='保留原有差异',kind='missing',scope='individual-entry',git_state='tracked',bytes='',mtime_ns='',sha256=''))
    fields = list(rows[0])
    for category, filename in [('保留','keep.csv'),('归档','archive.csv'),('可删','deletable.csv'),('待复核','review.csv'),(None,'inventory.csv')]:
        with (out / filename).open('w', encoding='utf-8-sig', newline='') as stream:
            writer = csv.DictWriter(stream, fieldnames=fields)
            writer.writeheader()
            writer.writerows(row for row in rows if category is None or row['category'] == category)
    write_json(out/'documents.json', documents)
    with (out/'documents.csv').open('w',encoding='utf-8-sig',newline='') as stream:
        writer = csv.DictWriter(stream,fieldnames=['path','category','bytes','lines','sha256','evidence','review_depth'])
        writer.writeheader()
        writer.writerows({key: doc[key] for key in writer.fieldnames} for doc in documents)
    write_json(out/'document-links.json', links)
    hashes = collections.defaultdict(list)
    for doc in documents:
        hashes[doc['sha256']].append(doc['path'])
    duplicates = [names for names in hashes.values() if len(names) > 1]
    write_json(out/'duplicate-documents.json', duplicates)
    # No development-report document is required in a source checkout.
    sections = []
    write_json(out/'report-sections.json',sections)
    write_json(out/'review-groups.json',dict(collections.Counter(row['source'].split('/')[1] if row['source'].startswith('.local/') and row['kind']=='directory' else '.local-private-files' if row['source'].startswith('.local/') else 'current-docs-and-config' for row in rows if row['category']=='待复核')))
    counts = dict(collections.Counter(row['category'] for row in rows))
    summary = dict(root=str(ROOT), entries=len(rows), categories=counts,
                   individual_files=sum(row['kind']=='file' for row in rows),
                   opaque_directories=sum(row['scope']=='opaque-directory' for row in rows),
                   tracked_file_count=len(tracked), current_documents=len(documents),
                   document_links=len(links), missing_document_links=sum(not link['exists'] for link in links),
                   identical_doc_groups=len(duplicates), errors=errors,
                   tracked_entries_covered=sum(row['git_state']=='tracked' for row in rows),report_sections=len(sections),
                   coverage='All tracked paths and all filesystem entries outside explicitly opaque directories; protected/dependency/local historic subtrees represented by directory entries. No content scan of credentials/data volumes/SDK/AVD/logs/private evidence.',
                   deleted_files=0, moved_files=0)
    write_json(out/'summary.json', summary)
    print(json.dumps(summary, ensure_ascii=False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-id', required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'\d{8}_\d{6}', args.run_id):
        parser.error('run-id must be YYYYMMDD_HHMMSS')
    out = ROOT/'.local/repo-organize'/args.run_id
    assert out.resolve().is_relative_to(ROOT/'.local/repo-organize')
    if not (out/'baseline.json').is_file():
        parser.error('existing baseline.json required; do not overwrite the baseline')
    inventory(out)


if __name__ == '__main__':
    main()
