import test from 'node:test'
import assert from 'node:assert/strict'
import { fitHeadingSize } from '../../ls-aegis-h5/src/utils/fitHeading.js'

test('long names shrink to the available single line without ellipsis', () => {
  const size = fitHeadingSize(420, 23, 300)
  assert.ok(size >= 12 && size < 23)
  assert.ok(420 * size / 23 <= 300)
})
test('shorter names and wider layouts can grow back to the standard size', () => {
  assert.equal(fitHeadingSize(80, 12, 300), 23)
})
test('extreme names use the readable minimum and allow the component to scroll', () => {
  assert.equal(fitHeadingSize(9000, 23, 280), 12)
  assert.equal(fitHeadingSize(0, 23, 0), 23)
})
