<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 640 ? 640 : '100%'"
    :ok-text="readonly ? '关闭' : '保存'"
    :hide-cancel="readonly"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <a-form class="psi-biz-form" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col v-if="readonly || isUpdate" :span="12">
          <a-form-item label="合同编号">
            <a-input v-model="form.contractNo" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="合同名称" required>
            <a-input v-model="form.name" :disabled="readonly" :max-length="200" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="签订日期" required>
            <a-date-picker
              v-model="form.signDate"
              show-time
              :format="DATETIME_FORMAT"
              :value-format="DATETIME_FORMAT"
              :disabled="readonly"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户" required>
            <a-select
              v-model="form.unitId"
              :options="unitOptions"
              :disabled="readonly"
              allow-search
              placeholder="请选择客户"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="含税总额" required>
            <a-input-number
              v-model="form.taxInclusiveAmount"
              :min="0"
              :precision="2"
              :disabled="readonly"
              placeholder="请输入"
              hide-button
              @focus="onAmountFocus"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="开始日期">
            <a-date-picker
              v-model="form.startDate"
              show-time
              :format="DATETIME_FORMAT"
              :value-format="DATETIME_FORMAT"
              :disabled="readonly"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="结束日期">
            <a-date-picker
              v-model="form.endDate"
              show-time
              :format="DATETIME_FORMAT"
              :value-format="DATETIME_FORMAT"
              :disabled="readonly"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="合同附件">
            <PlatformFileField
              v-model="form.fileId"
              :disabled="readonly"
              multiple
              variant="drag"
              drag-title="点击或拖拽上传合同附件"
              parent-path="/psi"
              tip="支持 Word、PDF、图片等，可多文件；上传后可点击预览。"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model="form.remark" :disabled="readonly" :auto-size="{ minRows: 2, maxRows: 4 }" :max-length="500" />
          </a-form-item>
        </a-col>
        <a-col v-if="readonly && form.progress != null" :span="24">
          <a-form-item label="执行进度">
            <a-progress :percent="progressPercent" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import PlatformFileField from '@/components/PlatformFileField/index.vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import {
  addContractSale,
  getContractSale,
  listUnitOptions,
  updateContractSale,
} from '@/apis/psi'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '销售合同详情' : isUpdate.value ? '修改销售合同' : '新增销售合同'))

const unitOptions = ref<{ label: string, value: string }[]>([])

const form = reactive<any>({
  contractNo: '',
  name: '',
  signDate: undefined,
  unitId: undefined,
  taxInclusiveAmount: undefined as number | undefined,
  startDate: undefined,
  endDate: undefined,
  fileId: undefined as string | undefined,
  remark: '',
  progress: undefined as number | undefined,
})

/** 后端 0~1 → 进度条 0~100 */
const progressPercent = computed(() => {
  const n = Number(form.progress)
  if (Number.isNaN(n) || n <= 0) return 0
  const pct = n <= 1 ? n * 100 : n
  return Math.min(100, Math.round(pct * 100) / 100)
})

const loadOptions = async () => {
  const { data } = await listUnitOptions('CUSTOMER')
  unitOptions.value = toLabelValueOpts(data || [])
}

/** 聚焦时去掉占位 0，避免先删再输入 */
const onAmountFocus = () => {
  if (form.taxInclusiveAmount === 0) {
    form.taxInclusiveAmount = undefined
  }
}

const reset = () => {
  form.contractNo = ''
  form.name = ''
  form.signDate = undefined
  form.unitId = undefined
  form.taxInclusiveAmount = undefined
  form.startDate = undefined
  form.endDate = undefined
  form.fileId = undefined
  form.remark = ''
  form.progress = undefined
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (readonly.value) return true
  try {
    if (!form.name || !form.unitId || form.taxInclusiveAmount == null || !form.signDate) {
      Message.warning('请完善合同名称、客户、含税总额与签订日期')
      return false
    }
    const payload = {
      name: form.name,
      signDate: form.signDate,
      unitId: form.unitId,
      taxInclusiveAmount: form.taxInclusiveAmount,
      startDate: form.startDate,
      endDate: form.endDate,
      fileId: form.fileId ? String(form.fileId) : '',
      remark: form.remark,
    }
    if (isUpdate.value) {
      await updateContractSale(payload, dataId.value)
      Message.success('修改成功')
    } else {
      await addContractSale(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fill = async (id: string) => {
  const { data } = await getContractSale(id)
  form.contractNo = data.contractNo || ''
  form.name = data.name
  form.signDate = data.signDate
  form.unitId = toBizId(data.unitId)
  form.taxInclusiveAmount = data.taxInclusiveAmount != null ? Number(data.taxInclusiveAmount) : undefined
  form.startDate = data.startDate
  form.endDate = data.endDate
  form.fileId = toBizId(data.fileId)
  form.remark = data.remark || ''
  form.progress = data.progress != null ? Number(data.progress) : undefined
  ensureSelectOpt(unitOptions.value, form.unitId, (data as any).unitName)
}

const open = async () => {
  await loadOptions()
  visible.value = true
}

const onAdd = async () => {
  reset()
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
