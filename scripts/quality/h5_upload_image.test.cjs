const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const source = fs.readFileSync('ls-aegis-h5/src/utils/uploadImage.js', 'utf8')
const normalize = new Function('File', source.replace(/export /g, '') + '\nreturn normalizeUploadImageFile')(File)

test('camera blob receives JPEG extension without changing uploaded bytes', async () => {
  const file = new File([new Uint8Array([1,2,3,255])], 'blob', { type: 'image/jpeg' })
  const result = normalize(file)
  assert.equal(result.name, 'photo.jpg'); assert.equal(result.type, 'image/jpeg')
  assert.deepEqual(Buffer.from(await result.arrayBuffer()), Buffer.from([1,2,3,255]))
})
test('PNG camera filename preserves PNG MIME and content', async () => {
  const file = new File(['synthetic'], 'camera', { type: 'image/png' })
  const result = normalize(file)
  assert.equal(result.name, 'camera.png'); assert.equal(result.type, 'image/png')
  assert.equal(await result.text(), 'synthetic')
})
test('existing uppercase image suffix keeps the original File object', () => {
  for (const name of ['PHOTO.JPG','photo.JPEG','photo.PNG']) {
    const file = new File(['synthetic'], name, { type: 'image/jpeg' })
    assert.equal(normalize(file), file)
  }
})
test('missing file and hidden filename do not create invalid empty base names', () => {
  assert.equal(normalize(undefined), undefined); assert.equal(normalize(null), null)
  assert.equal(normalize(new File(['x'], '.camera', { type: 'image/png' })).name, 'photo.png')
})
