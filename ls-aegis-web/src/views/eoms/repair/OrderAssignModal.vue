<template>
  <a-modal
    v-model:visible="visible"
    title="派单"
    :mask-closable="false"
    :width="width >= 480 ? 480 : '100%'"
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
import { type RepairOrderResp, assignEngineer, listEngineerOptions } from '@/apis/eoms'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'
import { ensureSelectOpt, toBizId, toUserSelectOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const orderId = ref<string>('')
const formRef = ref<InstanceType<typeof GiForm>>()
const engineerOptions = ref<{ label: string, value: string }[]>([])

const [form, resetForm] = useResetReactive({
  engineerId: undefined as string | undefined,
})

const columns: ColumnItem[] = reactive([
  {
    label: '工程师',
    field: 'engineerId',
    type: 'select',
    span: 24,
    required: true,
    props: { options: engineerOptions, allowSearch: true },
  },
])

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    await assignEngineer(orderId.value, form.engineerId!)
    Message.success('派单成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onOpen = async (record: RepairOrderResp) => {
  reset()
  orderId.value = record.id
  const { data } = await listEngineerOptions()
  engineerOptions.value = toUserSelectOpts(data || [])
  form.engineerId = toBizId(record.engineerId)
  ensureSelectOpt(engineerOptions.value, form.engineerId, record.engineerName)
  visible.value = true
}

defineExpose({ onOpen })
</script>
