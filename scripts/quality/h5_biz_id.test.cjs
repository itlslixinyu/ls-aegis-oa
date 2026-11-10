const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const source = fs.readFileSync('ls-aegis-h5/src/utils/biz-id.js', 'utf8')
const { toBizId, toBizIdList } = new Function(source.replaceAll('export ', '') + '\nreturn {toBizId,toBizIdList}')()

test('H5 selection ignores null entries without inventing null/undefined business IDs', () => {
  assert.deepEqual(toBizIdList([null, undefined, '', '  ', '909017964387238520']), ['909017964387238520'])
})
test('H5 snowflake ID retains every digit through scalar and list normalization', () => {
  const id = '9223372036854775807'
  assert.equal(toBizId('  ' + id + ' '), id)
  assert.deepEqual(toBizIdList(id + ', 909017964387238520, '), [id, '909017964387238520'])
})
test('H5 empty selections and zero follow scalar ID rules consistently', () => {
  for (const input of [null, undefined, '', '   ']) {
    assert.equal(toBizId(input), undefined)
    assert.deepEqual(toBizIdList(input), [])
  }
  assert.equal(toBizId(0), '0')
  assert.deepEqual(toBizIdList([0, ' 0 ']), ['0', '0'])
})
