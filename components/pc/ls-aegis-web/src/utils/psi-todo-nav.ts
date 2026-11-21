/** PSI 待办深链：按 todoType + bizType 跳到对应业务页 */

export function todoTypeLabel(v?: string | null): string {
  const map: Record<string, string> = {
    RED_INVOICE: '待冲红',
    REVERSE_WRITEOFF: '待反核销',
    REFUND: '待退款',
    RECEIVE_REFUND: '待收退款',
  }
  return v ? (map[v] || v) : '-'
}

export function bizTypeLabel(v?: string | null): string {
  const map: Record<string, string> = {
    SALE_RETURN: '销售退货',
    PURCHASE_RETURN: '采购退货',
    STOCK_RETURN: '退货单',
  }
  return v ? (map[v] || v) : '-'
}

export function resolvePsiTodoRoute(record: {
  todoType?: string
  bizType?: string
  bizNo?: string
}): { path: string, query?: Record<string, string> } {
  const purchase = record.bizType === 'PURCHASE_RETURN'
  // 旧数据 bizType=STOCK_RETURN 按销售退货兼容
  const docNo = record.bizNo || undefined

  switch (record.todoType) {
    case 'RED_INVOICE':
      return purchase
        ? { path: '/psi/invoice-in' }
        : { path: '/psi/invoice-out' }
    case 'REVERSE_WRITEOFF':
      return purchase
        ? { path: '/psi/payment' }
        : { path: '/psi/receipt' }
    case 'REFUND':
      // 销售退货退款 → 付款单
      return { path: '/psi/payment' }
    case 'RECEIVE_REFUND':
      // 采购退货收退款 → 回款单
      return { path: '/psi/receipt' }
    default:
      return {
        path: '/psi/stock-return',
        query: docNo ? { docNo } : undefined,
      }
  }
}

export function todoActionLabel(todoType?: string | null): string {
  switch (todoType) {
    case 'RED_INVOICE':
      return '去冲红'
    case 'REVERSE_WRITEOFF':
      return '去反核销'
    case 'REFUND':
      return '去退款'
    case 'RECEIVE_REFUND':
      return '去收退款'
    default:
      return '去退货'
  }
}
