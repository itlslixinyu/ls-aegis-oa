"""只读统计 Git 跟踪源码，不把依赖、构建产物和数据卷算作业务体积。"""
from collections import Counter
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def main():
    names = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode('utf-8').split('\0')
    counts, sizes, lines, largest = Counter(), Counter(), Counter(), []
    tests = 0
    non_utf8 = []
    for name in filter(None, names):
        path = ROOT / name
        if not path.is_file():
            continue
        group = name.split('/')[0]
        counts[group] += 1
        sizes[group] += path.stat().st_size
        if path.suffix in {'.java', '.vue', '.ts', '.tsx', '.js', '.scss', '.sql'}:
            raw = path.read_bytes()
            try:
                text = raw.decode('utf-8-sig')
            except UnicodeDecodeError:
                non_utf8.append(name)
                text = raw.decode('utf-16') if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else raw.decode('utf-8', errors='replace')
            n = len(text.splitlines())
            lines[group] += n
            largest.append((n, name))
        tests += name.endswith('Test.java')
    print('| 目录 | 跟踪文件 | 字节 | 源码行数 |\n|---|---:|---:|---:|')
    for group in sorted(counts, key=sizes.get, reverse=True):
        print(f'| {group} | {counts[group]} | {sizes[group]} | {lines[group]} |')
    print(f'\nJava Test.java 文件：{tests}；数量不代表覆盖率。')
    print('\n源码行数最多的 12 个文件（仅定位候选，不代表可删除）：')
    for n, name in sorted(largest, reverse=True)[:12]:
        print(f'{n:6}  {name}')
    if non_utf8:
        print('\n非 UTF-8 源文件（统计时兼容读取，不自动改编码）：')
        print('\n'.join(non_utf8))


if __name__ == '__main__':
    main()
