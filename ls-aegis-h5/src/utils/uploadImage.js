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
