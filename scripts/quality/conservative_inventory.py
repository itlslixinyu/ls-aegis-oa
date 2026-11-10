"""Stage 2 read-only inventories; never deletes, fixes, installs or edits manifests.

Run only after Java PMD, ESLint and isolated Vite evidence exists. Lexical negative
references are candidates, never proof of dead code. Evidence stays under .local.
"""
import argparse
import csv
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIELDS = 'run_id finding_id skill path line_start line_end symbol type classification risk evidence reference_evidence proposed_action actual_action before_sha256 after_sha256 validation_status reason'.split()
HIGH_PATH = re.compile(r'ls-aegis-(?:common|mybatis|redis|security|crypto|starter|boot)(?:/|-)|/(?:auth|rbac|tenant|schedule|task|mq|queue|controller|apis?|mapper|entity|dto|vo|rpc|protocol)/|biz-sign/|/sign/', re.I)
HIGH_TEXT = re.compile(r'\b(?:SM2|SM3|SM4|GB28181|RabbitMQ|Kafka|RocketMQ|SnailJob|Feign|ClassLoader)\b|国密|com\.ls\.aegis\.(?:crypto|mybatis|security)|ReflectionTestUtils', re.I)
SECRET = re.compile(r'(?:^|/)\.env(?:[./]|$)|\.(?:pem|key|p12|pfx|jks|cer|crt)$', re.I)
TEXT_EXT = {'.java', '.vue', '.ts', '.tsx', '.js', '.mjs', '.cjs', '.json', '.xml', '.yml', '.yaml', '.properties', '.css', '.scss', '.less', '.html', '.md', '.py', '.ps1', '.sh', '.bat', '.cmd', '.sql', '.txt', '.conf'}


def dump(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('run_directory')
    args = ap.parse_args()
    out = (ROOT / args.run_directory).resolve()
    if not out.is_relative_to(ROOT / '.local/conservative-cleanup') or out == ROOT / '.local/conservative-cleanup':
        raise ValueError('Invalid evidence directory')
    baseline = json.loads((out / 'baseline.json').read_text(encoding='utf-8'))
    if baseline['mode'] != 'conservative' or baseline['startupBaseline']['status'] != 'PASS':
        raise ValueError('Baseline not ready')
    java = json.loads((out / 'java-scan/raw-findings.json').read_text(encoding='utf-8'))
    vue = json.loads((out / 'vue-eslint-raw.json').read_text(encoding='utf-8'))
    graph = json.loads((out / 'vue-vite-graph.json').read_text(encoding='utf-8'))
    if not graph['originalSourceHashesMatched']:
        raise ValueError('Source changed during Vite scan')
    tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    tracked = [p for p in tracked if p]
    hashes, texts, coverage, rows, stages = {}, {}, [], [], []
    for p in tracked:
        path = ROOT / p
        protected = bool(SECRET.search(p))
        if not protected:
            hashes[p] = hashlib.sha256(path.read_bytes()).hexdigest()
            if path.suffix.lower() in TEXT_EXT or path.name.startswith('Dockerfile'):
                try:
                    texts[p] = path.read_text(encoding='utf-8-sig')
                except UnicodeError:
                    pass
        method = 'METADATA_ONLY_BINARY_OR_OTHER'
        if protected:
            method = 'EXCLUDED_SECRET_OR_ENV_CONTENT'
        elif p.endswith('.java'):
            method = 'PMD_CONFIGURED_INPUT; NO_ZERO_FINDING_PER_FILE_PROCESS_PROOF'
        elif p in {x['file'] for x in vue['results']}:
            method = 'ESLINT; H5_VIRTUAL_PARSER; SEE_PARSE_GAPS'
        elif p in texts:
            method = 'LEXICAL_REFERENCE_INDEX_ONLY; NO_SEMANTIC_PROOF'
        coverage.append({'path': p, 'method': method, 'sha256': hashes.get(p, ''), 'automaticDeletion': False})

    def high(p, text=''):
        return bool(HIGH_PATH.search(p) or HIGH_TEXT.search(text))

    def add(skill, p, line, symbol, kind, evidence, reference, reason, classification='MANUAL_REVIEW'):
        if high(p, texts.get(p, '') if p.endswith(('.java', '.vue', '.ts')) else symbol):
            classification = 'PROHIBITED_AUTO'
            reason = '高风险基座/国密/鉴权/契约/动态消费边界；' + reason
        rows.append(dict(zip(FIELDS, [baseline['runId'], f'S2-{len(rows)+1:05d}', skill, p, line, line, symbol, kind,
            classification, {'CONFIRMED_REMOVABLE': 'LOW', 'MANUAL_REVIEW': 'MEDIUM', 'PROHIBITED_AUTO': 'HIGH'}[classification],
            evidence, reference, '仅登记，不执行清理；后续须按阶段4/5逐项审批证据', 'NONE_SCAN_ONLY', hashes.get(p, ''), hashes.get(p, ''),
            'STATIC_EVIDENCE_ONLY; NOT_CLEANUP_REGRESSION', reason])))

    # 1/2: consume actual completed tool results and independently narrow LOW imports.
    for item in java:
        p, line = item['path'], int(item['beginline'])
        source = texts.get(p, '')
        src_line = source.splitlines()[line-1] if source else ''
        c = item['classification']
        if c == 'CONFIRMED_REMOVABLE':
            m = re.fullmatch(r'\s*import\s+([\w.]+);\s*', src_line)
            name = m[1].split('.')[-1] if m else ''
            if not name or len(re.findall(r'\b' + re.escape(name) + r'\b', source)) != 1:
                c = 'MANUAL_REVIEW'
        add('java-unused-scan', p, line, src_line.strip(), item['rule'], 'PMD7.17.0:' + item['message'], 'java-scan/target/pmd.xml; ordinary import additionally checked for exact one lexical occurrence',
            'PMD候选；无应用auxclasspath，静态/通配符import、方法及赋值不自动处理', c)
    stages.append({'skill': 'java-unused-scan', 'status': 'PASS_WITH_LIMITATIONS', 'rawCandidates': len(java)})
    for item in vue['results']:
        for m in item.get('messages', []):
            add('vue-unused-scan', item['file'], m.get('line', 1), m.get('ruleId') or 'PARSER_GAP', 'ESLINT_DIAGNOSTIC',
                m['message'], 'vue-eslint-raw.json; ' + item['mode'], '禁止eslint --fix；import可能有副作用；参数/模板变量需契约复核；解析失败单列缺口')
    stages.append({'skill': 'vue-unused-scan', 'status': 'PASS_WITH_LIMITATIONS', 'targets': vue['targets'], 'parserFatalFiles': sum(any(m.get('fatal') for m in x.get('messages', [])) for x in vue['results']), 'viteSourceUnchanged': True})

    # 3: config/resource consumers; zero basename hits is NOT dead-file evidence.
    resource_inventory = []
    for p in tracked:
        path = ROOT / p
        is_resource = SECRET.search(p) or path.suffix.lower() in {'.yml', '.yaml', '.properties', '.svg', '.png', '.jpg', '.ico', '.woff', '.woff2', '.sql', '.conf', '.ps1', '.sh', '.bat', '.cmd'} or '/resources/' in p or path.name.startswith('Dockerfile') or p.endswith('tsconfig.json')
        if not is_resource:
            continue
        name = path.name
        consumers = [q for q, t in texts.items() if q != p and name in t]
        dynamic = bool(re.search(r'/public/|/assets/|/resources/|Dockerfile|compose|tsconfig|vite\.config|/sql/', p, re.I))
        resource_inventory.append({'path': p, 'lexicalConsumers': consumers[:20], 'consumerCount': len(consumers), 'conventionalOrDynamicEntry': dynamic, 'secretContentRead': False, 'decision': 'KEEP; FILE_DELETION_OUT_OF_SCOPE'})
        if SECRET.search(p) or p.endswith('.sql'):
            add('config-resource-clean', p, 1, name, 'PROTECTED_RESOURCE_BOUNDARY', '只登记路径；密钥/环境内容排除；迁移脚本契约保护', 'config-resource-inventory.json', '敏感配置/迁移一律保留', 'PROHIBITED_AUTO')
        elif not consumers:
            add('config-resource-clean', p, 1, name, 'LEXICAL_ZERO_REFERENCE_CANDIDATE', '其他已索引文本无同文件名引用；不是不存在消费者的证明', 'config-resource-inventory.json', '可能由框架约定、glob、public URL或外部部署消费；本模式不删文件')
    dump(out / 'config-resource-inventory.json', resource_inventory)
    stages.append({'skill': 'config-resource-clean', 'status': 'PASS_WITH_LIMITATIONS', 'inventoryFiles': len(resource_inventory), 'method': 'lexical plus conventional/dynamic entry protection; no environment activation proof'})

    # 4: AST literals, then lexically located Java/config-controlled branches.
    completed = subprocess.run(['node', str(ROOT / 'scripts/quality/conservative_branch_scan.mjs'), str(out.relative_to(ROOT))], cwd=ROOT, capture_output=True, text=True, encoding='utf-8')
    (out / 'frontend-ast.log').write_text(completed.stdout + completed.stderr, encoding='utf-8')
    if completed.returncode:
        raise RuntimeError('AST collector failed; inspect frontend-ast.log')
    ast = json.loads((out / 'frontend-ast.json').read_text(encoding='utf-8'))
    for item in ast['results']:
        for b in item['branches']:
            add('dead-branch-scan', item['file'], b['line'], b['condition'], 'AST_BOOLEAN_LITERAL', b['proof'], 'frontend-ast.json', '字面值已证明；控制流、副作用及未来开关意图未证明，不能自动简化')
    for p, text in texts.items():
        if not p.endswith(('.java', '.ts', '.vue', '.js')):
            continue
        for i, line in enumerate(text.splitlines(), 1):
            if (p.endswith('.java') and re.search(r'\bif\s*\(\s*(?:true|false)\s*\)', line)) or re.search(r'\bif\s*\(.*(?:import\.meta\.env|@Value|hasPermission|hasRole|isDev|isProd)', line):
                add('dead-branch-scan', p, i, 'condition/flag', 'LEXICAL_BRANCH_REVIEW', '仅词法定位；不证明恒定或不可达', '当前文件对应行；PMD UnconditionalIfStatement无命中不代表无死分支', '环境/权限/运维分支保留；Java全局常量传播未执行')
    stages.append({'skill': 'dead-branch-scan', 'status': 'PASS_WITH_LIMITATIONS', 'astFiles': len(ast['results']), 'astParseErrorFiles': sum(bool(x['parseErrors'] or x.get('sfcErrors')) for x in ast['results']), 'globalConstantPropagation': 'NOT_RUN'})

    # 5: dependency declarations and observed consumers; no manifest modifications.
    dep_inventory = []
    for p in tracked:
        if Path(p).name == 'package.json' and p in texts:
            pkg = json.loads(texts[p]); base = str(Path(p).parent).replace('\\', '/') + '/'
            for scope in ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies']:
                for name, version in pkg.get(scope, {}).items():
                    imports = [{'path': a['file'], **m} for a in ast['results'] if a['file'].startswith(base) for m in a['imports'] if m['specifier'] == name or m['specifier'].startswith(name + '/')]
                    lexical = [q for q, t in texts.items() if q.startswith(base) and q != p and not q.endswith('lock.json') and name in t]
                    observed = [x for x in graph.get('allModules', []) if '/node_modules/' + name + '/' in x['id'].replace('\\', '/')]
                    evidence = {'path': p, 'name': name, 'version': version, 'scope': scope, 'astImports': imports[:20], 'lexicalConsumers': lexical[:20], 'observedViteModules': len(observed), 'decision': 'KEEP_CONSERVATIVE; no unused dependency proven', 'limitations': 'CLI/peer/types/CSS/auto-import/H5 modes may consume; isolated graph only PC defaults'}
                    dep_inventory.append(evidence)
                    if not imports or HIGH_TEXT.search(name) or re.search(r'crypto|bytebuddy|aws|snail|rabbit|kafka|serialize', name, re.I):
                        add('dependency-clean', p, 1, name, 'DEPENDENCY_DECLARATION_REVIEW', f'{scope}; AST imports={len(imports)}; lexical consumers={len(lexical)}; PC graph nodes={len(observed)}', 'dependency-inventory.json', '零import不证明无用；核对CLI/peer/types/第三方运行消费；本模式禁止删依赖', 'PROHIBITED_AUTO' if re.search(r'crypto|bytebuddy|aws|snail|rabbit|kafka|serialize', name, re.I) else 'MANUAL_REVIEW')
        elif Path(p).name == 'pom.xml' and p in texts:
            tree = ET.fromstring(texts[p]); ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
            for d in tree.findall('.//m:dependency', ns):
                get = lambda key: d.findtext('m:' + key, '', ns)
                name = get('groupId') + ':' + get('artifactId')
                dep_inventory.append({'path': p, 'name': name, 'version': get('version'), 'scope': get('scope') or 'compile_or_managed', 'decision': 'KEEP_CONSERVATIVE; graph/type/third-party consumers not completely resolved'})
                add('dependency-clean', p, 1, name, 'MAVEN_DECLARATION_INVENTORY', 'XML声明/管理项盘点；不是依赖未使用结论', 'dependency-inventory.json; source/runtime consumer graph incomplete', '注解处理/自动装配/SPI/序列化及间接消费须人工复核；本模式禁止删依赖', 'PROHIBITED_AUTO' if re.search(r'crypto|byte.?buddy|aws|snail|rabbit|kafka|redis|security|jackson|mybatis|starter', name, re.I) else 'MANUAL_REVIEW')
    dump(out / 'dependency-inventory.json', dep_inventory)
    stages.append({'skill': 'dependency-clean', 'status': 'PASS_WITH_LIMITATIONS', 'declarations': len(dep_inventory), 'unusedDependenciesProven': 0, 'mavenResolvedDependencyUsageAnalysis': 'NOT_RUN; declaration inventory only'})

    # 6: locate comment TODO/FIXME only; never infer obsolete from wording or age.
    for p, text in texts.items():
        if not p.endswith(('.java', '.vue', '.ts', '.js', '.py', '.ps1', '.sh')):
            continue
        for i, line in enumerate(text.splitlines(), 1):
            if re.search(r'(?:TODO|FIXME|XXX)\b', line) and re.search(r'//|/\*|^\s*\*|^\s*#|<!--', line):
                add('doc-comment-clean', p, i, 'TODO/FIXME/XXX', 'LEXICAL_COMMENT_CANDIDATE', '当前注释标记存在；不证明说明过时', '当前文件对应行；历史实现/接口消费者未完全核验', '无事实证据证明已过时，保留；协议/安全/工具指令禁止自动删除')
    stages.append({'skill': 'doc-comment-clean', 'status': 'PASS_WITH_LIMITATIONS', 'method': 'comment marker lexical candidates; no obsolete comment proven'})

    def csvout(name, selected):
        with (out / name).open('w', encoding='utf-8-sig', newline='') as handle:
            writer = csv.DictWriter(handle, fieldnames=FIELDS)
            writer.writeheader(); writer.writerows(selected)
    for c in ['CONFIRMED_REMOVABLE', 'MANUAL_REVIEW', 'PROHIBITED_AUTO']:
        csvout(c.lower() + '.csv', [r for r in rows if r['classification'] == c])
    for s in stages:
        selected = [r for r in rows if r['skill'] == s['skill']]
        s['classificationCounts'] = dict(Counter(r['classification'] for r in selected))
        csvout(s['skill'] + '.csv', selected)
    dump(out / 'coverage-inventory.json', coverage)
    previous = json.loads((out / 'tracked-sha256.json').read_text(encoding='utf-8'))
    changed = [p for p, h in previous.items() if hashes.get(p) != h]
    if changed:
        raise RuntimeError('Baseline changed while scanning: ' + repr(changed))
    summary = {'runId': baseline['runId'], 'phase': 2, 'mode': 'conservative', 'status': 'COMPLETED_WITH_EXPLICIT_COVERAGE_LIMITATIONS', 'stages': stages, 'classificationCounts': dict(Counter(r['classification'] for r in rows)), 'trackedFilesInventoried': len(coverage), 'baselineHashesMatched': len(previous), 'applicationModified': False, 'cleanupApplied': False, 'runtimeInstrumentation': 'NOT_RUN', 'compileAndUnitTests': 'NOT_RUN_PHASE2_READ_ONLY', 'nextPhases': 'NOT_AUTHORIZED_THIS_TURN', 'countsMeaning': 'tool candidates + review inventories + protected boundaries; not defect/dead-code counts'}
    dump(out / 'phase2-summary.json', summary)
    dump(out / 'phase1-gate.json', {'status': 'REVIEWED_READY_FOR_PHASE2_SCAN', 'baseline': baseline['runId'], 'startup': '9/9 PASS', 'toolchain': 'JDK21 / pnpm9.15.4 process-local', 'cleanupGate': 'NOT_APPROVED_FOR_PHASE5'})
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == '__main__':
    main()
