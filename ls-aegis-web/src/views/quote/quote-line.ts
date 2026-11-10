export type QuoteLine = {
  uid: string
  lineType: 'MATERIAL' | 'SERVICE'
  goodsId?: string
  itemName?: string
  brandModel?: string
  description?: string
  unit?: string
  quantity?: number
  price?: number
  amount?: number
  sortNo?: number
}
