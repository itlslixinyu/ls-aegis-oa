const labels: Record<string, string> = {
  TRANSFER: '银行转账',
  CASH: '现金',
  CHECK: '支票',
  OTHER: '其他',
}

export function formatPaymentMethod(value?: string | null) {
  return value ? labels[value] || value : '-'
}
