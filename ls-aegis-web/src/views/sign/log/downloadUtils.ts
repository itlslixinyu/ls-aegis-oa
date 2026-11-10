export function createDownloadFileName(row: { signedFileName?: string, fileName?: string } = {}) {
  return row.signedFileName || row.fileName || 'signed.pdf'
}

export function createReceiptDownloadFileName(receipt: { receiptNo?: string, recordId?: string | number } = {}) {
  if (receipt.receiptNo) return `${receipt.receiptNo}.pdf`
  if (receipt.recordId) return `receipt-${receipt.recordId}.pdf`
  return 'receipt.pdf'
}

export function triggerBlobDownload(
  blob: Blob,
  fileName: string,
  { documentApi = document, urlApi = window.URL } = {},
) {
  const url = urlApi.createObjectURL(blob)
  const link = documentApi.createElement('a')
  link.href = url
  link.download = fileName
  documentApi.body.appendChild(link)
  link.click()
  documentApi.body.removeChild(link)
  urlApi.revokeObjectURL(url)
}
