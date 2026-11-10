// Read-only TypeScript/Vue AST evidence. No project configuration or source writes.
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve, relative, isAbsolute } from 'node:path'
import { createRequire } from 'node:module'
import { execFileSync } from 'node:child_process'
const root = resolve(import.meta.dirname, '../..')
const out = resolve(root, process.argv[2] || '')
const rel = relative(resolve(root, '.local/conservative-cleanup'), out)
if (!rel || rel.startsWith('..') || isAbsolute(rel)) throw Error('Invalid evidence directory')
const req = createRequire(resolve(root, 'ls-aegis-web/package.json'))
const vueReq = createRequire(req.resolve('vue/package.json'))
const ts = req('typescript'), { parse } = vueReq('@vue/compiler-sfc')
const files = execFileSync('git', ['ls-files', '-z'], { cwd: root, encoding: 'utf8' }).split('\0').filter(p => /^ls-aegis-(web|h5)\//.test(p) && /\.(vue|tsx?|[cm]?js)$/.test(p))
const results = []
for (const file of files) {
  const source = readFileSync(resolve(root, file), 'utf8'), item = { file, imports: [], branches: [], parseErrors: [] }
  let blocks = [{ content: source, offset: 0, kind: /\.tsx$/.test(file) ? ts.ScriptKind.TSX : ts.ScriptKind.TS }]
  if (file.endsWith('.vue')) {
    const parsed = parse(source, { filename: file })
    item.sfcErrors = parsed.errors.map(String)
    blocks = [parsed.descriptor.script, parsed.descriptor.scriptSetup].filter(Boolean).map(b => ({ content: b.content, offset: b.loc.start.line - 1, kind: b.lang === 'tsx' ? ts.ScriptKind.TSX : ts.ScriptKind.TS }))
  }
  for (const b of blocks) {
    const ast = ts.createSourceFile(file, b.content, ts.ScriptTarget.Latest, true, b.kind)
    item.parseErrors.push(...ast.parseDiagnostics.map(d => ({ line: ast.getLineAndCharacterOfPosition(d.start || 0).line + 1 + b.offset, message: ts.flattenDiagnosticMessageText(d.messageText, ' ') })))
    function walk(n) {
      const line = ast.getLineAndCharacterOfPosition(n.getStart(ast)).line + 1 + b.offset
      if ((ts.isImportDeclaration(n) || ts.isExportDeclaration(n)) && n.moduleSpecifier && ts.isStringLiteral(n.moduleSpecifier)) item.imports.push({ specifier: n.moduleSpecifier.text, line, typeOnly: Boolean(n.importClause?.isTypeOnly) })
      if (ts.isCallExpression(n) && n.arguments.length && ts.isStringLiteral(n.arguments[0]) && (n.expression.kind === ts.SyntaxKind.ImportKeyword || n.expression.getText(ast) === 'require')) item.imports.push({ specifier: n.arguments[0].text, line, dynamic: true })
      if (ts.isIfStatement(n) || ts.isWhileStatement(n) || ts.isDoStatement(n) || ts.isConditionalExpression(n)) {
        const c = n.expression || n.condition
        if (c && [ts.SyntaxKind.TrueKeyword, ts.SyntaxKind.FalseKeyword].includes(c.kind)) item.branches.push({ line, condition: c.getText(ast), node: ts.SyntaxKind[n.kind], proof: 'AST literal boolean only; side effects/control-flow NOT proven safe' })
      }
      ts.forEachChild(n, walk)
    }
    walk(ast)
  }
  results.push(item)
}
writeFileSync(resolve(out, 'frontend-ast.json'), JSON.stringify({ tool: 'TypeScript AST / Vue SFC parser', typescript: ts.version, typeChecked: false, results }, null, 2))
console.log(JSON.stringify({ files: results.length, literalBranches: results.reduce((n, x) => n + x.branches.length, 0), parseErrorFiles: results.filter(x => x.parseErrors.length || x.sfcErrors?.length).length }))
