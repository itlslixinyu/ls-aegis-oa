<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 960 ? 960 : '100%'"
    :ok-text="readonly ? '关闭' : '保存草稿'"
    :hide-cancel="readonly"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <a-form class="psi-biz-form" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="盘点日期" required>
            <a-date-picker v-model="form.checkDate" show-time :format="DATETIME_FORMAT" :value-format="DATETIME_FORMAT" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库" required>
            <a-select
              v-model="form.warehouseId"
              :options="warehouseOptions"
              :disabled="readonly"
              allow-search
              placeholder="请选择仓库"
              @change="onWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model="form.remark" :disabled="readonly" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="psi-biz-form__section">
        <div class="psi-biz-form__section-title">
          <h4>明细</h4>
          <div v-if="!readonly" style="display: flex; gap: 8px; align-items: center;">
            <a-link @click="downloadExcelTemplate">下载模板</a-link>
            <a-upload :custom-request="handleExcelUpload" :show-file-list="false" accept=".xlsx,.xls">
              <a-button type="outline" size="mini">Excel 导入实盘</a-button>
            </a-upload>
            <a-button type="outline" size="mini" @click="addLine">加行</a-button>
          </div>
        </div>
        <a-table class="psi-biz-form__table" :data="form.items" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <a-table-column title="商品" :width="240">
              <template #cell="{ record }">
                <a-select
                  v-model="record.goodsId"
                  :options="goodsOptions"
                  :disabled="readonly"
                  allow-search
                  placeholder="请选择商品"
                  @change="() => onGoodsChange(record)"
                />
              </template>
            </a-table-column>
            <a-table-column title="账面数量" :width="120">
              <template #cell="{ record }">
                <a-input-number v-model="record.bookQty" :min="0" :precision="4" disabled />
              </template>
            </a-table-column>
            <a-table-column title="实盘数量" :width="120">
              <template #cell="{ record }">
                <a-input-number v-model="record.checkQty" :min="0" :precision="4" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="未税单价" :width="130">
              <template #cell="{ record }">
                <a-input-number v-model="record.untaxPrice" :min="0" :precision="6" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column v-if="!readonly" title="操作" :width="72">
              <template #cell="{ rowIndex }">
                <a-link status="danger" @click="form.items.splice(rowIndex, 1)">删除</a-link>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { Message, type RequestOption } from '@arco-design/web-vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useWindowSize } from '@vueuse/core'
import {
  addStockCheck,
  downloadStockCheckImportTemplate,
  getGoods,
  getStockCheck,
  getStockCheckBookInfo,
  listGoodsOptions,
  listWarehouseOptions,
  parseImportStockCheck,
  updateStockCheck,
} from '@/apis/psi'
import { useDownload } from '@/hooks'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '盘点详情' : isUpdate.value ? '修改盘点草稿' : '新增盘点草稿'))

const warehouseOptions = ref<{ label: string, value: string }[]>([])
const goodsOptions = ref<{ label: string, value: string }[]>([])
/** 旧后端无 book-info 时的仓库存缓存 */
const legacyStockQtyMap = ref<Record<string, number>>({})

const form = reactive<any>({
  checkDate: undefined,
  warehouseId: undefined,
  remark: '',
  items: [] as any[],
})

const loadLegacyStockQtyMap = async () => {
  if (!form.warehouseId) {
    legacyStockQtyMap.value = {}
    return
  }
  try {
    const { data } = await listGoodsOptions(form.warehouseId)
    const map: Record<string, number> = {}
    for (const i of data || []) {
      const id = toBizId(i.value)
      if (id) map[id] = Number(i.stockQty ?? 0) || 0
    }
    legacyStockQtyMap.value = map
  } catch {
    legacyStockQtyMap.value = {}
  }
}

/** 按仓库+商品拉账面数量与参考未税单价并写回行 */
const fillLineFromStock = async (record: any) => {
  const warehouseId = form.warehouseId
  const goodsId = toBizId(record.goodsId)
  if (!warehouseId || !goodsId) {
    record.bookQty = 0
    record.untaxPrice = 0
    return
  }
  try {
    const { data } = await getStockCheckBookInfo(warehouseId, goodsId)
    record.bookQty = Number(data?.bookQty ?? 0)
    record.untaxPrice = Number(data?.untaxPrice ?? 0)
    return
  } catch {
    // 旧后端或接口暂不可用：库存下拉 + 商品采购价
  }
  record.bookQty = legacyStockQtyMap.value[goodsId] ?? 0
  try {
    const { data: goods } = await getGoods(goodsId)
    record.untaxPrice = Number(goods?.purchasePrice ?? 0) || 0
  } catch {
    record.untaxPrice = 0
  }
}

const refreshItemsFromStock = async () => {
  await loadLegacyStockQtyMap()
  await Promise.all(form.items.filter((i: any) => i.goodsId).map((i: any) => fillLineFromStock(i)))
}

const onGoodsChange = async (record: any) => {
  if (!form.warehouseId) {
    Message.warning('请先选择仓库，再选商品以带出账面数量与未税单价')
    record.bookQty = 0
    record.untaxPrice = 0
    return
  }
  if (!Object.keys(legacyStockQtyMap.value).length) {
    await loadLegacyStockQtyMap()
  }
  await fillLineFromStock(record)
}

const onWarehouseChange = async () => {
  await refreshItemsFromStock()
}

const loadOptions = async () => {
  const [w, g] = await Promise.all([listWarehouseOptions(), listGoodsOptions()])
  warehouseOptions.value = toLabelValueOpts(w.data || [])
  goodsOptions.value = toLabelValueOpts(g.data || [])
}

const addLine = () => {
  form.items.push({ uid: crypto.randomUUID(), goodsId: undefined, bookQty: 0, checkQty: 0, untaxPrice: 0 })
}

const downloadExcelTemplate = () => {
  useDownload(() => downloadStockCheckImportTemplate())
}

const handleExcelUpload = (options: RequestOption) => {
  const controller = new AbortController()
  ;(async () => {
    const { onError, onSuccess, fileItem, name = 'file' } = options
    if (!form.warehouseId) {
      Message.warning('请先选择仓库，再导入实盘明细')
      onError(new Error('warehouse required'))
      return
    }
    const formData = new FormData()
    formData.append(name as string, fileItem.file as Blob)
    try {
      const res = await parseImportStockCheck(formData, form.warehouseId)
      const items = res.data?.items || []
      if (!items.length) {
        Message.warning('未解析到有效明细')
        onError(new Error('empty'))
        return
      }
      form.items = items.map((i: any) => {
        const goodsId = toBizId(i.goodsId)
        ensureSelectOpt(goodsOptions.value, goodsId, i.goodsName)
        return {
          uid: crypto.randomUUID(),
          goodsId,
          bookQty: Number(i.bookQty ?? 0),
          checkQty: Number(i.checkQty ?? 0),
          untaxPrice: Number(i.untaxPrice ?? 0),
        }
      })
      Message.success(`已载入 ${items.length} 行实盘明细，请核对后保存草稿`)
      onSuccess(res)
    } catch (e) {
      onError(e)
    }
  })()
  return {
    abort() {
      controller.abort()
    },
  }
}

const reset = () => {
  form.checkDate = undefined
  form.warehouseId = undefined
  form.remark = ''
  form.items = []
  legacyStockQtyMap.value = {}
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (readonly.value) return true
  try {
    if (!form.checkDate || !form.warehouseId || !form.items.length) {
      Message.warning('请完善盘点日期、仓库与明细')
      return false
    }
    if (form.items.some((i: any) => !i.goodsId)) {
      Message.warning('请选择商品')
      return false
    }
    const payload = {
      checkDate: form.checkDate,
      warehouseId: form.warehouseId,
      remark: form.remark,
      items: form.items.map((i: any) => ({
        goodsId: i.goodsId,
        bookQty: i.bookQty,
        checkQty: i.checkQty,
        untaxPrice: i.untaxPrice,
      })),
    }
    if (isUpdate.value) {
      await updateStockCheck(payload, dataId.value)
      Message.success('修改成功')
    } else {
      await addStockCheck(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fill = async (id: string) => {
  const { data } = await getStockCheck(id)
  form.checkDate = data.checkDate
  form.warehouseId = toBizId(data.warehouseId)
  form.remark = data.remark
  form.items = (data.items || []).map((i: any) => ({
    uid: crypto.randomUUID(),
    goodsId: toBizId(i.goodsId),
    bookQty: Number(i.bookQty),
    checkQty: Number(i.checkQty),
    untaxPrice: i.untaxPrice != null ? Number(i.untaxPrice) : 0,
  }))
  ensureSelectOpt(warehouseOptions.value, form.warehouseId, data.warehouseName)
  ;(data.items || []).forEach((i: any) => ensureSelectOpt(goodsOptions.value, i.goodsId, i.goodsName))
  // 编辑草稿时按当前库存重带账面数量与未税单价（保存时账面仍由后端按库存重算）
  if (!readonly.value) {
    await refreshItemsFromStock()
  }
}

const open = async () => {
  await loadOptions()
  visible.value = true
}

const onAdd = async () => {
  reset()
  addLine()
  await open()
}
const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  await loadOptions()
  await fill(id)
  visible.value = true
}
const onView = async (id: string) => {
  reset()
  readonly.value = true
  dataId.value = id
  await loadOptions()
  await fill(id)
  visible.value = true
}

defineExpose({ onAdd, onUpdate, onView })
</script>

<style lang="scss">
@import '../styles/psi-form.scss';
</style>
