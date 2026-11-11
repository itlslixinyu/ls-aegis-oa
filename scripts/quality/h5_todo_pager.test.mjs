import test from 'node:test'
import assert from 'node:assert/strict'
import { createTodoPager } from '../../ls-aegis-h5/src/utils/todoPager.js'

test('pages advance only after a successful full page, and failed pages can be retried', async () => {
  const calls = []
  let fail = true
  const pager = createTodoPager(async params => {
    calls.push(params.page)
    if (params.page === 2 && fail) { fail = false; throw new Error('offline') }
    return { data: { list: params.page === 1 ? [{ id: '1' }, { id: '2' }] : [] } }
  }, 2)
  assert.equal((await pager.load('PROCESSING')).finished, false)
  await assert.rejects(pager.load('PROCESSING'))
  assert.equal((await pager.load('PROCESSING')).finished, true)
  assert.deepEqual(calls, [1, 2, 2])
})

test('category reset discards a stale page without advancing the new category', async () => {
  let complete
  const calls = []
  const pager = createTodoPager(params => {
    calls.push(params)
    if (params.status === 'WAIT_ACCEPT') return new Promise(resolve => { complete = resolve })
    return { data: { list: [] } }
  })
  const stale = pager.load('WAIT_ACCEPT')
  assert.equal(stale, pager.load('WAIT_ACCEPT'))
  await Promise.resolve()
  pager.reset()
  assert.equal((await pager.load('PROCESSING')).finished, true)
  complete({ data: { list: [{ id: 'old' }] } })
  assert.equal(await stale, null)
  assert.deepEqual(calls.map(x => [x.status, x.page]), [['WAIT_ACCEPT', 1], ['PROCESSING', 1]])
})

test('invalid response is an error rather than a false empty-state success', async () => {
  await assert.rejects(createTodoPager(async () => ({ data: {} })).load())
})
