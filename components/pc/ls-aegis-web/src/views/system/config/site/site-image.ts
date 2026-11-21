export interface SiteImageSize {
  maxEdge: number
  square?: boolean
}

const MAX_FILE_BYTES = 25 * 1024 * 1024
const MAX_IMAGE_PIXELS = 64_000_000

export async function convertSiteImage(file: File, size: SiteImageSize): Promise<File> {
  if (file.size > MAX_FILE_BYTES) throw new Error('图片不能超过 25 MB')

  const source = /\.svg$/i.test(file.name) ? file.slice(0, file.size, 'image/svg+xml') : file
  const url = URL.createObjectURL(source)
  try {
    const image = new Image()
    image.src = url
    try {
      await image.decode()
    } catch {
      throw new Error('当前浏览器无法读取这张图片，请换用 PNG、JPEG 或 WebP')
    }

    const { naturalWidth, naturalHeight } = image
    if (!naturalWidth || !naturalHeight || naturalWidth * naturalHeight > MAX_IMAGE_PIXELS) {
      throw new Error('图片尺寸无效或超过 6400 万像素')
    }

    const scale = size.maxEdge / Math.max(naturalWidth, naturalHeight)
    const ratio = size.square ? scale : Math.min(scale, 1)
    const width = Math.max(1, Math.round(naturalWidth * ratio))
    const height = Math.max(1, Math.round(naturalHeight * ratio))
    const canvas = document.createElement('canvas')
    canvas.width = size.square ? size.maxEdge : width
    canvas.height = size.square ? size.maxEdge : height
    const context = canvas.getContext('2d')
    if (!context) throw new Error('当前浏览器无法转换图片')
    context.imageSmoothingQuality = 'high'
    context.drawImage(image, (canvas.width - width) / 2, (canvas.height - height) / 2, width, height)

    const png = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (blob) resolve(blob)
        else reject(new Error('图片转换失败，请换一张图片'))
      }, 'image/png')
    })
    const baseName = file.name.replace(/\.[^.]+$/, '') || 'site-image'
    return new File([png], `${baseName}.png`, { type: 'image/png' })
  } finally {
    URL.revokeObjectURL(url)
  }
}
