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
import { verifyPayment, listUnsettledReceivable } from '@/apis/eoms'
import { listUnitOptions } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useResetReactive } from '@/hooks'
import { useDict } from '@/hooks/app'
import { toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { eoms_payment_method } = useDict('eoms_payment_method')

const visible = ref(false)
const title = '新增维修收款'
const formRef = ref<InstanceType<typeof GiForm>>()
const customerOptions = ref<{ label: string, value: string }[]>([])
const receivableOptions = ref<{ label: string, value: string }[]>([])
const receiptToken = ref('')

const [form, resetForm] = useResetReactive({
  customerId: undefined as string | undefined,
  receivableId: undefined as string | undefined,
  amount: 0,
})

let customerLoad = 0
watch(() => form.customerId, async (customerId) => {
  const request = ++customerLoad
  form.receivableId = undefined
  receivableOptions.value = []
  if (!customerId) return
  const { data } = await listUnsettledReceivable(customerId)
  if (request !== customerLoad) return
  receivableOptions.value = (data || []).map((row) => ({
    label: `应收 #${row.id} · 待收 ${row.arrearsAmount ?? 0}`,
    value: row.id,
  }))
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
  { label: '待核销应收单', field: 'receivableId', type: 'select', span: 24, required: true, props: { options: receivableOptions, allowSearch: true } },
  { label: '金额', field: 'amount', type: 'input-number', span: 12, required: true, props: { min: 0.01, precision: 2 } },
  {
    label: '收款方式',
    field: 'paymentMethod',
    type: 'select',
    span: 12,
    props: {
      options: computed(() =>
        (eoms_payment_method.value || []).map((d) => ({ label: d.label, value: d.value })),
      ),
      allowClear: true,
    },
  },
  {
    label: '收款时间',
    field: 'paymentTime',
    type: 'date-picker',
    span: 24,
    props: { showTime: true, format: DATETIME_FORMAT, valueFormat: DATETIME_FORMAT },
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
    await verifyPayment({ ...form, idempotentToken: receiptToken.value })
    Message.success('新增成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  reset()
  receiptToken.value = crypto.randomUUID()
  await loadCustomers()
  visible.value = true
}

defineExpose({ onAdd })
</script>
