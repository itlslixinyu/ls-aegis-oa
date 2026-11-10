<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 720 ? 720 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <a-form class="psi-biz-form" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="模板名称" required>
            <a-input v-model="form.templateName" :max-length="128" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="业务类型" required>
            <a-select v-model="form.bizType" :options="printBizTypeOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="默认模板">
            <a-switch v-model="form.isDefault" :checked-value="1" :unchecked-value="0" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态">
            <a-switch
              v-model="form.status"
              type="round"
              :checked-value="1"
              :unchecked-value="2"
              checked-text="启用"
              unchecked-text="禁用"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="Word 套打模板">
            <PlatformFileField
              v-model="form.fileId"
              accept=".docx"
              variant="drag"
              drag-title="点击或拖拽上传 Word 模板"
              parent-path="/psi/print"
              tip="仅支持 .docx；上传后出入库「Word」导出按此模板套打，未上传则用系统内置模板。"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="HTML 模板">
            <a-textarea
              v-model="form.contentHtml"
              :auto-size="{ minRows: 10, maxRows: 18 }"
              placeholder="可留空使用系统内置出库单样式。占位符：{{docNo}} {{outDate}} {{warehouseName}} {{unitName}} {{totalAmount}} {{remark}} {{itemsHtml}}"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="字段映射说明">
            <a-textarea v-model="form.fieldMapping" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="可选 JSON 备注" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model="form.remark" :auto-size="{ minRows: 2, maxRows: 3 }" />
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
import { addPrintTemplate, getPrintTemplate, updatePrintTemplate } from '@/apis/psi'
import { useResetReactive } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { psi_print_biz_type } = useDict('psi_print_biz_type')
const printBizTypeOptions = computed(() => dictSelectOptions(psi_print_biz_type.value))

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改打印模板' : '新增打印模板'))

const [form, resetForm] = useResetReactive({
  templateName: '',
  bizType: 'STOCK_OUT',
  fileId: undefined as string | undefined,
  contentHtml: '',
  fieldMapping: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

const reset = () => {
  dataId.value = ''
  resetForm()
}

const save = async () => {
  try {
    if (!form.templateName || !form.bizType) {
      Message.warning('请填写模板名称与业务类型')
      return false
    }
    const payload = {
      ...form,
      fileId: form.fileId || undefined,
      isDefault: Number(form.isDefault) || 0,
      status: (Number(form.status) || 1) as 1 | 2,
      contentHtml: form.contentHtml || undefined,
      fieldMapping: form.fieldMapping || undefined,
    }
    if (isUpdate.value) {
      await updatePrintTemplate(payload, dataId.value)
      Message.success('修改成功')
    } else {
      await addPrintTemplate(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = () => {
  reset()
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  const { data } = await getPrintTemplate(id)
  Object.assign(form, data)
  form.fileId = data.fileId != null ? String(data.fileId) : undefined
  form.isDefault = Number(data.isDefault) || 0
  form.status = (Number(data.status) || 1) as 1 | 2
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>

<style scoped lang="scss">
@import '../styles/psi-form.scss';
</style>
