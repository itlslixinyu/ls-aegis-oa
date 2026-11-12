import test from 'node:test'
import assert from 'node:assert/strict'
import { createPhotoUploadSession, photoSubmissionError } from '../../ls-aegis-h5/src/utils/photoUploadSession.js'
import { validateUploadImage } from '../../ls-aegis-h5/src/utils/uploadImage.js'

const png = () => new File([new Uint8Array([137,80,78,71,13,10,26,10,1])], 'camera', { type: '' })
const tick = () => new Promise(resolve => setImmediate(resolve))
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }

test('byte validation normalizes native names and rejects empty, unsupported and oversized files', async () => {
  const file = await validateUploadImage(png())
  assert.equal(file.name, 'camera.png'); assert.equal(file.type, 'image/png')
  await assert.rejects(validateUploadImage(new File([], 'empty.png')), /非空/)
  await assert.rejects(validateUploadImage(new File(['plain text'], 'pretend.jpg', { type: 'image/jpeg' })), /真实/)
  await assert.rejects(validateUploadImage({ size: 10 * 1024 * 1024 + 1, slice() {} }), /10MB/)
})

test('queue bounds concurrency, deduplicates double clicks and preserves every successful ID', async () => {
  const items = Array.from({ length: 5 }, () => ({ file: png() }))
  const gates = []; let active = 0; let peak = 0; let calls = 0
  const session = createPhotoUploadSession({ getItems: () => items, upload: async () => {
    const id = ++calls; active++; peak = Math.max(peak, active)
    const gate = deferred(); gates.push(gate); await gate.promise; active--
    return { data: { fileId: String(id + 100) } }
  } })
  const promises = items.map(item => session.enqueue(item))
  assert.equal(session.enqueue(items[0]), promises[0])
  await tick(); assert.equal(calls, 2)
  for (let i = 0; i < 5; i++) { while (!gates[i]) await tick(); gates[i].resolve(); await tick() }
  await Promise.all(promises); assert.equal(peak, 2); assert.equal(calls, 5)
  assert.ok(items.every(item => item.status === 'done' && item.fileId)); assert.equal(new Set(items.map(item => item.fileId)).size, 5)
  session.dispose()
})

test('failed upload remains visible, blocks submission and can retry with the original photo category', async () => {
  const item = { file: png() }; let calls = 0; const contexts = []
  const session = createPhotoUploadSession({ getItems: () => [item], upload: async (file, context) => {
    contexts.push(context.photoType); if (++calls === 1) throw new Error('network unavailable')
    return { data: { fileId: '9007199254740993' } }
  } })
  await session.enqueue(item, { photoType: 'completed' })
  assert.equal(item.status, 'failed'); assert.match(photoSubmissionError([item]), /重试或删除/)
  await session.retry(item); assert.equal(item.fileId, '9007199254740993')
  assert.equal(photoSubmissionError([item], 1), ''); assert.deepEqual(contexts, ['completed', 'completed'])
})

test('removal aborts a request and ignores its late successful response', async () => {
  const gate = deferred(); const item = { file: png() }; const items = [item]; let signal
  const session = createPhotoUploadSession({ getItems: () => items, upload: async (file, context, options) => { signal = options.signal; return gate.promise } })
  const pending = session.enqueue(item); await tick(); session.remove(item); items.splice(0)
  assert.equal(signal.aborted, true); gate.resolve({ data: { fileId: '123' } }); await pending
  assert.equal(item.fileId, undefined)
})

test('preview failure preserves upload success; disposal releases acquired previews once', async () => {
  const item = { file: png() }; const released = []
  const session = createPhotoUploadSession({ getItems: () => [item], upload: async () => ({ data: { fileId: '123' } }), preview: async () => true, release: id => released.push(id) })
  await session.enqueue(item); session.dispose(); session.dispose(); assert.deepEqual(released, ['123'])
  const second = { file: png() }
  const failedPreview = createPhotoUploadSession({ getItems: () => [second], upload: async () => ({ data: { fileId: '456' } }), preview: async () => { throw Error('preview unavailable') } })
  await failedPreview.enqueue(second); assert.equal(second.status, 'done')
})

test('disposal during preview acquisition releases the late preview and settles queued work', async () => {
  const gate = deferred(); const items = [{ file: png() }, { file: png() }]; const released = []
  const session = createPhotoUploadSession({ concurrency: 1, getItems: () => items, upload: async () => ({ data: { fileId: '123' } }), preview: () => gate.promise, release: id => released.push(id) })
  const one = session.enqueue(items[0]); const two = session.enqueue(items[1]); await tick()
  session.dispose(); gate.resolve(true); await Promise.all([one, two]); assert.deepEqual(released, ['123'])
  assert.equal(items[1].fileId, undefined)
})

test('unsafe numeric IDs and filename-only responses cannot become successful photo references', async () => {
  for (const payload of [{ fileName: 'photo.jpg' }, { fileId: 9007199254740992 }]) {
    const item = { file: png() }
    const session = createPhotoUploadSession({ getItems: () => [item], upload: async () => ({ data: payload }) })
    await session.enqueue(item); assert.equal(item.status, 'failed'); assert.equal(item.fileId, undefined)
  }
  assert.match(photoSubmissionError([], 1), /至少/)
  assert.equal(photoSubmissionError([]), '')
  assert.match(photoSubmissionError([{ status: 'uploading' }]), /等待/)
})
