"""Prepare a clean, reviewable source candidate; never creates/pushes a Git repository."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-id', required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'\d{8}_\d{6}',args.run_id):
        parser.error('Invalid run id')
    run = ROOT/'.local/repo-organize'/args.run_id
    if not (run/'baseline.json').is_file():
        parser.error('Existing baseline required')
    destination = run/'candidate/ls-aegis-oa'
    if destination.exists():
        parser.error('Candidate exists; do not overwrite reviewed work')
    assert destination.resolve().is_relative_to(run.resolve())
    names = set()
    for command in [('ls-files','-z'),('ls-files','--others','--exclude-standard','-z')]:
        names.update(filter(None,subprocess.check_output(['git',*command],cwd=ROOT).decode().split('\0')))
    forbidden_parts = {'.git','.local','node_modules','target','dist','dist-engineer','.gradle','backups','logs'}
    records,excluded = [],[]
    for name in sorted(names):
        path = ROOT/name
        public_env = path.name.endswith('.example') or path.name in {'.env.customer','.env.engineer','.env.development','.env.production','.env.test'}
        if set(path.relative_to(ROOT).parts)&forbidden_parts or name.startswith('docker/data/') or (path.name.startswith('.env') and not public_env) or path.suffix.lower() in {'.pem','.key','.p12','.pfx','.jks','.keystore','.log','.apk'} or name=='scripts/deploy-info.json':
            excluded.append(name)
            continue
        if path.is_symlink() or path.is_junction():
            parser.error('Review symlink before exporting: '+name)
        if not path.is_file():
            continue
        target=destination/name
        assert target.resolve().is_relative_to(destination.resolve())
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(path,target)
        records.append({'path':name,'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
    assert any(item['path']=='android-demo/gradle/wrapper/gradle-wrapper.jar' for item in records)
    provenance={'source_repository':ROOT.name,'source_head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip(),
                'source_is_dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT).strip()),'product':'LS·Aegis OA 管理平台','version':(ROOT/'VERSION').read_text().strip(),
                'candidate':str(destination),'files':records,'excluded_tracked_or_untracked':excluded,
                'history_policy':'原仓库和历史保留原位；候选仅工作树文件，不包含.git，不宣称已经建立或发布新Git历史。',
                'published':False}
    (run/'release-manifest.json').write_text(json.dumps(provenance,ensure_ascii=False,indent=2),encoding='utf-8')
    print(f'Prepared {len(records)} source files at {destination}; excluded {len(excluded)}; no Git/network mutation')


if __name__ == '__main__':
    main()
