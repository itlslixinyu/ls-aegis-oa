import http from '@/utils/http'

export function uploadSignFile(file: File, bizType = 'sign') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return http.post<any>('/sign/file/upload', formData)
}

export function getSignFileInfo(fileId: string) {
  return http.get<any>(`/sign/file/${fileId}`)
}

export function getAllowedExtensions() {
  return http.get<string[]>('/sign/file/allowed')
}

export function getSignFileDownloadResource(fileId: string) {
  return `/sign/file/download/${fileId}`
}

export function downloadSignFile(fileId: string) {
  return http.download(getSignFileDownloadResource(fileId))
}
