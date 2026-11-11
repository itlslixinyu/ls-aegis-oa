"""Normalize the fresh-install artifact; never connect to an existing database."""
import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / 'ls-aegis-server/sql/init.sql'


def statements(source):
    start = 0
    quote = None
    line_comment = block_comment = False
    index = 0
    while index < len(source):
        char = source[index]
        pair = source[index:index + 2]
        if line_comment:
            if char == '\n': line_comment = False
        elif block_comment:
            if pair == '*/': block_comment = False; index += 1
        elif quote:
            if char == '\\': index += 1
            elif char == quote:
                if index + 1 < len(source) and source[index + 1] == quote: index += 1
                else: quote = None
        elif pair == '--' or char == '#': line_comment = True
        elif pair == '/*': block_comment = True; index += 1
        elif char in "'\"`": quote = char
        elif char == ';':
            yield source[start:index + 1]
            start = index + 1
        index += 1
    if source[start:]: yield source[start:]


def tuples(source):
    depth = 0
    quote = None
    start = None
    index = 0
    while index < len(source):
        char = source[index]
        if quote:
            if char == '\\': index += 1
            elif char == quote:
                if index + 1 < len(source) and source[index + 1] == quote: index += 1
                else: quote = None
        elif char in "'\"": quote = char
        elif char == '(':
            if depth == 0: start = index
            depth += 1
        elif char == ')':
            depth -= 1
            if depth == 0:
                yield source[start:index + 1]
        elif depth == 0 and char not in ',; \r\n\t':
            raise ValueError('Unsupported seed tuple syntax')
        index += 1
    if depth or quote: raise ValueError('Unterminated seed tuple')


def clean(source):
    result = []
    for statement in statements(source):
        match = re.search(r'^INSERT\s+INTO\s+`?(sys_user_role|sys_user|sys_dept|sys_notice)`?\s*\(', statement, re.M | re.I)
        if not match:
            result.append(statement)
            continue
        table = match.group(1).lower()
        if table == 'sys_notice':
            continue
        values = re.search(r'\bVALUES\b', statement[match.end():], re.I)
        if not values: raise ValueError('Unsupported seed INSERT form')
        cut = match.end() + values.end()
        keep = []
        for row in tuples(statement[cut:]):
            ids = re.match(r'\(\s*(\d+)\s*,\s*(\d+)', row) if table == 'sys_user_role' else re.match(r'\(\s*(\d+)', row)
            if not ids: raise ValueError('Seed identity must be a literal integer')
            identity = int(ids.group(2 if table == 'sys_user_role' else 1))
            if identity == 1: keep.append(row)
        if not keep: raise ValueError(f'Mandatory administrator seed missing: {table}')
        result.append(statement[:cut] + '\n' + ',\n'.join(keep) + ';')
    return ''.join(result)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    source = SQL.read_text(encoding='utf-8-sig')
    normalized = clean(source)
    if args.write:
        SQL.write_text(normalized, encoding='utf-8', newline='\n')
    elif normalized != source:
        raise SystemExit('FAIL: fresh-install artifact still includes non-admin users, extra departments or seeded notices')
    print('PASS fresh-install seed identities; no database connection performed')


if __name__ == '__main__': main()
