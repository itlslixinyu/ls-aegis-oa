import type * as T from './type'
import http from '@/utils/http'

export type * from './type'

const UNIT_URL = '/psi/unit'
const GOODS_URL = '/psi/goods'
const GOODS_CATEGORY_URL = '/psi/goods-category'
const WAREHOUSE_URL = '/psi/warehouse'

export function listUnit(query: T.UnitPageQuery) {
  return http.get<PageRes<T.UnitResp[]>>(UNIT_URL, query)
}
export function getUnit(id: string) {
  return http.get<T.UnitResp>(`${UNIT_URL}/${id}`)
}
export function addUnit(data: any) {
  return http.post(UNIT_URL, data)
}
export function updateUnit(data: any, id: string) {
  return http.put(`${UNIT_URL}/${id}`, data)
}
export function deleteUnit(id: string) {
  return http.del(UNIT_URL, { ids: [id] })
}
export function exportUnit(query: T.UnitPageQuery) {
  return http.download(`${UNIT_URL}/export`, query)
}
export function downloadUnitImportTemplate() {
  return http.download(`${UNIT_URL}/import/template`)
}
export function parseImportUnit(data: FormData) {
  return http.post(`${UNIT_URL}/import/parse`, data)
}
export function importUnit(data: any) {
  return http.post(`${UNIT_URL}/import`, data)
}

/** 往来单位子表 */
export function listUnitContacts(unitId: string) {
  return http.get<T.UnitContactResp[]>(`${UNIT_URL}/${unitId}/contacts`)
}
export function addUnitContact(unitId: string, data: T.UnitContactReq) {
  return http.post(`${UNIT_URL}/${unitId}/contacts`, data)
}
export function updateUnitContact(unitId: string, id: string, data: T.UnitContactReq) {
  return http.put(`${UNIT_URL}/${unitId}/contacts/${id}`, data)
}
export function deleteUnitContact(unitId: string, id: string) {
  return http.del(`${UNIT_URL}/${unitId}/contacts/${id}`)
}

export function listUnitInvoices(unitId: string) {
  return http.get<T.UnitInvoiceResp[]>(`${UNIT_URL}/${unitId}/invoices`)
}
export function addUnitInvoice(unitId: string, data: T.UnitInvoiceReq) {
  return http.post(`${UNIT_URL}/${unitId}/invoices`, data)
}
export function updateUnitInvoice(unitId: string, id: string, data: T.UnitInvoiceReq) {
  return http.put(`${UNIT_URL}/${unitId}/invoices/${id}`, data)
}
export function deleteUnitInvoice(unitId: string, id: string) {
  return http.del(`${UNIT_URL}/${unitId}/invoices/${id}`)
}

export function listUnitAddresses(unitId: string) {
  return http.get<T.UnitAddressResp[]>(`${UNIT_URL}/${unitId}/addresses`)
}
export function addUnitAddress(unitId: string, data: T.UnitAddressReq) {
  return http.post(`${UNIT_URL}/${unitId}/addresses`, data)
}
export function updateUnitAddress(unitId: string, id: string, data: T.UnitAddressReq) {
  return http.put(`${UNIT_URL}/${unitId}/addresses/${id}`, data)
}
export function deleteUnitAddress(unitId: string, id: string) {
  return http.del(`${UNIT_URL}/${unitId}/addresses/${id}`)
}

export function listUnitPays(unitId: string) {
  return http.get<T.UnitPayResp[]>(`${UNIT_URL}/${unitId}/pays`)
}
export function addUnitPay(unitId: string, data: T.UnitPayReq) {
  return http.post(`${UNIT_URL}/${unitId}/pays`, data)
}
export function updateUnitPay(unitId: string, id: string, data: T.UnitPayReq) {
  return http.put(`${UNIT_URL}/${unitId}/pays/${id}`, data)
}
export function deleteUnitPay(unitId: string, id: string) {
  return http.del(`${UNIT_URL}/${unitId}/pays/${id}`)
}

export function listGoods(query: T.GoodsPageQuery) {
  return http.get<PageRes<T.GoodsResp[]>>(GOODS_URL, query)
}
export function getGoods(id: string) {
  return http.get<T.GoodsResp>(`${GOODS_URL}/${id}`)
}
export function addGoods(data: any) {
  return http.post(GOODS_URL, data)
}
export function updateGoods(data: any, id: string) {
  return http.put(`${GOODS_URL}/${id}`, data)
}
export function deleteGoods(id: string) {
  return http.del(GOODS_URL, { ids: [id] })
}
export function exportGoods(query: T.GoodsPageQuery) {
  return http.download(`${GOODS_URL}/export`, query)
}
export function downloadGoodsImportTemplate() {
  return http.download(`${GOODS_URL}/import/template`)
}
export function parseImportGoods(data: FormData) {
  return http.post(`${GOODS_URL}/import/parse`, data)
}
export function importGoods(data: any) {
  return http.post(`${GOODS_URL}/import`, data)
}

export function listGoodsCategory(query?: T.GoodsCategoryQuery) {
  return http.get<T.GoodsCategoryResp[]>(`${GOODS_CATEGORY_URL}/tree`, query)
}
export function getGoodsCategory(id: string) {
  return http.get<T.GoodsCategoryResp>(`${GOODS_CATEGORY_URL}/${id}`)
}
export function addGoodsCategory(data: any) {
  return http.post(GOODS_CATEGORY_URL, data)
}
export function updateGoodsCategory(data: any, id: string) {
  return http.put(`${GOODS_CATEGORY_URL}/${id}`, data)
}
export function deleteGoodsCategory(id: string) {
  return http.del(GOODS_CATEGORY_URL, { ids: [id] })
}
export function exportGoodsCategory(query?: T.GoodsCategoryQuery) {
  return http.download(`${GOODS_CATEGORY_URL}/export`, query)
}
export function downloadGoodsCategoryImportTemplate() {
  return http.download(`${GOODS_CATEGORY_URL}/import/template`)
}
export function parseImportGoodsCategory(data: FormData) {
  return http.post(`${GOODS_CATEGORY_URL}/import/parse`, data)
}
export function importGoodsCategory(data: any) {
  return http.post(`${GOODS_CATEGORY_URL}/import`, data)
}

export function listWarehouse(query: T.WarehousePageQuery) {
  return http.get<PageRes<T.WarehouseResp[]>>(WAREHOUSE_URL, query)
}
export function getWarehouse(id: string) {
  return http.get<T.WarehouseResp>(`${WAREHOUSE_URL}/${id}`)
}
export function addWarehouse(data: any) {
  return http.post(WAREHOUSE_URL, data)
}
export function updateWarehouse(data: any, id: string) {
  return http.put(`${WAREHOUSE_URL}/${id}`, data)
}
export function deleteWarehouse(id: string) {
  return http.del(WAREHOUSE_URL, { ids: [id] })
}
export function exportWarehouse(query: T.WarehousePageQuery) {
  return http.download(`${WAREHOUSE_URL}/export`, query)
}
export function downloadWarehouseImportTemplate() {
  return http.download(`${WAREHOUSE_URL}/import/template`)
}
export function parseImportWarehouse(data: FormData) {
  return http.post(`${WAREHOUSE_URL}/import/parse`, data)
}
export function importWarehouse(data: any) {
  return http.post(`${WAREHOUSE_URL}/import`, data)
}

const STOCK_IN_URL = '/psi/stock-in'
const STOCK_OUT_URL = '/psi/stock-out'

export function listStockIn(query: T.StockInPageQuery) {
  return http.get<PageRes<T.StockInResp[]>>(STOCK_IN_URL, query)
}
export function getStockIn(id: string) {
  return http.get<T.StockInResp>(`${STOCK_IN_URL}/${id}`)
}
export function addStockIn(data: any) {
  return http.post<string>(STOCK_IN_URL, data)
}
export function updateStockIn(data: any, id: string) {
  return http.put(`${STOCK_IN_URL}/${id}`, data)
}
export function deleteStockIn(id: string) {
  return http.del(`${STOCK_IN_URL}/${id}`)
}
export function confirmStockIn(id: string, idempotentKey: string) {
  return http.post<string>(`${STOCK_IN_URL}/${id}/confirm`, { idempotentKey })
}
export function exportStockIn(query: T.StockInPageQuery) {
  return http.download(`${STOCK_IN_URL}/export`, query)
}
export function downloadStockInImportTemplate() {
  return http.download(`${STOCK_IN_URL}/import/template`)
}
export function parseImportStockIn(data: FormData) {
  return http.post<{ totalRows?: number, validRows?: number, items?: any[] }>(`${STOCK_IN_URL}/import/parse`, data)
}

export function listStockOut(query: T.StockOutPageQuery) {
  return http.get<PageRes<T.StockOutResp[]>>(STOCK_OUT_URL, query)
}
export function getStockOut(id: string) {
  return http.get<T.StockOutResp>(`${STOCK_OUT_URL}/${id}`)
}
export function addStockOut(data: any) {
  return http.post<string>(STOCK_OUT_URL, data)
}
export function updateStockOut(data: any, id: string) {
  return http.put(`${STOCK_OUT_URL}/${id}`, data)
}
export function deleteStockOut(id: string) {
  return http.del(`${STOCK_OUT_URL}/${id}`)
}
export function confirmStockOut(id: string, idempotentKey: string) {
  return http.post<string>(`${STOCK_OUT_URL}/${id}/confirm`, { idempotentKey })
}
export function exportStockOut(query: T.StockOutPageQuery) {
  return http.download(`${STOCK_OUT_URL}/export`, query)
}
export function downloadStockOutImportTemplate() {
  return http.download(`${STOCK_OUT_URL}/import/template`)
}
export function parseImportStockOut(data: FormData) {
  return http.post<{ totalRows?: number, validRows?: number, items?: any[] }>(`${STOCK_OUT_URL}/import/parse`, data)
}

export function listWarehouseOptions() {
  return http.get<T.LabelValue[]>('/psi/options/warehouses')
}
/** warehouseId：该仓有库存商品（含 stockQty）；stockAcrossWarehouses：跨可见仓有库存商品 */
export function listGoodsOptions(warehouseId?: string, opts?: { stockAcrossWarehouses?: boolean }) {
  return http.get<T.LabelValue[]>('/psi/options/goods', {
    warehouseId,
    stockAcrossWarehouses: opts?.stockAcrossWarehouses,
  })
}
export function listUnitOptions(unitType?: string) {
  return http.get<T.LabelValue[]>('/psi/options/units', { unitType })
}
export function listAccountOptions() {
  return http.get<T.LabelValue[]>('/psi/options/accounts')
}

const STOCK_RETURN_URL = '/psi/stock-return'
const STOCK_TRANSFER_URL = '/psi/stock-transfer'

export function listStockReturn(query: T.StockReturnPageQuery) {
  return http.get<PageRes<T.StockReturnResp[]>>(STOCK_RETURN_URL, query)
}
export function exportStockReturn(query: T.StockReturnPageQuery) {
  return http.download(`${STOCK_RETURN_URL}/export`, query)
}
export function getStockReturn(id: string) {
  return http.get<T.StockReturnResp>(`${STOCK_RETURN_URL}/${id}`)
}
export function addStockReturn(data: any) {
  return http.post<string>(STOCK_RETURN_URL, data)
}
export function updateStockReturn(data: any, id: string) {
  return http.put(`${STOCK_RETURN_URL}/${id}`, data)
}
export function deleteStockReturn(id: string) {
  return http.del(`${STOCK_RETURN_URL}/${id}`)
}
export function confirmStockReturn(id: string, idempotentKey: string) {
  return http.post<string>(`${STOCK_RETURN_URL}/${id}/confirm`, { idempotentKey })
}

export function listStockTransfer(query: T.StockTransferPageQuery) {
  return http.get<PageRes<T.StockTransferResp[]>>(STOCK_TRANSFER_URL, query)
}
export function exportStockTransfer(query: T.StockTransferPageQuery) {
  return http.download(`${STOCK_TRANSFER_URL}/export`, query)
}
export function getStockTransfer(id: string) {
  return http.get<T.StockTransferResp>(`${STOCK_TRANSFER_URL}/${id}`)
}
export function addStockTransfer(data: any) {
  return http.post<string>(STOCK_TRANSFER_URL, data)
}
export function updateStockTransfer(data: any, id: string) {
  return http.put(`${STOCK_TRANSFER_URL}/${id}`, data)
}
export function deleteStockTransfer(id: string) {
  return http.del(`${STOCK_TRANSFER_URL}/${id}`)
}
export function confirmStockTransfer(id: string, idempotentKey: string) {
  return http.post<string>(`${STOCK_TRANSFER_URL}/${id}/confirm`, { idempotentKey })
}

const ACCOUNT_URL = '/psi/account'
const INIT_STOCK_URL = '/psi/init/stock'
const INIT_URL = '/psi/init'

export function listAccount(query: T.CompanyAccountPageQuery) {
  return http.get<PageRes<T.CompanyAccountResp[]>>(ACCOUNT_URL, query)
}
export function getAccount(id: string) {
  return http.get<T.CompanyAccountResp>(`${ACCOUNT_URL}/${id}`)
}
export function addAccount(data: any) {
  return http.post(ACCOUNT_URL, data)
}
export function updateAccount(data: any, id: string) {
  return http.put(`${ACCOUNT_URL}/${id}`, data)
}
export function deleteAccount(id: string) {
  return http.del(ACCOUNT_URL, { ids: [id] })
}
export function exportAccount(query: T.CompanyAccountPageQuery) {
  return http.download(`${ACCOUNT_URL}/export`, query)
}
export function downloadAccountImportTemplate() {
  return http.download(`${ACCOUNT_URL}/import/template`)
}
export function parseImportAccount(data: FormData) {
  return http.post(`${ACCOUNT_URL}/import/parse`, data)
}
export function importAccount(data: any) {
  return http.post(`${ACCOUNT_URL}/import`, data)
}

export function listInitStock(query: T.InitStockPageQuery) {
  return http.get<PageRes<T.InitStockResp[]>>(INIT_STOCK_URL, query)
}
export function getInitStock(id: string) {
  return http.get<T.InitStockResp>(`${INIT_STOCK_URL}/${id}`)
}
export function addInitStock(data: any) {
  return http.post<string>(INIT_STOCK_URL, data)
}
export function updateInitStock(data: any, id: string) {
  return http.put(`${INIT_STOCK_URL}/${id}`, data)
}
export function deleteInitStock(id: string) {
  return http.del(`${INIT_STOCK_URL}/${id}`)
}
export function confirmInitStock(id: string, idempotentKey: string) {
  return http.post<string>(`${INIT_STOCK_URL}/${id}/confirm`, { idempotentKey })
}
export function downloadInitStockImportTemplate() {
  return http.download(`${INIT_STOCK_URL}/import/template`)
}
export function parseImportInitStock(data: FormData) {
  return http.post(`${INIT_STOCK_URL}/import/parse`, data)
}
export function importInitStock(data: any) {
  return http.post(`${INIT_STOCK_URL}/import`, data)
}

/** 统一期初（AR/AP/PREPAY/STOCK），type=AR|AP|PREPAY|STOCK */
export function listInitDoc(query: T.InitDocPageQuery) {
  return http.get<PageRes<T.InitDocResp[]>>(INIT_URL, query)
}
export function getInitDoc(type: string, id: string) {
  return http.get<T.InitDocResp>(`${INIT_URL}/${type}/${id}`)
}
export function addInitDoc(data: any) {
  return http.post<string>(INIT_URL, data)
}
export function updateInitDoc(data: any, id: string) {
  return http.put(`${INIT_URL}/${id}`, data)
}
export function deleteInitDoc(type: string, id: string) {
  return http.del(`${INIT_URL}/${type}/${id}`)
}
export function confirmInitDoc(type: string, id: string, idempotentKey: string) {
  return http.post<string>(`${INIT_URL}/${type}/${id}/confirm`, { idempotentKey })
}

const STOCK_CHECK_URL = '/psi/stock-check'
const STOCK_ADJUST_URL = '/psi/stock-adjust'

export function listStockCheck(query: T.StockCheckPageQuery) {
  return http.get<PageRes<T.StockCheckResp[]>>(STOCK_CHECK_URL, query)
}
export function exportStockCheck(query: T.StockCheckPageQuery) {
  return http.download(`${STOCK_CHECK_URL}/export`, query)
}
export function getStockCheck(id: string) {
  return http.get<T.StockCheckResp>(`${STOCK_CHECK_URL}/${id}`)
}
/** 盘点录入：按仓库+商品带出账面数量与参考未税单价 */
export function getStockCheckBookInfo(warehouseId: string, goodsId: string) {
  return http.get<{ bookQty: number, untaxPrice: number }>(`${STOCK_CHECK_URL}/book-info`, { warehouseId, goodsId })
}
export function addStockCheck(data: any) {
  return http.post<string>(STOCK_CHECK_URL, data)
}
export function updateStockCheck(data: any, id: string) {
  return http.put(`${STOCK_CHECK_URL}/${id}`, data)
}
export function deleteStockCheck(id: string) {
  return http.del(`${STOCK_CHECK_URL}/${id}`)
}
export function confirmStockCheck(id: string, idempotentKey: string, snapshotVersion?: string) {
  return http.post<string>(`${STOCK_CHECK_URL}/${id}/confirm`, { idempotentKey, snapshotVersion })
}
export function downloadStockCheckImportTemplate() {
  return http.download(`${STOCK_CHECK_URL}/import/template`)
}
export function parseImportStockCheck(data: FormData, warehouseId?: string) {
  const qs = warehouseId != null && warehouseId !== '' ? `?warehouseId=${warehouseId}` : ''
  return http.post<{ totalRows?: number, validRows?: number, items?: any[] }>(`${STOCK_CHECK_URL}/import/parse${qs}`, data)
}

export function listStockAdjust(query: T.StockAdjustPageQuery) {
  return http.get<PageRes<T.StockAdjustResp[]>>(STOCK_ADJUST_URL, query)
}
export function exportStockAdjust(query: T.StockAdjustPageQuery) {
  return http.download(`${STOCK_ADJUST_URL}/export`, query)
}
export function getStockAdjust(id: string) {
  return http.get<T.StockAdjustResp>(`${STOCK_ADJUST_URL}/${id}`)
}
export function addStockAdjust(data: any) {
  return http.post<string>(STOCK_ADJUST_URL, data)
}
export function updateStockAdjust(data: any, id: string) {
  return http.put(`${STOCK_ADJUST_URL}/${id}`, data)
}
export function deleteStockAdjust(id: string) {
  return http.del(`${STOCK_ADJUST_URL}/${id}`)
}
export function confirmStockAdjust(id: string, idempotentKey: string) {
  return http.post<string>(`${STOCK_ADJUST_URL}/${id}/confirm`, { idempotentKey })
}

const RECEIPT_URL = '/psi/receipt'
const PAYMENT_URL = '/psi/payment'

export function listReceipt(query: T.ReceiptPageQuery) {
  return http.get<PageRes<T.ReceiptResp[]>>(RECEIPT_URL, query)
}
export function getReceipt(id: string) {
  return http.get<T.ReceiptResp>(`${RECEIPT_URL}/${id}`)
}
export function addReceipt(data: any) {
  return http.post<string>(RECEIPT_URL, data)
}
export function updateReceipt(data: any, id: string) {
  return http.put(`${RECEIPT_URL}/${id}`, data)
}
export function deleteReceipt(id: string) {
  return http.del(`${RECEIPT_URL}/${id}`)
}
export function confirmReceipt(id: string, idempotentKey: string, extra?: {
  writeOffItems?: Array<{ openId: string, amount: number }>
  writeOffReason?: string
}) {
  return http.post<string>(`${RECEIPT_URL}/${id}/confirm`, { idempotentKey, ...extra })
}
export function writeOffReceipt(id: string, data: any) {
  return http.post<string>(`${RECEIPT_URL}/${id}/write-off`, data)
}
export function reverseReceipt(id: string, data: any) {
  return http.post<string>(`${RECEIPT_URL}/${id}/reverse`, data)
}
export function listReceiptWriteOffs(id: string) {
  return http.get<T.WriteOffSummaryResp[]>(`${RECEIPT_URL}/${id}/write-offs`)
}
export function exportReceipt(query: T.ReceiptPageQuery) {
  return http.download(`${RECEIPT_URL}/export`, query)
}
export function voidReceipt(id: string, data: T.CashCompensationReq) {
  return http.post(`${RECEIPT_URL}/${id}/void`, data)
}
export function refundReceipt(id: string, data: T.CashRefundReq) {
  return http.post<string>(`${RECEIPT_URL}/${id}/refund`, data)
}
export function reverseReceiptRefund(id: string, data: T.CashCompensationReq) {
  return http.post(`${RECEIPT_URL}/${id}/refund/reverse`, data)
}

export function listPayment(query: T.PaymentPageQuery) {
  return http.get<PageRes<T.PaymentResp[]>>(PAYMENT_URL, query)
}
export function getPayment(id: string) {
  return http.get<T.PaymentResp>(`${PAYMENT_URL}/${id}`)
}
export function addPayment(data: any) {
  return http.post<string>(PAYMENT_URL, data)
}
export function updatePayment(data: any, id: string) {
  return http.put(`${PAYMENT_URL}/${id}`, data)
}
export function deletePayment(id: string) {
  return http.del(`${PAYMENT_URL}/${id}`)
}
export function confirmPayment(id: string, idempotentKey: string, extra?: {
  writeOffItems?: Array<{ openId: string, amount: number }>
  writeOffReason?: string
}) {
  return http.post<string>(`${PAYMENT_URL}/${id}/confirm`, { idempotentKey, ...extra })
}
export function writeOffPayment(id: string, data: any) {
  return http.post<string>(`${PAYMENT_URL}/${id}/write-off`, data)
}
export function reversePayment(id: string, data: any) {
  return http.post<string>(`${PAYMENT_URL}/${id}/reverse`, data)
}
export function listPaymentWriteOffs(id: string) {
  return http.get<T.WriteOffSummaryResp[]>(`${PAYMENT_URL}/${id}/write-offs`)
}
export function exportPayment(query: T.PaymentPageQuery) {
  return http.download(`${PAYMENT_URL}/export`, query)
}
export function voidPayment(id: string, data: T.CashCompensationReq) {
  return http.post(`${PAYMENT_URL}/${id}/void`, data)
}
export function refundPayment(id: string, data: T.CashRefundReq) {
  return http.post<string>(`${PAYMENT_URL}/${id}/refund`, data)
}
export function reversePaymentRefund(id: string, data: T.CashCompensationReq) {
  return http.post(`${PAYMENT_URL}/${id}/refund/reverse`, data)
}

const INVOICE_OUT_URL = '/psi/invoice-out'
const INVOICE_IN_URL = '/psi/invoice-in'

export function listInvoiceOut(query: T.InvoicePageQuery) {
  return http.get<PageRes<T.InvoiceResp[]>>(INVOICE_OUT_URL, query)
}
export function getInvoiceOut(id: string) {
  return http.get<T.InvoiceResp>(`${INVOICE_OUT_URL}/${id}`)
}
export function addInvoiceOut(data: any) {
  return http.post<string>(INVOICE_OUT_URL, data)
}
export function updateInvoiceOut(data: any, id: string) {
  return http.put(`${INVOICE_OUT_URL}/${id}`, data)
}
export function deleteInvoiceOut(id: string) {
  return http.del(`${INVOICE_OUT_URL}/${id}`)
}
export function confirmInvoiceOut(id: string, idempotentKey: string) {
  return http.post<string>(`${INVOICE_OUT_URL}/${id}/confirm`, { idempotentKey })
}
export function redInvoiceOut(id: string, idempotentKey: string) {
  return http.post<string>(`${INVOICE_OUT_URL}/${id}/red`, { idempotentKey })
}
export function exportInvoiceOut(query: T.InvoicePageQuery) {
  return http.download(`${INVOICE_OUT_URL}/export`, query)
}
export function parseImportInvoiceOut(data: FormData) {
  return http.post<any>(`${INVOICE_OUT_URL}/import/parse`, data)
}

export function listInvoiceIn(query: T.InvoicePageQuery) {
  return http.get<PageRes<T.InvoiceResp[]>>(INVOICE_IN_URL, query)
}
export function getInvoiceIn(id: string) {
  return http.get<T.InvoiceResp>(`${INVOICE_IN_URL}/${id}`)
}
export function addInvoiceIn(data: any) {
  return http.post<string>(INVOICE_IN_URL, data)
}
export function updateInvoiceIn(data: any, id: string) {
  return http.put(`${INVOICE_IN_URL}/${id}`, data)
}
export function deleteInvoiceIn(id: string) {
  return http.del(`${INVOICE_IN_URL}/${id}`)
}
export function confirmInvoiceIn(id: string, idempotentKey: string) {
  return http.post<string>(`${INVOICE_IN_URL}/${id}/confirm`, { idempotentKey })
}
export function redInvoiceIn(id: string, idempotentKey: string) {
  return http.post<string>(`${INVOICE_IN_URL}/${id}/red`, { idempotentKey })
}
export function exportInvoiceIn(query: T.InvoicePageQuery) {
  return http.download(`${INVOICE_IN_URL}/export`, query)
}
export function parseImportInvoiceIn(data: FormData) {
  return http.post<any>(`${INVOICE_IN_URL}/import/parse`, data)
}

const BANK_URL = '/psi/bank'
const CONTRACT_SALE_URL = '/psi/contract-sale'
const CONTRACT_PURCHASE_URL = '/psi/contract-purchase'

export function listBank(query: T.BankStatementPageQuery) {
  return http.get<PageRes<T.BankStatementResp[]>>(BANK_URL, query)
}
export function unmatchBank(id: string, data: T.CashCompensationReq) {
  return http.post(`${BANK_URL}/${id}/unmatch`, data)
}
export function getBank(id: string) {
  return http.get<T.BankStatementResp>(`${BANK_URL}/${id}`)
}
export function addBank(data: any) {
  return http.post<number>(`${BANK_URL}/import`, data)
}
export function downloadBankImportTemplate() {
  return http.download(`${BANK_URL}/import/template`)
}
export function parseImportBank(data: FormData) {
  return http.post<{ totalRows?: number, validRows?: number, items?: any[] }>(`${BANK_URL}/import/parse`, data)
}
export function matchBank(data: any) {
  return http.post<string>(`${BANK_URL}/match`, data)
}
export function listBankCandidates(id: string, matchBizType?: string) {
  return http.get<T.LabelValue[]>(`${BANK_URL}/${id}/candidates`, { matchBizType })
}

export function listContractSale(query: T.ContractPageQuery) {
  return http.get<PageRes<T.ContractResp[]>>(CONTRACT_SALE_URL, query)
}
export function exportContractSale(query: T.ContractPageQuery) {
  return http.download(`${CONTRACT_SALE_URL}/export`, query)
}
export function getContractSale(id: string) {
  return http.get<T.ContractResp>(`${CONTRACT_SALE_URL}/${id}`)
}
export function addContractSale(data: any) {
  return http.post<string>(CONTRACT_SALE_URL, data)
}
export function updateContractSale(data: any, id: string) {
  return http.put(`${CONTRACT_SALE_URL}/${id}`, data)
}
export function deleteContractSale(id: string) {
  return http.del(`${CONTRACT_SALE_URL}/${id}`)
}
export function confirmContractSale(id: string, idempotentKey: string) {
  return http.post<string>(`${CONTRACT_SALE_URL}/${id}/confirm`, { idempotentKey })
}
export function progressContractSale(id: string) {
  return http.get<T.ContractResp>(`${CONTRACT_SALE_URL}/${id}/progress`)
}

export function listContractPurchase(query: T.ContractPageQuery) {
  return http.get<PageRes<T.ContractResp[]>>(CONTRACT_PURCHASE_URL, query)
}
export function exportContractPurchase(query: T.ContractPageQuery) {
  return http.download(`${CONTRACT_PURCHASE_URL}/export`, query)
}
export function getContractPurchase(id: string) {
  return http.get<T.ContractResp>(`${CONTRACT_PURCHASE_URL}/${id}`)
}
export function addContractPurchase(data: any) {
  return http.post<string>(CONTRACT_PURCHASE_URL, data)
}
export function updateContractPurchase(data: any, id: string) {
  return http.put(`${CONTRACT_PURCHASE_URL}/${id}`, data)
}
export function deleteContractPurchase(id: string) {
  return http.del(`${CONTRACT_PURCHASE_URL}/${id}`)
}
export function confirmContractPurchase(id: string, idempotentKey: string) {
  return http.post<string>(`${CONTRACT_PURCHASE_URL}/${id}/confirm`, { idempotentKey })
}
export function progressContractPurchase(id: string) {
  return http.get<T.ContractResp>(`${CONTRACT_PURCHASE_URL}/${id}/progress`)
}

const RECEIVABLE_URL = '/psi/receivable'
const PAYABLE_URL = '/psi/payable'

export function listReceivable(query: T.OpenItemPageQuery) {
  return http.get<PageRes<T.OpenItemResp[]>>(RECEIVABLE_URL, query)
}
export function exportReceivable(query: T.OpenItemPageQuery) {
  return http.download(`${RECEIVABLE_URL}/export`, query)
}
export function listPayable(query: T.OpenItemPageQuery) {
  return http.get<PageRes<T.OpenItemResp[]>>(PAYABLE_URL, query)
}
export function exportPayable(query: T.OpenItemPageQuery) {
  return http.download(`${PAYABLE_URL}/export`, query)
}
export function listOpenReceivables(unitId?: string) {
  return http.get<T.OpenItemOptionResp[]>('/psi/options/open-receivables', { unitId })
}
export function listOpenPayables(unitId?: string) {
  return http.get<T.OpenItemOptionResp[]>('/psi/options/open-payables', { unitId })
}

const STOCK_FLOW_URL = '/psi/stock-flow'
const STOCK_QUERY_URL = '/psi/stock-query'
const TODO_URL = '/psi/todo'

export function listStockFlow(query: T.StockFlowPageQuery) {
  return http.get<PageRes<T.StockFlowResp[]>>(STOCK_FLOW_URL, query)
}
export function exportStockFlow(query: T.StockFlowPageQuery) {
  return http.download(`${STOCK_FLOW_URL}/export`, query)
}

export function listStockQuery(query: T.StockQueryPageQuery) {
  return http.get<PageRes<T.StockQueryResp[]>>(STOCK_QUERY_URL, query)
}
export function exportStockQuery(query: T.StockQueryPageQuery) {
  return http.download(`${STOCK_QUERY_URL}/export`, query)
}

export function listPsiTodo(query: T.PsiTodoPageQuery) {
  return http.get<PageRes<T.PsiTodoResp[]>>(TODO_URL, query)
}
export function donePsiTodo(id: string) {
  return http.post(`${TODO_URL}/${id}/done`)
}
export function reopenPsiTodo(id: string) {
  return http.post(`${TODO_URL}/${id}/reopen`)
}

export function getPsiWorkbenchSummary() {
  return http.get<T.PsiWorkbenchSummary>('/psi/workbench/summary')
}

const PRINT_TEMPLATE_URL = '/psi/print-template'

export function listPrintTemplate(query: T.PrintTemplatePageQuery) {
  return http.get<PageRes<T.PrintTemplateResp[]>>(PRINT_TEMPLATE_URL, query)
}
export function getPrintTemplate(id: string) {
  return http.get<T.PrintTemplateResp>(`${PRINT_TEMPLATE_URL}/${id}`)
}
export function addPrintTemplate(data: T.PrintTemplateReq) {
  return http.post(PRINT_TEMPLATE_URL, data)
}
export function updatePrintTemplate(data: T.PrintTemplateReq, id: string) {
  return http.put(`${PRINT_TEMPLATE_URL}/${id}`, data)
}
export function deletePrintTemplate(id: string) {
  return http.del(`${PRINT_TEMPLATE_URL}/${id}`)
}
export function renderPrintTemplate(params: { bizType: string, bizId: string, templateId?: string }) {
  return http.get<T.PrintRenderResp>(`${PRINT_TEMPLATE_URL}/render`, params)
}
export function downloadPrintDocx(params: { bizType: string, bizId: string, templateId?: string }) {
  return http.download(`${PRINT_TEMPLATE_URL}/render-docx`, params)
}
