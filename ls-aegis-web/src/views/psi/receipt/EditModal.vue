<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 640 ? 640 : '100%'"
    :ok-text="readonly ? '关闭' : '保存草稿'"
    :hide-cancel="readonly"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <a-form class="psi-biz-form" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="回款日期" required>
            <a-date-picker v-model="form.receiptDate" show-time :format="DATETIME_FORMAT" :value-format="DATETIME_FORMAT" :disabled="readonly || !editorReady" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户" required>
            <a-select
              v-model="form.unitId"
              :options="unitOptions"
              :disabled="readonly || !editorReady"
              allow-search
              placeholder="请选择客户"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收款账户">
            <a-select
              v-model="form.accountId"
              :options="accountOptions"
              :disabled="readonly || !editorReady"
              allow-search
              allow-clear
              placeholder="请选择账户"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收款方式">
            <a-select v-model="form.payMethod" :disabled="readonly || !editorReady" allow-clear placeholder="请选择">
              <a-option value="TRANSFER">银行转账</a-option>
              <a-option value="CASH">现金</a-option>
              <a-option value="CHECK">支票</a-option>
              <a-option value="OTHER">其他</a-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="金额" required>
            <a-input-number v-model="form.amount" :min="0.01" :precision="2" :disabled="readonly || !editorReady" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model="form.remark" :disabled="readonly || !editorReady" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useWindowSize } from '@vueuse/core'
import {
  addReceipt,
  getReceipt,
  listAccount,
  listAccountOptions,
  listUnitOptions,
  updateReceipt,
} from '@/apis/psi'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
let editorRequestVersion = 0
const editorReady = ref(false)
const editorSaving = ref(false)
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '回款详情' : isUpdate.value ? '修改回款草稿' : '新增回款草稿'))

const unitOptions = ref<{ label: string, value: string }[]>([])
const accountOptions = ref<{ label: string, value: string }[]>([])

const form = reactive<any>({
  receiptDate: undefined,
  unitId: undefined,
  accountId: undefined,
  payMethod: 'TRANSFER',
  amount: undefined,
  remark: '',
})

const loadAccountOptions = async () => {
  try {
    const { data } = await listAccountOptions()
    if (data?.length) {
      accountOptions.value = toLabelValueOpts(data)
      return
    }
  } catch {
    // 无账户下拉接口时回退分页
  }
  const { data } = await listAccount({ page: 1, size: 200, sort: ['id,desc'] })
  accountOptions.value = (data?.list || []).map((i: any) => ({
    label: i.accountNo ? `${i.accountName} (${i.accountNo})` : i.accountName,
    value: String(i.id),
  }))
}

const loadOptions = async () => {
  const [u] = await Promise.all([listUnitOptions('CUSTOMER'), loadAccountOptions()])
  unitOptions.value = toLabelValueOpts(u.data || [])
}

const reset = () => {
  editorRequestVersion++
  editorReady.value = false
  form.receiptDate = undefined
  form.unitId = undefined
  form.accountId = undefined
  form.payMethod = 'TRANSFER'
  form.amount = undefined
  form.remark = ''
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (!editorReady.value || editorSaving.value) return false
  if (readonly.value) return true
  const version = editorRequestVersion
  const id = dataId.value
  editorSaving.value = true
  try {
    if (!form.receiptDate || !form.unitId || !form.amount) {
      Message.warning('请完善回款日期、客户与金额')
      return false
    }
    const payload = {
      receiptDate: form.receiptDate,
      unitId: form.unitId,
      accountId: form.accountId,
      payMethod: form.payMethod,
      amount: form.amount,
      remark: form.remark,
    }
    if (isUpdate.value) {
      await updateReceipt(payload, id)
      if (version !== editorRequestVersion || id !== dataId.value) return false
      Message.success('修改成功')
    } else {
      await addReceipt(payload)
      if (version !== editorRequestVersion || id !== dataId.value) return false
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  } finally {
    editorSaving.value = false
  }
}

const fill = async (id: string, version: number) => {
  const { data } = await getReceipt(id)
  if (version !== editorRequestVersion || id !== dataId.value) return false
  if (!data || String(data.id) !== id) {
    Message.warning('单据不存在或无权访问')
    return false
  }
  form.receiptDate = data.receiptDate
  form.unitId = toBizId(data.unitId)
  form.accountId = toBizId(data.accountId)
  form.payMethod = data.payMethod || 'TRANSFER'
  form.amount = Number(data.amount)
  form.remark = data.remark
  ensureSelectOpt(unitOptions.value, form.unitId, (data as any).unitName)
  ensureSelectOpt(accountOptions.value, form.accountId, (data as any).accountName)
  return version === editorRequestVersion && id === dataId.value
}

const open = async (version = editorRequestVersion) => {
  await loadOptions()
  if (version !== editorRequestVersion) return
  editorReady.value = true
  visible.value = true
}

const onAdd = async () => {
  reset()
  await open()
}
const onUpdate = async (id: string) => {
  reset()
  const version = editorRequestVersion
  dataId.value = id
  try {
    await loadOptions()
    if (version !== editorRequestVersion || id !== dataId.value) return
    if (!(await fill(id, version))) return
    if (version !== editorRequestVersion || id !== dataId.value) return
    editorReady.value = true
    visible.value = true
  } catch {
    if (version === editorRequestVersion) visible.value = false
  }
}
const onView = async (id: string) => {
  reset()
  readonly.value = true
  const version = editorRequestVersion
  dataId.value = id
  try {
    await loadOptions()
    if (version !== editorRequestVersion || id !== dataId.value) return
    if (!(await fill(id, version))) return
    if (version !== editorRequestVersion || id !== dataId.value) return
    editorReady.value = true
    visible.value = true
  } catch {
    if (version === editorRequestVersion) visible.value = false
  }
}

defineExpose({ onAdd, onUpdate, onView })
</script>

<style lang="scss">
@import '../styles/psi-form.scss';
</style>
