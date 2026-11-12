<template>
  <GiPageLayout>
    <a-card title="印章提取工具" :bordered="false">
      <a-space wrap style="margin-bottom: 16px">
        <a-upload :auto-upload="false" :disabled="extracting || saving" accept="image/png,image/jpeg,image/gif" :show-file-list="false" @change="onFileChange">
          <template #upload-button>
            <a-button>选择文件</a-button>
          </template>
        </a-upload>
        <span class="file-name">{{ selectedFileName || '未选择文件' }}</span>
        <a-button type="primary" :disabled="!selectedFile" :loading="extracting" @click="doExtract">印章提取</a-button>
        <a-input v-model="customFileName" :disabled="saving" placeholder="请输入文件名" style="width: 180px" />
        <a-select
          v-model="sealCategory"
          :disabled="extracting || saving"
          placeholder="印章类型"
          style="width: 180px"
          :options="extractCategoryOptions"
        />
        <a-button type="outline" status="warning" :disabled="!extractPreview" :loading="saving" @click="doSave">保存</a-button>
        <a-button type="outline" status="success" :disabled="!currentExtractRecordId" @click="downloadExtract">下载提取结果</a-button>
      </a-space>

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="12">
          <div class="preview-box">
            <div class="preview-label">原始图片</div>
            <div class="preview-area">
              <img v-if="originalPreview" :src="originalPreview" alt="原始图片" />
              <a-empty v-else description="请先选择图片" />
            </div>
          </div>
        </a-col>
        <a-col :span="12">
          <div class="preview-box">
            <div class="preview-label">提取结果</div>
            <div class="preview-area">
              <img v-if="extractPreview" :src="extractPreview" alt="提取结果" />
              <a-empty v-else description="点击「印章提取」生成结果" />
            </div>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <BaseTable
      row-key="id"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      style="margin-top: 12px"
      @refresh="refresh"
    >
      <template #thumb="{ record }">
        <a-link v-if="record.id" @click="previewExtract(record)">{{ record.extractFileDisplayName || record.extractFileName || '查看' }}</a-link>
        <span v-else>-</span>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link :disabled="!record.id" @click="downloadRecord(record)">下载</a-link>
          <a-link status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>

    <a-modal v-model:visible="previewVisible" :title="previewTitle" :width="720" :footer="false" @close="onPreviewClose">
      <div class="dialog-preview">
        <img v-if="previewUrl" :src="previewUrl" />
        <a-empty v-else description="暂无可预览图片" />
      </div>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { SEAL_EXTRACT_SIZE_MM, SignCol } from '../constants/table-col'
import {
  type ExtractRecordResp,
  deleteExtractRecord,
  downloadExtractFile,
  extractSealPreview,
  extractSealSave,
  fetchSignImageObjectUrl,
  getExtractImageResource,
  getExtractList,
} from '@/apis/sign'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

defineOptions({ name: 'SealExtract' })

const { sign_seal_extract_category } = useDict('sign_seal_extract_category')
const extractCategoryOptions = computed(() =>
  dictSelectOptions(sign_seal_extract_category.value).map((d) => ({
    label: `${d.label}（${SEAL_EXTRACT_SIZE_MM[String(d.value)] ?? '-'}mm）`,
    value: d.value,
  })),
)

const selectedFile = ref<File | null>(null)
const selectedFileName = ref('')
const originalPreview = ref('')
const extractPreview = ref('')
const currentExtractRecordId = ref<string | number | null>(null)
const extracting = ref(false)
const saving = ref(false)
const customFileName = ref('')
const sealCategory = ref('公章')
watch(sealCategory, () => { extractPreview.value = ''; currentExtractRecordId.value = null })

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => getExtractList(page), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '原始图片', dataIndex: 'originalFileName', ellipsis: true, tooltip: true },
  { title: '提取图片', dataIndex: 'extractFileName', slotName: 'thumb', width: SignCol.nameLg },
  { title: '生成时间', dataIndex: 'createTime', width: SignCol.date },
  { title: '生成人', dataIndex: 'createBy', width: SignCol.type },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: SignCol.actionM },
]

let imageReadVersion = 0
let disposed = false
let previewVersion = 0
const onFileChange = (fileList: any[]) => {
  if (disposed || extracting.value || saving.value) return
  const fileItem = fileList?.[fileList.length - 1]
  const file = fileItem?.file as File | undefined
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/jpg', 'image/gif'].includes(file.type)) { Message.error('请选择JPG/PNG/GIF图片'); return }
  if (!file.size || file.size > 10 * 1024 * 1024) { Message.error('图片不能为空且不能超过10MB'); return }
  const version = ++imageReadVersion
  selectedFile.value = file
  selectedFileName.value = file.name
  extractPreview.value = ''
  currentExtractRecordId.value = null
  const reader = new FileReader()
  reader.onload = (e) => {
    if (disposed || version !== imageReadVersion) return
    originalPreview.value = String(e.target?.result || '')
  }
  reader.onerror = () => { if (!disposed && version === imageReadVersion) Message.error('图片读取失败，请重新选择') }
  reader.readAsDataURL(file)
}

const doExtract = async () => {
  if (extracting.value || saving.value) return
  if (!selectedFile.value) {
    Message.warning('请先选择文件')
    return
  }
  extracting.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    if (sealCategory.value) fd.append('sealCategory', sealCategory.value)
    const res = await extractSealPreview(fd)
    if (disposed) return
    Message.success('提取成功，请输入文件名后点击「保存」')
    if (res.data?.extractImageBase64) {
      extractPreview.value = `data:image/png;base64,${res.data.extractImageBase64}`
      currentExtractRecordId.value = null
    }
  } finally {
    extracting.value = false
  }
}

const doSave = async () => {
  if (saving.value || extracting.value) return
  if (!selectedFile.value) {
    Message.warning('请先选择文件')
    return
  }
  if (!customFileName.value.trim()) {
    Message.warning('请先输入文件名')
    return
  }
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('fileName', customFileName.value.trim())
    if (sealCategory.value) fd.append('sealCategory', sealCategory.value)
    const res = await extractSealSave(fd)
    if (disposed) return
    Message.success('保存成功')
    currentExtractRecordId.value = res.data?.recordId ?? null
    search()
  } finally {
    saving.value = false
  }
}

const downloadExtract = () => {
  if (!currentExtractRecordId.value) {
    Message.warning('请先保存提取结果')
    return
  }
  useDownload(() => downloadExtractFile(String(currentExtractRecordId.value!)), `${customFileName.value || 'extracted-seal'}.png`)
}

const downloadRecord = (row: ExtractRecordResp) => {
  useDownload(
    () => downloadExtractFile(row.id),
    row.extractFileDisplayName || row.extractFileName || 'extracted-seal.png',
  )
}

const onDelete = (row: ExtractRecordResp) =>
  handleDelete(() => deleteExtractRecord(row.id), {
    content: `确认删除记录「${row.extractFileName || row.extractFileDisplayName}」？`,
  })

const previewVisible = ref(false)
const previewTitle = ref('')
const previewUrl = ref('')

const previewExtract = async (row: ExtractRecordResp) => {
  const version = ++previewVersion
  previewTitle.value = '提取图片预览'
  previewVisible.value = true
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  try {
    const url = await fetchSignImageObjectUrl(getExtractImageResource(row.id))
    if (disposed || version !== previewVersion) { URL.revokeObjectURL(url); return }
    previewUrl.value = url
  } catch {
    if (!disposed && version === previewVersion) Message.warning('图片加载失败')
  }
}

const onPreviewClose = () => {
  previewVersion++
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

onBeforeUnmount(() => { disposed = true; imageReadVersion++; onPreviewClose() })
</script>

<style scoped>
.file-name {
  color: var(--color-text-3);
  font-size: 13px;
}
.preview-box {
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  overflow: hidden;
}
.preview-label {
  padding: 8px 12px;
  background: var(--color-fill-1);
  border-bottom: 1px solid var(--color-border-2);
  font-size: 13px;
}
.preview-area {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.preview-area img {
  max-width: 100%;
  max-height: 100%;
  padding: 12px;
}
.dialog-preview {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dialog-preview img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
</style>
