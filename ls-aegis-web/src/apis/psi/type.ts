/** 往来单位 */
export interface UnitResp {
  id: string
  unitCode: string
  unitName: string
  unitType: string
  unitNature: string
  creditCode?: string
  address?: string
  status: 1 | 2
  remark?: string
  createUserString?: string
  createTime?: string
  updateUserString?: string
  updateTime?: string
}

export interface UnitQuery {
  keyword?: string
  unitType?: string
  status?: 1 | 2
  sort?: Array<string>
}

export interface UnitPageQuery extends UnitQuery, PageQuery {}

/** 往来单位子表 */
export interface UnitContactResp {
  id: string
  unitId?: string
  contactName: string
  mobile?: string
  phone?: string
  email?: string
  position?: string
  isDefault?: number
  remark?: string
}
export interface UnitContactReq {
  contactName: string
  mobile?: string
  phone?: string
  email?: string
  position?: string
  isDefault?: number
  remark?: string
}

export interface UnitInvoiceResp {
  id: string
  unitId?: string
  title: string
  taxNo?: string
  bankName?: string
  accountNo?: string
  address?: string
  phone?: string
  isDefault?: number
  remark?: string
}
export interface UnitInvoiceReq {
  title: string
  taxNo?: string
  bankName?: string
  accountNo?: string
  address?: string
  phone?: string
  isDefault?: number
  remark?: string
}

export interface UnitAddressResp {
  id: string
  unitId?: string
  contactName?: string
  mobile?: string
  region?: string
  address: string
  isDefault?: number
  remark?: string
}
export interface UnitAddressReq {
  contactName?: string
  mobile?: string
  region?: string
  address: string
  isDefault?: number
  remark?: string
}

export interface UnitPayResp {
  id: string
  unitId?: string
  accountName: string
  bankName?: string
  accountNo: string
  isDefault?: number
  remark?: string
}
export interface UnitPayReq {
  accountName: string
  bankName?: string
  accountNo: string
  isDefault?: number
  remark?: string
}

/** 商品 */
export interface GoodsResp {
  id: string
  goodsCode: string
  goodsName: string
  barcode?: string
  spec?: string
  unit: string
  defaultTaxRate: number
  categoryId?: string
  categoryName?: string
  brandId?: string
  purchasePrice?: number
  salePrice?: number
  stockLower?: number
  stockUpper?: number
  status: 1 | 2
  remark?: string
  createUserString?: string
  createTime?: string
}

export interface GoodsQuery {
  keyword?: string
  categoryId?: string
  status?: 1 | 2
  sort?: Array<string>
}

export interface GoodsPageQuery extends GoodsQuery, PageQuery {}

/** 商品分类 */
export interface GoodsCategoryResp {
  id: string
  parentId?: string
  code?: string
  name: string
  /** 1=一级 2=二级 */
  level?: number
  levelName?: string
  sort?: number
  status: 1 | 2
  children?: GoodsCategoryResp[]
  createTime?: string
}

export interface GoodsCategoryQuery {
  name?: string
  status?: 1 | 2
}

/** 仓库 */
export interface WarehouseResp {
  id: string
  warehouseCode: string
  warehouseName: string
  managerUserId?: string
  manager?: string
  address?: string
  isDefault: boolean
  status: 1 | 2
  remark?: string
  createUserString?: string
  createTime?: string
}

export interface WarehouseQuery {
  keyword?: string
  status?: 1 | 2
  sort?: Array<string>
}

export interface WarehousePageQuery extends WarehouseQuery, PageQuery {}

/** 出入库明细行 */
export interface StockLine {
  id?: string
  goodsId: string
  quantity: number
  taxInclusivePrice: number
  taxRate: number
  untaxPrice?: number
  untaxAmount?: number
  taxAmount?: number
  taxInclusiveAmount?: number
  costAmount?: number
}

export interface StockInResp {
  id: string
  docNo: string
  docStatus: string
  inType: string
  inDate: string
  warehouseId: string
  unitId?: string
  taxInclusiveAmount: number
  untaxAmount: number
  taxAmount: number
  payStatus?: string
  remark?: string
  items?: StockLine[]
  createTime?: string
}

export interface StockInQuery {
  docNo?: string
  docStatus?: string
  inType?: string
  warehouseId?: string
  sort?: Array<string>
}
export interface StockInPageQuery extends StockInQuery, PageQuery {}

export interface StockOutResp {
  id: string
  docNo: string
  docStatus: string
  outType: string
  outDate: string
  warehouseId: string
  unitId?: string
  contractId?: string
  contractName?: string
  taxInclusiveAmount: number
  untaxAmount: number
  taxAmount: number
  costAmount?: number
  invoiceStatus?: string
  remark?: string
  items?: StockLine[]
  createTime?: string
}

export interface StockOutQuery {
  docNo?: string
  docStatus?: string
  outType?: string
  warehouseId?: string
  sort?: Array<string>
}
export interface StockOutPageQuery extends StockOutQuery, PageQuery {}

export interface LabelValue {
  label: string
  value: string
  /** 可选：库存数量（按仓/跨仓汇总时返回） */
  stockQty?: number
  /** 可选：库存加权未税单价 */
  untaxPrice?: number
}

export interface StockReturnResp {
  id: string
  docNo: string
  docStatus: string
  returnType: string
  returnDate: string
  warehouseId: string
  unitId?: string
  originStockOutId?: string
  originStockInId?: string
  taxInclusiveAmount: number
  costAmount?: number
  remark?: string
  items?: Array<{
    id?: string
    originItemId: string
    goodsId: string
    quantity: number
    taxInclusivePrice: number
    taxRate: number
  }>
}

export interface StockReturnQuery {
  unitId?: string
  docNo?: string
  docStatus?: string
  returnType?: string
  sort?: Array<string>
}
export interface StockReturnPageQuery extends StockReturnQuery, PageQuery {}

export interface StockTransferResp {
  id: string
  docNo: string
  docStatus: string
  transferDate: string
  fromWarehouseId: string
  fromWarehouseName?: string
  toWarehouseId: string
  toWarehouseName?: string
  remark?: string
  items?: Array<{ id?: string, goodsId: string, quantity: number, costAmount?: number }>
}

export interface StockTransferQuery {
  docNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface StockTransferPageQuery extends StockTransferQuery, PageQuery {}

export interface CompanyAccountResp {
  id: string
  accountName: string
  bankName: string
  accountNo: string
  status: 1 | 2
  remark?: string
  createTime?: string
}
export interface CompanyAccountQuery {
  accountName?: string
  accountNo?: string
  status?: 1 | 2
  sort?: Array<string>
}
export interface CompanyAccountPageQuery extends CompanyAccountQuery, PageQuery {}

export interface InitStockResp {
  id: string
  docNo: string
  docStatus: string
  initDate: string
  warehouseId: string
  warehouseName?: string
  remark?: string
  items?: Array<{ id?: string, goodsId: string, quantity: number, untaxPrice: number, untaxAmount?: number }>
}
export interface InitStockQuery {
  docNo?: string
  docStatus?: string
  warehouseId?: string
  sort?: Array<string>
}
export interface InitStockPageQuery extends InitStockQuery, PageQuery {}

export interface InitDocResp {
  id: string
  type?: string
  docNo: string
  docStatus: string
  initDate: string
  warehouseId?: string
  remark?: string
  items?: Array<{
    id?: string
    goodsId?: string
    qty?: number
    quantity?: number
    untaxPrice?: number
    unitId?: string
    taxInclusiveAmount?: number
  }>
}
export interface InitDocQuery {
  type?: string
  docNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface InitDocPageQuery extends InitDocQuery, PageQuery {}

export interface StockCheckResp {
  id: string
  docNo: string
  docStatus: string
  checkDate: string
  warehouseId: string
  warehouseName?: string
  snapshotVersion?: string
  remark?: string
  items?: Array<{ goodsId: string, bookQty: number, checkQty: number, untaxPrice?: number }>
}
export interface StockCheckQuery {
  docNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface StockCheckPageQuery extends StockCheckQuery, PageQuery {}

export interface StockAdjustResp {
  id: string
  docNo: string
  docStatus: string
  adjustDate: string
  warehouseId: string
  warehouseName?: string
  remark?: string
  items?: Array<{ goodsId: string, adjustType: string, quantity: number, untaxPrice?: number }>
}
export interface StockAdjustQuery {
  docNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface StockAdjustPageQuery extends StockAdjustQuery, PageQuery {}

export interface ReceiptResp {
  id: string
  docNo: string
  docStatus: string
  receiptDate: string
  unitId: string
  unitName?: string
  accountId?: string
  accountName?: string
  payMethod?: string
  amount: number
  unallocatedAmount?: number
  refundedAmount?: number
  refundSourceId?: string
  refundReturnId?: string
  voidReason?: string
  voidTime?: string
  remark?: string
}
export interface ReceiptQuery {
  docNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface ReceiptPageQuery extends ReceiptQuery, PageQuery {}

export interface PaymentResp {
  id: string
  docNo: string
  docStatus: string
  paymentDate: string
  unitId: string
  unitName?: string
  accountId?: string
  accountName?: string
  payMethod?: string
  amount: number
  unallocatedAmount?: number
  refundedAmount?: number
  refundSourceId?: string
  refundReturnId?: string
  voidReason?: string
  voidTime?: string
  remark?: string
}
export interface PaymentQuery {
  docNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface PaymentPageQuery extends PaymentQuery, PageQuery {}

export interface CashCompensationReq {
  reason: string
  idempotentKey: string
}
export interface CashRefundReq extends CashCompensationReq {
  stockReturnId: string
  amount: number
}

export interface InvoiceResp {
  id: string
  docNo?: string
  buyerTaxNo?: string
  sellerTaxNo?: string
  invoiceNo: string
  invoiceType: string
  invoiceDate: string
  unitId: string
  docStatus: string
  taxInclusiveAmount: number
  untaxAmount?: number
  taxAmount?: number
  linkedAmount?: number
  unallocatedAmount?: number
  fileId?: string
  remark?: string
  items?: Array<any>
  links?: Array<{ stockDocId: string, amount: number }>
}
export interface InvoiceQuery {
  invoiceNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface InvoicePageQuery extends InvoiceQuery, PageQuery {}

export interface BankStatementResp {
  id: string
  accountId: string
  accountName?: string
  stmtDate: string
  direction: string
  amount: number
  counterparty?: string
  summary?: string
  bankSeqNo?: string
  matchBizType?: string
  matchBizId?: string
  matchStatus: string
}
export interface BankStatementQuery {
  accountId?: string
  matchStatus?: string
  sort?: Array<string>
}
export interface BankStatementPageQuery extends BankStatementQuery, PageQuery {}

export interface ContractResp {
  id: string
  contractNo: string
  name: string
  docStatus: string
  signDate?: string
  unitId: string
  taxInclusiveAmount: number
  startDate?: string
  endDate?: string
  fileId?: string
  progress?: number
  remark?: string
}
export interface ContractQuery {
  contractNo?: string
  name?: string
  docStatus?: string
  unitId?: string
  sort?: Array<string>
}
export interface ContractPageQuery extends ContractQuery, PageQuery {}

export interface OpenItemResp {
  id: string
  unitId: string
  unitName?: string
  sourceType?: string
  sourceId?: string
  sourceNo?: string
  taxInclusiveAmount: number
  writtenOffAmount: number
  unwrittenAmount: number
  docStatus: string
  createTime?: string
}
export interface OpenItemQuery {
  unitId?: string
  sourceNo?: string
  docStatus?: string
  sort?: Array<string>
}
export interface OpenItemPageQuery extends OpenItemQuery, PageQuery {}

export interface OpenItemOptionResp {
  value: string
  label: string
  unwrittenAmount: number
  sourceNo?: string
  sourceType?: string
}

export interface WriteOffSummaryResp {
  writeOffId: string
  writeOffDate?: string
  amount: number
  reversed?: boolean
  reason?: string
}

/** 库存流水 */
export interface StockFlowResp {
  id: string
  warehouseId: string
  warehouseName?: string
  goodsId: string
  goodsName?: string
  flowType: string
  beforeQty?: number
  changeQty?: number
  afterQty?: number
  beforeAmount?: number
  changeAmount?: number
  afterAmount?: number
  sourceType?: string
  sourceId?: string
  sourceNo?: string
  confirmUser?: string
  confirmTime?: string
  createTime?: string
}
export interface StockFlowQuery {
  warehouseId?: string
  goodsId?: string
  flowType?: string
  sourceType?: string
  sourceNo?: string
  sort?: Array<string>
}
export interface StockFlowPageQuery extends StockFlowQuery, PageQuery {}

/** 库存查询（仓×商品实时剩余） */
export interface StockQueryResp {
  id: string
  warehouseId: string
  warehouseName?: string
  goodsId: string
  goodsCode?: string
  goodsName?: string
  spec?: string
  unit?: string
  remainQuantity: number
  remainAmount: number
}
export interface StockQueryQuery {
  warehouseId?: string
  goodsId?: string
  onlyPositive?: boolean
}
export interface StockQueryPageQuery extends StockQueryQuery, PageQuery {}

/** 业务待办 */
export interface PsiTodoResp {
  id: string
  todoType: string
  title: string
  bizType?: string
  bizId?: string
  bizNo?: string
  done: boolean
  createTime?: string
}
export interface PsiTodoQuery {
  todoType?: string
  bizType?: string
  done?: boolean
  title?: string
  sort?: Array<string>
}
export interface PsiTodoPageQuery extends PsiTodoQuery, PageQuery {}

/** 工作台库存预警 */
export interface PsiStockAlert {
  goodsId: string
  goodsCode?: string
  goodsName?: string
  stockQty: number
  stockLower?: number
  stockUpper?: number
  alertType: 'LOW' | 'HIGH' | string
}

/** 工作台摘要 */
export interface PsiWorkbenchSummary {
  openTodoCount: number
  openReceivableCount: number
  openReceivableAmount: number
  openPayableCount: number
  openPayableAmount: number
  recentTodos: PsiTodoResp[]
  stockAlerts?: PsiStockAlert[]
  /** 待确认回款笔数 */
  pendingReceiptCount?: number
  /** 待确认回款金额 */
  pendingReceiptAmount?: number
  /** 未匹配银行流水笔数 */
  unmatchedBankCount?: number
  /** 未匹配银行流水金额 */
  unmatchedBankAmount?: number
  /** 本月销售（已确认出库）笔数 */
  monthSaleCount?: number
  /** 本月销售含税合计 */
  monthSaleAmount?: number
  /** 本月销售数量合计 */
  monthSaleQty?: number
  /** 本月采购（已确认入库）笔数 */
  monthPurchaseCount?: number
  /** 本月采购含税合计 */
  monthPurchaseAmount?: number
  /** 本月采购数量合计 */
  monthPurchaseQty?: number
  /** 近 30 天入库笔数 */
  last30InCount?: number
  /** 近 30 天入库含税合计 */
  last30InAmount?: number
  /** 近 30 天入库数量合计 */
  last30InQty?: number
  /** 近 30 天出库笔数 */
  last30OutCount?: number
  /** 近 30 天出库含税合计 */
  last30OutAmount?: number
  /** 近 30 天出库数量合计 */
  last30OutQty?: number
}

/** 打印模板 */
export interface PrintTemplateResp {
  id: string
  templateName: string
  bizType: string
  fileId?: string
  contentHtml?: string
  fieldMapping?: string
  isDefault?: number
  status: 1 | 2
  remark?: string
  createTime?: string
}
export interface PrintTemplateReq {
  templateName: string
  bizType: string
  fileId?: number
  contentHtml?: string
  fieldMapping?: string
  isDefault?: number
  status: 1 | 2
  remark?: string
}
export interface PrintTemplateQuery {
  templateName?: string
  bizType?: string
  status?: 1 | 2
  sort?: Array<string>
}
export interface PrintTemplatePageQuery extends PrintTemplateQuery, PageQuery {}
export interface PrintRenderResp {
  title: string
  html: string
  fileId?: string
}
