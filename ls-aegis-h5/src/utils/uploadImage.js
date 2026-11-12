/**
 * 规范化移动端拍照上传的文件名。
 * 部分浏览器（尤其手机相机）返回无扩展名或名为 blob 的 File，后端会拒绝。
 */
export function normalizeUploadImageFile(file) {
  if (!file) return file
  const rawName = (file.name || '').trim()
  const lower = rawName.toLowerCase()
  if (/\.(jpe?g|png)$/.test(lower)) {
    return file
  }
  const mime = (file.type || '').toLowerCase()
  let ext = '.jpg'
  if (mime.includes('png')) {
    ext = '.png'
  } else if (mime.includes('jpeg') || mime.includes('jpg')) {
    ext = '.jpg'
  }
  const base = rawName && rawName !== 'blob' && !rawName.startsWith('.')
    ? rawName.replace(/\.[^.]+$/, '')
    : 'photo'
  return new File([file], `${base}${ext}`, { type: file.type || 'image/jpeg' })
}

/** Validate bytes before uploading; changing a suffix is never format conversion. */
export async function validateUploadImage(file) {
  if (!file || typeof file.slice !== 'function' || !file.size) throw new Error('请选择非空的图片文件')
  if (file.size > 10 * 1024 * 1024) throw new Error('图片大小不能超过 10MB')
  const bytes = new Uint8Array(await file.slice(0, 16).arrayBuffer())
  const png = [137, 80, 78, 71, 13, 10, 26, 10].every((value, index) => bytes[index] === value)
  const jpeg = bytes.length >= 3 && bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255
  if (!png && !jpeg) throw new Error('仅支持真实的 JPG/PNG 图片，请转换格式后再上传')
  const type = png ? 'image/png' : 'image/jpeg'
  const extension = png ? '.png' : '.jpg'
  const name = (file.name || 'photo').replace(/\.[^.]*$/, '') + extension
  return new File([file], name, { type, lastModified: file.lastModified })
}
