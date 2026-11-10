import http from '@/utils/http'

export interface PaymentResp {
  id: string
  paymentNo?: string
  customerId?: string
  customerName?: string
  amount?: number
  paymentMethod?: string
  paymentTime?: string
  status?: string
  createTime?: string
  remark?: string
}

const RECEIPT_URL = '/eoms/finance/receipt'

export function listPayment(query: any) {
  return http.get<PageRes<PaymentResp[]>>(`${RECEIPT_URL}/list`, query)
}

export function getPayment(id: string) {
  return http.get<PaymentResp>(`${RECEIPT_URL}/${id}`)
}

export function addPayment(data: any) {
  return http.post(RECEIPT_URL, data)
}

export function updatePayment(data: any) {
  return http.put(RECEIPT_URL, data)
}

export function deletePayment(id: string) {
  return http.del(`${RECEIPT_URL}/${id}`)
}

export function verifyPayment(data: any) {
  return http.post(`${RECEIPT_URL}/verify`, {
    ...data,
    idempotentToken: data?.idempotentToken || crypto.randomUUID(),
  })
}

export function verifyPaymentBatch(data: any) {
  return http.post(`${RECEIPT_URL}/verify/batch`, {
    ...data,
    idempotentToken: data?.idempotentToken || crypto.randomUUID(),
  })
}
