<template>
  <a-drawer
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :esc-to-close="false"
    :width="width >= 560 ? 560 : '100%'"
    ok-text="确认导入"
    cancel-text="取消"
    @before-ok="save"
    @close="reset"
  >
    <a-alert style="margin-bottom: 15px">
      请按模板填写后先上传解析，再确认导入。编码留空则系统自动生成。
      <template #action>
        <a-link @click="downloadTemplate">
          <template #icon><icon-download /></template>
          下载模板
        </a-link>
      </template>
    </a-alert>
    <a-form :model="form" layout="vertical">
      <a-form-item label="上传文件">
        <a-upload
          draggable
          :custom-request="handleUpload"
          :limit="1"
          :show-retry-button="false"
          :show-cancel-button="false"
          tip="仅支持 xls、xlsx"
          :file-list="uploadFile"
          accept=".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        />
      </a-form-item>
      <div v-if="parseResult.importKey" style="margin-bottom: 16px">
        <a-space size="large">
          <a-statistic title="总计行数" :value="parseResult.totalRows || 0" />
          <a-statistic title="有效行数" :value="parseResult.validRows || 0" />
          <a-statistic title="库中已存在" :value="parseResult.duplicateRows || 0" />
        </a-space>
      </div>
      <a-form-item label="已存在时">
        <a-radio-group v-model="form.duplicatePolicy" type="button">
          <a-radio :value="1">跳过</a-radio>
          <a-radio :value="2">覆盖更新</a-radio>
          <a-radio :value="3">停止导入</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="默认状态（行内未填时）">
        <a-switch
          v-model="form.defaultStatus"
          :checked-value="1"
          :unchecked-value="2"
          checked-text="启用"
          unchecked-text="禁用"
          type="round"
        />
      </a-form-item>
      <a-alert v-if="hint" type="info" :show-icon="true">{{ hint }}</a-alert>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import { Message, type RequestOption } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { useDownload, useResetReactive } from '@/hooks'

export interface PsiImportParseResult {
  importKey?: string
  totalRows?: number
  validRows?: number
  duplicateRows?: number
}

const props = defineProps<{
  title: string
  hint?: string
  downloadTemplateApi: () => Promise<any>
  parseApi: (data: FormData) => Promise<{ data: PsiImportParseResult }>
  importApi: (data: any) => Promise<{ data: { insertRows?: number, updateRows?: number, skipRows?: number } }>
}>()

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const visible = ref(false)
const uploadFile = ref([])
const parseResult = ref<PsiImportParseResult>({})
const [form, resetForm] = useResetReactive({
  duplicatePolicy: 1,
  defaultStatus: 1,
})

const reset = () => {
  uploadFile.value = []
  parseResult.value = {}
  resetForm()
}

const downloadTemplate = () => {
  useDownload(() => props.downloadTemplateApi())
}

const handleUpload = (options: RequestOption) => {
  const controller = new AbortController()
  ;(async () => {
    const { onProgress, onError, onSuccess, fileItem, name = 'file' } = options
    onProgress(20)
    const formData = new FormData()
    formData.append(name as string, fileItem.file as Blob)
    try {
      const res = await props.parseApi(formData)
      parseResult.value = res.data || {}
      Message.success('解析成功')
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

const save = async () => {
  try {
    if (!parseResult.value.importKey) {
      Message.warning('请先上传并解析文件')
      return false
    }
    const res = await props.importApi({
      importKey: parseResult.value.importKey,
      duplicatePolicy: form.duplicatePolicy,
      defaultStatus: form.defaultStatus,
    })
    const d = res.data || {}
    Message.success(`导入完成：新增 ${d.insertRows ?? 0}，更新 ${d.updateRows ?? 0}，跳过 ${d.skipRows ?? 0}`)
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onOpen = () => {
  reset()
  visible.value = true
}

defineExpose({ onOpen })
</script>
