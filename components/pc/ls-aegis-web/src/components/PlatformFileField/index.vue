<template>
  <div class="platform-file-field" :class="{ 'platform-file-field--drag': isDrag }">
    <a-upload
      v-if="!disabled && isDrag"
      name="file"
      draggable
      :show-file-list="false"
      :accept="accept"
      :multiple="multiple"
      :custom-request="handleUpload"
      :on-before-upload="beforeUpload"
      :disabled="uploading"
      class="platform-file-field__upload-drag"
    >
      <template #upload-button>
        <div
          class="platform-file-field__drop"
          :class="{ 'is-uploading': uploading, 'is-filled': idList.length > 0 }"
        >
          <a-spin v-if="uploading && !idList.length" :size="20" />
          <template v-else-if="!idList.length">
            <div class="platform-file-field__badge">UP</div>
            <div class="platform-file-field__title">{{ dragTitle }}</div>
            <div v-if="dragDesc" class="platform-file-field__desc">{{ dragDesc }}</div>
          </template>
          <template v-else>
            <div class="platform-file-field__files" @click.stop>
              <div
                v-for="id in idList"
                :key="id"
                class="platform-file-field__file"
                @click.stop="openFile(id)"
              >
                <span
                  class="platform-file-field__type"
                  :class="`is-${fileTypeMeta(id).tone}`"
                >{{ fileTypeMeta(id).letter }}</span>
                <span class="platform-file-field__name" :title="displayName(id)">
                  <icon-loading v-if="openingId === id" />
                  {{ displayName(id) }}
                </span>
                <a-button
                  class="platform-file-field__remove"
                  size="mini"
                  status="danger"
                  @click.stop="removeOne(id)"
                >
                  移除
                </a-button>
              </div>
            </div>
            <div class="platform-file-field__more">
              <a-spin v-if="uploading" :size="14" />
              <span v-else>{{ multiple ? '继续点击或拖拽添加文件' : '点击或拖拽可替换文件' }}</span>
            </div>
          </template>
        </div>
      </template>
      <template #tip />
    </a-upload>

    <div
      v-else-if="disabled && isDrag"
      class="platform-file-field__drop is-filled is-readonly"
    >
      <div v-if="idList.length" class="platform-file-field__files">
        <div
          v-for="id in idList"
          :key="id"
          class="platform-file-field__file"
          @click="openFile(id)"
        >
          <span
            class="platform-file-field__type"
            :class="`is-${fileTypeMeta(id).tone}`"
          >{{ fileTypeMeta(id).letter }}</span>
          <span class="platform-file-field__name" :title="displayName(id)">
            <icon-loading v-if="openingId === id" />
            {{ displayName(id) }}
          </span>
        </div>
      </div>
      <span v-else class="platform-file-field__empty">无附件</span>
    </div>

    <a-space v-if="!isDrag" wrap>
      <a-upload
        v-if="!disabled"
        name="file"
        :show-file-list="false"
        :accept="accept"
        :multiple="multiple"
        :custom-request="handleUpload"
        :on-before-upload="beforeUpload"
        :disabled="uploading"
      >
        <a-button type="outline" :loading="uploading">
          <template #icon><icon-upload /></template>
          {{ uploadLabel }}
        </a-button>
      </a-upload>
      <template v-if="idList.length">
        <a-tag
          v-for="id in idList"
          :key="id"
          color="arcoblue"
          size="small"
          :closable="!disabled"
          @close="removeOne(id)"
        >
          <a-link :disabled="!!openingId" @click="openFile(id)">
            <icon-loading v-if="openingId === id" />
            {{ displayName(id) }}
          </a-link>
        </a-tag>
        <a-link v-if="!disabled" status="danger" @click="clear">清除</a-link>
      </template>
      <span v-else-if="disabled" class="platform-file-field__empty">无附件</span>
    </a-space>
    <div v-if="tip && !isDrag" class="platform-file-field__tip">{{ tip }}</div>

    <FilePreview ref="filePreviewRef" @close="invalidateOpen" />
    <a-modal
      v-model:visible="imageVisible"
      :title="imageTitle"
      :footer="false"
      :width="width >= 900 ? 900 : '100%'"
      unmount-on-close
      @close="closeImagePreview"
    >
      <div class="platform-file-field__img-wrap">
        <img v-if="imageUrl" :src="imageUrl" alt="预览" />
      </div>
    </a-modal>
    <a-modal
      v-model:visible="textVisible"
      :title="textTitle"
      :footer="false"
      :width="width >= 800 ? 800 : '100%'"
      unmount-on-close
    >
      <pre class="platform-file-field__text">{{ textContent }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { Message, type RequestOption } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { downloadFile, getFileInfo, upload as uploadCommon } from '@/apis/system/common'
import FilePreview from '@/components/FilePreview/index.vue'
import type { ExcelConfig } from '@/components/FilePreview/type'
import { ExcelTypes, ImageTypes, OfficeTypes, WordTypes } from '@/constant/file'

export interface PlatformFileUploaded {
  id: string
  url?: string
  name?: string
  size?: number
}

const props = withDefaults(defineProps<{
  modelValue?: string | number | null
  disabled?: boolean
  accept?: string
  tip?: string
  parentPath?: string
  /** 多文件时 modelValue 为逗号分隔的文件 ID */
  multiple?: boolean
  buttonText?: string
  /** button=描边按钮；drag=虚线拖拽区（印章上传同款） */
  variant?: 'button' | 'drag'
  /** 拖拽区主文案 */
  dragTitle?: string
}>(), {
  disabled: false,
  accept: '.doc,.docx,.pdf,.xls,.xlsx,.png,.jpg,.jpeg',
  tip: '',
  parentPath: '/',
  multiple: false,
  buttonText: '',
  variant: 'button',
  dragTitle: '点击或拖拽上传附件',
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | undefined): void
  (e: 'uploaded', meta: PlatformFileUploaded): void
}>()

const { width } = useWindowSize()
const uploading = ref(false)
const uploadControllers = new Set<AbortController>()
let disposed = false
let pendingIds: string[] | undefined
const openingId = ref('')
let openRequestVersion = 0
const invalidateOpen = () => { openRequestVersion++; openingId.value = '' }
const nameMap = reactive<Record<string, string>>({})
const extMap = reactive<Record<string, string>>({})
const filePreviewRef = ref<InstanceType<typeof FilePreview>>()

const imageVisible = ref(false)
const imageTitle = ref('图片预览')
const imageUrl = ref('')
const textVisible = ref(false)
const textTitle = ref('文本预览')
const textContent = ref('')

const idList = computed(() => {
  if (props.modelValue == null || props.modelValue === '') {
    return [] as string[]
  }
  return String(props.modelValue).split(',').map((s) => s.trim()).filter(Boolean)
})

const isDrag = computed(() => props.variant === 'drag')

const dragDesc = computed(() => props.tip || '')

const uploadLabel = computed(() => {
  if (props.buttonText) {
    return props.buttonText
  }
  if (props.multiple) {
    return '上传附件'
  }
  return idList.value.length ? '重新上传' : '上传附件'
})

const displayName = (id: string) => nameMap[id] || `附件 #${id}`

const fileTypeMeta = (id: string) => {
  const name = displayName(id)
  const ext = (extMap[id] || (name.includes('.') ? name.split('.').pop() : '') || '').toLowerCase()
  if (ext === 'pdf') {
    return { letter: 'PDF', tone: 'pdf' }
  }
  if (WordTypes.includes(ext)) {
    return { letter: 'WORD', tone: 'word' }
  }
  if (ExcelTypes.includes(ext)) {
    return { letter: 'EXCEL', tone: 'excel' }
  }
  if (ext === 'ppt' || ext === 'pptx') {
    return { letter: 'PPT', tone: 'ppt' }
  }
  if (ImageTypes.includes(ext)) {
    return { letter: '图片', tone: 'image' }
  }
  return { letter: '文件', tone: 'other' }
}

const emitIds = (ids: string[]) => {
  emit('update:modelValue', ids.length ? ids.join(',') : undefined)
}

const rememberMeta = (id: string, name?: string, extension?: string) => {
  if (name) {
    nameMap[id] = name
  }
  if (extension) {
    extMap[id] = extension.replace(/^\./, '').toLowerCase()
  } else if (name?.includes('.')) {
    extMap[id] = name.split('.').pop()!.toLowerCase()
  }
}

const resolveMeta = async (ids: string[]) => {
  await Promise.all(ids.map(async (id) => {
    if (nameMap[id]) {
      return
    }
    try {
      const { data } = await getFileInfo(id)
      rememberMeta(id, data?.originalName || data?.name, data?.extension)
    } catch {
      // 元数据失败不阻断预览
    }
  }))
}

watch(idList, (ids) => {
  pendingIds = undefined
  if (ids.length) {
    void resolveMeta(ids)
  }
}, { immediate: true })

const closeImagePreview = () => {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
    imageUrl.value = ''
  }
  imageVisible.value = false
}

const previewOffice = async (blob: Blob, fileName: string, ext: string, requestVersion?: number, isCurrent = () => true) => {
  const excelConfig: ExcelConfig = {
    xls: ext === 'xls',
    minColLength: 0,
    minRowLength: 0,
    widthOffset: 10,
    heightOffset: 10,
  }
  const baseName = fileName.includes('.') ? fileName.slice(0, fileName.lastIndexOf('.')) : fileName
  // PDF 用 ArrayBuffer，vue-office 渲染更稳；其它 Office 仍用 Blob
  const data = ext === 'pdf' ? await blob.arrayBuffer() : blob
  if (!isCurrent()) return
  filePreviewRef.value?.onPreview({
    fileInfo: { data, fileName: baseName, fileType: ext },
    excelConfig,
  }, requestVersion)
}

const openFile = async (id: string) => {
  const requestVersion = ++openRequestVersion
  let previewRequest: number | undefined
  openingId.value = id
  const fileName = nameMap[id] || `file-${id}`
  const knownExt = (extMap[id] || fileName.split('.').pop() || '').toLowerCase()
  const baseName = fileName.includes('.') ? fileName.slice(0, fileName.lastIndexOf('.')) : fileName
  const likelyOffice = OfficeTypes.includes(knownExt) || knownExt === 'pdf'
  if (likelyOffice) {
    previewRequest = filePreviewRef.value?.onLoading?.({
      fileName: baseName,
      fileType: knownExt || 'pdf',
    })
  }
  try {
    const res = await downloadFile(id)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    if (requestVersion !== openRequestVersion) return
    const ext = (knownExt || (blob.type === 'application/pdf' ? 'pdf' : '')).toLowerCase()

    if (ImageTypes.includes(ext) || blob.type.startsWith('image/')) {
      filePreviewRef.value?.onClose?.(false)
      closeImagePreview()
      imageTitle.value = fileName
      imageUrl.value = URL.createObjectURL(blob)
      imageVisible.value = true
      return
    }

    if (OfficeTypes.includes(ext) || (blob.type === 'application/pdf' && !ext)) {
      const type = ext || 'pdf'
      if (type === 'pdf' || WordTypes.includes(type) || ExcelTypes.includes(type)) {
        await previewOffice(blob, fileName, type, previewRequest, () => requestVersion === openRequestVersion)
        return
      }
    }

    filePreviewRef.value?.onClose?.(false)

    if (ext === 'txt' || blob.type.startsWith('text/')) {
      textTitle.value = fileName
      const text = await blob.text()
      if (requestVersion !== openRequestVersion) return
      textContent.value = text
      textVisible.value = true
      return
    }

    // 其余类型无法内嵌预览时仍提供下载
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName.includes('.') ? fileName : `${fileName}${ext ? `.${ext}` : ''}`
    link.rel = 'noopener'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
    Message.info('该类型暂不支持预览，已开始下载')
  } catch (e: any) {
    if (requestVersion !== openRequestVersion) return
    filePreviewRef.value?.onClose?.(false)
    Message.error(e?.msg || e?.message || '附件打开失败')
  } finally {
    if (requestVersion === openRequestVersion) openingId.value = ''
  }
}

const handleUpload = (options: RequestOption) => {
  const controller = new AbortController()
  uploadControllers.add(controller)
  ;(async () => {
    const { onError, onSuccess, fileItem } = options
    uploading.value = true
    const formData = new FormData()
    formData.append('parentPath', props.parentPath)
    formData.append('file', fileItem.file as Blob)
    try {
      const res = await uploadCommon(formData, {
        signal: controller.signal,
        onUploadProgress: event => {
          if (!disposed && !controller.signal.aborted) options.onProgress?.(event.total ? Math.min(99, event.loaded / event.total * 100) : 0, event.event)
        },
      })
      if (disposed || controller.signal.aborted) return
      const id = res.data?.id
      if ((typeof id === 'number' && !Number.isSafeInteger(id)) || !/^[1-9][0-9]{0,18}$/.test(String(id ?? ''))) {
        throw new Error('上传响应未返回有效文件 ID')
      }
      const idStr = String(id)
      const raw = fileItem.file as File | undefined
      if (raw?.name) {
        rememberMeta(idStr, raw.name)
      }
      if (props.multiple) {
        const base = pendingIds ?? idList.value
        const next = base.includes(idStr) ? base : [...base, idStr]
        pendingIds = next
        emitIds(next)
      } else {
        emitIds([idStr])
      }
      emit('uploaded', {
        id: idStr,
        url: res.data?.url,
        name: raw?.name,
        size: raw?.size,
      })
      Message.success('上传成功')
      onSuccess(res)
    } catch (e) {
      if (!disposed && !controller.signal.aborted) onError(e)
    } finally {
      uploadControllers.delete(controller)
      uploading.value = uploadControllers.size > 0
    }
  })()
  return {
    abort() {
      controller.abort()
    },
  }
}

const beforeUpload = (file: File) => {
  if (!file.size || file.size > 10 * 1024 * 1024) {
    Message.error('文件不能为空且不能超过10MB'); return false
  }
  const rules = props.accept.split(',').map(value => value.trim().toLowerCase()).filter(Boolean)
  const name = file.name.toLowerCase()
  const type = file.type.toLowerCase()
  if (rules.length && !rules.some(rule => rule.startsWith('.') ? name.endsWith(rule) : rule.endsWith('/*') ? type.startsWith(rule.slice(0, -1)) : type === rule)) {
    Message.error('文件格式不符合当前入口要求'); return false
  }
  return true
}

defineExpose({ isUploading: () => uploading.value })

const removeOne = (id: string) => {
  delete nameMap[id]
  delete extMap[id]
  pendingIds = (pendingIds ?? idList.value).filter((x) => x !== id)
  emitIds(pendingIds)
}

const clear = () => {
  pendingIds = []
  idList.value.forEach((id) => {
    delete nameMap[id]
    delete extMap[id]
  })
  emit('update:modelValue', undefined)
}

onUnmounted(() => {
  disposed = true
  uploadControllers.forEach(controller => controller.abort())
  uploadControllers.clear()
  invalidateOpen()
  filePreviewRef.value?.onClose?.(false)
  closeImagePreview()
})
</script>

<style scoped>
.platform-file-field--drag {
  width: 100%;
}
.platform-file-field__upload-drag {
  display: block;
  width: 100%;
}
.platform-file-field__upload-drag :deep(.arco-upload),
.platform-file-field__upload-drag :deep(.arco-upload-trigger),
.platform-file-field__upload-drag :deep(.arco-upload-list) {
  width: 100%;
}
.platform-file-field__upload-drag :deep(.arco-upload-drag) {
  padding: 0;
  border: none;
  background: transparent;
}
.platform-file-field__upload-drag :deep(.arco-upload-tip),
.platform-file-field__upload-drag :deep(.arco-upload-drag-icon) {
  display: none;
}
.platform-file-field__drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  min-height: 120px;
  padding: 16px 12px;
  border: 1px dashed rgb(var(--primary-3));
  border-radius: 6px;
  background: rgba(var(--primary-1), 0.45);
  text-align: center;
  cursor: pointer;
  box-sizing: border-box;
  transition: border-color 0.2s, background-color 0.2s;
}
.platform-file-field__drop:hover,
.platform-file-field__drop.is-uploading {
  border-color: rgb(var(--primary-6));
  background: rgba(var(--primary-1), 0.7);
}
.platform-file-field__drop.is-filled {
  align-items: stretch;
  justify-content: flex-start;
  gap: 10px;
  min-height: 140px;
  padding: 12px;
  background: var(--color-fill-1);
  border-style: solid;
  border-color: var(--color-border-3);
  cursor: default;
}
.platform-file-field__drop.is-filled:hover {
  border-color: rgb(var(--primary-5));
  background: var(--color-fill-1);
}
.platform-file-field__drop.is-readonly {
  cursor: default;
}
.platform-file-field__badge {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 1px;
  color: var(--color-text-4);
}
.platform-file-field__title {
  font-size: 14px;
  line-height: 1.4;
  color: var(--color-text-1);
}
.platform-file-field__desc {
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-3);
}
.platform-file-field__files {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.platform-file-field__file {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  background: var(--color-bg-2);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.platform-file-field__file:hover {
  border-color: rgb(var(--primary-5));
  box-shadow: 0 0 0 2px rgba(var(--primary-2), 0.45);
}
.platform-file-field__type {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 28px;
  padding: 0 6px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.2px;
  color: #fff;
  background: var(--color-text-3);
}
.platform-file-field__type.is-pdf {
  background: #e34d4d;
}
.platform-file-field__type.is-word {
  background: #2b7de9;
}
.platform-file-field__type.is-excel {
  background: #1f9d55;
}
.platform-file-field__type.is-ppt {
  background: #d9480f;
}
.platform-file-field__type.is-image {
  background: #7c5cbf;
}
.platform-file-field__type.is-other {
  background: #86909c;
}
.platform-file-field__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--color-text-1);
}
.platform-file-field__remove {
  flex: none;
}
.platform-file-field__more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding-top: 2px;
  font-size: 12px;
  color: var(--color-text-3);
  cursor: pointer;
}
.platform-file-field__tip {
  margin-top: 6px;
  color: var(--color-text-3);
  font-size: 12px;
}
.platform-file-field__empty {
  color: var(--color-text-3);
  font-size: 13px;
}
.platform-file-field__img-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 240px;
  max-height: 70vh;
  overflow: auto;
}
.platform-file-field__img-wrap img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
.platform-file-field__text {
  margin: 0;
  max-height: 70vh;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.5;
}
</style>
