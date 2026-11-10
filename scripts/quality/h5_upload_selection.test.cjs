const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

// Exercise the real component functions, controlling only external services and Vue refs.
function fixture(role, services = {}) {
  const source = fs.readFileSync(`ls-aegis-h5/src/views/${role === 'customer' ? 'customer/Apply' : 'engineer/Record'}.vue`, 'utf8')
    .match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')
  const sent = [], messages = []
  const submit = async form => { sent.push(JSON.parse(JSON.stringify(form))); return services.submit ? services.submit(form) : { success: true } }
  const names = ['ref', 'reactive', 'computed', 'useRouter', 'useRoute', 'showSuccessToast', 'showFailToast',
    'openEomsImagePreview', 'bindUploaderWatermarkPreview', 'submitApply', 'uploadWithWatermark', 'faultTypeOptions',
    'getCurrentPosition', 'getTaskDetail', 'submitRecord', 'uploadRepairPhoto', 'getOrderStatusText', 'toBizId', 'setTimeout']
  const values = [v => ({ value: v }), v => v, fn => ({ get value() { return fn() } }),
    () => ({ replace() {}, back() {} }), () => ({ params: { orderId: '123' } }), () => {}, msg => messages.push(msg),
    async () => {}, services.preview || (async () => {}), submit, services.upload, [], () => {},
    async () => ({ data: { order: {} } }), submit, services.upload, () => '', String, () => {}]
  const expose = role === 'customer' ? 'fileList' : 'processingFileList,completedFileList'
  const view = new Function(...names, source + `\nreturn {${expose},uploadOne,beforeDelete,onSubmit,loading}`)(...values)
  return { view, sent, messages }
}

test('customer: upload completion order does not change which remaining photo is submitted', async () => {
  const a = deferred(), b = deferred()
  const { view, sent } = fixture('customer', { upload: file => file.name === 'A' ? a.promise : b.promise })
  const A = { file: { name: 'A' } }, B = { file: { name: 'B' } }; view.fileList.value = [A, B]
  const pa = view.uploadOne(A), pb = view.uploadOne(B)
  b.resolve({ data: { fileId: '102' } }); await pb
  a.resolve({ data: { fileId: '101' } }); await pa
  assert.equal(view.beforeDelete(A), true); view.fileList.value.splice(0, 1)
  await view.onSubmit(); assert.equal(sent[0].photos, '102')
})

test('customer: deleting a failed item preserves the successful photo', async () => {
  const { view, sent } = fixture('customer', { upload: async () => ({ data: { fileId: '102' } }) })
  const failed = { status: 'failed' }, B = { file: { name: 'B' } }; view.fileList.value = [failed, B]
  await view.uploadOne(B); assert.equal(view.beforeDelete(failed), true); view.fileList.value.splice(0, 1)
  await view.onSubmit(); assert.equal(sent[0].photos, '102')
})

test('customer: pending uploads block submission/deletion, then submit succeeds', async () => {
  const upload = deferred(); const { view, sent } = fixture('customer', { upload: () => upload.promise })
  const A = { file: {} }; view.fileList.value = [A]; const pending = view.uploadOne(A)
  assert.equal(view.beforeDelete(A), false); await view.onSubmit(); assert.equal(sent.length, 0)
  upload.resolve({ data: { fileId: '101' } }); await pending
  await view.onSubmit(); assert.equal(sent[0].photos, '101')
})

test('customer: late upload of an externally removed item cannot revive a selected photo', async () => {
  const upload = deferred(); const { view, sent } = fixture('customer', { upload: () => upload.promise })
  const A = { file: {} }; view.fileList.value = [A]; const pending = view.uploadOne(A)
  view.fileList.value = []; upload.resolve({ data: { fileId: '101' } }); await pending
  assert.equal(A.fileId, undefined); await view.onSubmit(); assert.equal(sent.length, 0)
})

test('engineer: deleted processing/completed photos are omitted from the submitted record', async () => {
  const { view, sent } = fixture('engineer', { upload: async file => ({ data: { fileId: file.id } }) })
  const A = { file: { id: '201' } }, B = { file: { id: '202' } }, C = { file: { id: '203' } }
  view.processingFileList.value = [A, B]; view.completedFileList.value = [C]
  await Promise.all([view.uploadOne(A, 'processing'), view.uploadOne(B, 'processing'), view.uploadOne(C, 'completed')])
  assert.equal(view.beforeDelete(A), true); view.processingFileList.value.splice(0, 1)
  assert.equal(view.beforeDelete(C), true); view.completedFileList.value.splice(0, 1)
  await view.onSubmit(); assert.equal(sent[0].processingPhotos, '202'); assert.equal(sent[0].completedPhotos, '')
})

test('engineer: pending and late removed uploads do not silently become record attachments', async () => {
  const upload = deferred(); const { view, sent } = fixture('engineer', { upload: () => upload.promise })
  const A = { file: {} }; view.processingFileList.value = [A]; const pending = view.uploadOne(A, 'processing')
  assert.equal(view.beforeDelete(A), false); await view.onSubmit(); assert.equal(sent.length, 0)
  view.processingFileList.value = []; upload.resolve({ data: { fileId: '201' } }); await pending
  await view.onSubmit(); assert.equal(sent[0].processingPhotos, '')
})

test('engineer: pending record submission cannot be submitted a second time', async () => {
  const response = deferred(); const { view, sent } = fixture('engineer', { submit: () => response.promise })
  const first = view.onSubmit(); await view.onSubmit(); assert.equal(sent.length, 1)
  response.resolve({ success: true }); await first; assert.equal(view.loading.value, false)
})

test('successful upload stays selectable when the real watermark preview helper cannot fetch its image', async () => {
  const helper = fs.readFileSync('ls-aegis-h5/src/utils/eomsFile.js', 'utf8')
    .replace(/\r\n/g, '\n')
    .match(/export async function bindUploaderWatermarkPreview\([\s\S]*?\n}\n/)[0].replace('export ', '')
  const preview = new Function('needsAuthFetch', 'extractFileId', 'fetchEomsFileObjectUrl', 'console', helper + '\nreturn bindUploaderWatermarkPreview')(
    () => true, () => '301', async () => { throw new Error('controlled preview failure') }, { warn() {} })
  const { view, sent } = fixture('customer', { upload: async () => ({ data: { fileId: '301' } }), preview })
  const A = { file: {} }; view.fileList.value = [A]; await view.uploadOne(A)
  assert.equal(A.status, 'done'); await view.onSubmit(); assert.equal(sent[0].photos, '301')
})

test('customer: pending form submission cannot send the same selection twice', async () => {
  const response = deferred(); const { view, sent } = fixture('customer', { submit: () => response.promise })
  view.fileList.value = [{ status: 'done', fileId: '401' }]
  const first = view.onSubmit(); await view.onSubmit(); assert.equal(sent.length, 1)
  response.resolve({ success: true }); await first
})
