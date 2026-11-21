<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 560 ? 560 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <GiForm ref="formRef" v-model="form" :columns="columns" layout="vertical" />
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { addReceivable, getReceivable, updateReceivable } from '@/apis/eoms'
import { listUnitOptions } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'
import { useDict } from '@/hooks/app'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { eoms_receivable_status } = useDict('eoms_receivable_status')

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改应收' : '新增应收'))
const formRef = ref<InstanceType<typeof GiForm>>()
const customerOptions = ref<{ label: string, value: string }[]>([])

const [form, resetForm] = useResetReactive({
  amount: 0,
  receivedAmount: 0,
})

const loadCustomers = async () => {
  const { data } = await listUnitOptions('CUSTOMER')
  customerOptions.value = toLabelValueOpts(data || [])
}

const columns: ColumnItem[] = reactive([
  {
    label: '客户',
    field: 'customerId',
    type: 'select',
    span: 24,
    required: true,
    props: {
      options: customerOptions,
      allowSearch: true,
    },
  },
  { label: '应收金额', field: 'amount', type: 'input-number', span: 12, required: true, props: { min: 0, precision: 2 } },
  { label: '已收金额', field: 'receivedAmount', type: 'input-number', span: 12, props: { min: 0, precision: 2 } },
  {
    label: '状态',
    field: 'status',
    type: 'select',
    span: 24,
    props: {
      options: computed(() =>
        (eoms_receivable_status.value || []).map((d) => ({ label: d.label, value: d.value })),
      ),
      allowClear: true,
    },
  },
  { label: '备注', field: 'remark', type: 'textarea', span: 24 },
])

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    if (isUpdate.value) {
      await updateReceivable({ ...form, id: dataId.value })
      Message.success('修改成功')
    } else {
      await addReceivable(form)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  dataId.value = ''
  reset()
  await loadCustomers()
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  await loadCustomers()
  const { data } = await getReceivable(id)
  Object.assign(form, data, {
    customerId: toBizId(data.customerId),
    amount: data.amount != null ? Number(data.amount) : 0,
    receivedAmount: data.receivedAmount != null ? Number(data.receivedAmount) : 0,
  })
  ensureSelectOpt(customerOptions.value, form.customerId, (data as any).customerName)
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
