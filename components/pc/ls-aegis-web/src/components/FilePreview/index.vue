<template>
  <a-modal
    :visible="visible"
    :width="width >= 1350 ? 1350 : '100%'"
    :footer="false"
    esc-to-close
    @cancel="onClose()"
  >
    <template #title>
      {{ modalTitle }}
      <div class="toolbar">
        <a-tooltip position="tl" content="在新标签页打开">
          <icon-launch style="cursor: pointer" @click="onOpen" />
        </a-tooltip>
      </div>
    </template>
    <div class="preview-wrap">
      <div v-if="loading" class="preview-loading">
        <a-spin :size="40" tip="加载中..." />
      </div>
      <div class="preview-body">
        <a-card v-if="filePreview.fileInfo?.data" :key="previewVersion" class="preview-content">
          <VueOfficePdf
            v-if="filePreview.fileInfo?.fileType === 'pdf'"
            :src="filePreview.fileInfo?.data"
            class="h-full"
            @rendered="rendererEvents.rendered"
            @error="rendererEvents.error"
          />
          <VueOfficeDocx
            v-else-if="WordTypes.includes(filePreview.fileInfo?.fileType || '')"
            :src="filePreview.fileInfo?.data"
            class="h-full"
            @rendered="rendererEvents.rendered"
            @error="rendererEvents.error"
          />
          <VueOfficeExcel
            v-else-if="ExcelTypes.includes(filePreview.fileInfo?.fileType || '')"
            :src="filePreview.fileInfo?.data"
            style="height: 80vh; width: 100%"
            :options="filePreview.excelConfig"
            @rendered="rendererEvents.rendered"
            @error="rendererEvents.error"
          />
        </a-card>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { type AsyncComponentLoader, defineAsyncComponent } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import type { FilePreview } from '@/components/FilePreview/type'
import { ExcelTypes, WordTypes } from '@/constant/file'

// 预览器按文件类型加载；普通列表和表单不应下载三套 Office/PDF 引擎。
// CSS 随对应引擎加载，v-if 只会激活当前文件所需的 loader。
const createPreviewRenderer = (loader: AsyncComponentLoader, types?: string[]) => defineAsyncComponent({
  loader,
  onError: (_error, _retry, fail) => {
    if (!types || types.includes(filePreview.fileInfo?.fileType || '')) errorHandler()
    fail()
  },
})
const VueOfficePdf = createPreviewRenderer(() => import('./renderers/PdfRenderer.vue'), ['pdf'])
const VueOfficeDocx = createPreviewRenderer(() => import('./renderers/DocxRenderer.vue'), WordTypes)
const VueOfficeExcel = createPreviewRenderer(() => import('./renderers/ExcelRenderer.vue'), ExcelTypes)

const emit = defineEmits<{ close: [] }>()
const previewVersion = ref(0)
const rendererEvents = shallowRef({ rendered: () => {}, error: () => {} })
const visible = ref<boolean>(false)
const loading = ref<boolean>(false)
const { width } = useWindowSize()

// 用于关闭弹框时销毁,释放内存
const blobUrl = ref<string>('')
/** 渲染超时兜底，避免个别文件不触发 rendered 一直转圈 */
let loadingTimer: ReturnType<typeof setTimeout> | undefined

// 文件预览对象
const filePreview = reactive<FilePreview>({
  fileInfo: {},
  excelConfig: {},
})
// 弹框标题
const modalTitle = computed(() => {
  const { fileName, fileType } = filePreview.fileInfo || {}
  return fileName && fileType ? `${fileName}.${fileType}` : '文件预览'
})

const clearLoadingTimer = () => {
  if (loadingTimer) {
    clearTimeout(loadingTimer)
    loadingTimer = undefined
  }
}

const startLoading = () => {
  loading.value = true
  clearLoadingTimer()
  // PDF/Office 渲染偶发不回调，60s 后结束转圈避免卡死
  loadingTimer = setTimeout(() => {
    loading.value = false
  }, 60_000)
}

const stopLoading = () => {
  clearLoadingTimer()
  loading.value = false
}

/** 先打开弹窗并转圈，等下载完成后再灌入内容 */
const releaseBlobUrl = () => {
  if (blobUrl.value) URL.revokeObjectURL(blobUrl.value)
  blobUrl.value = ''
}

const onLoading = (meta?: { fileName?: string, fileType?: string }) => {
  previewVersion.value++
  releaseBlobUrl()
  filePreview.fileInfo = {
    fileName: meta?.fileName,
    fileType: meta?.fileType,
  }
  filePreview.excelConfig = {}
  visible.value = true
  startLoading()
  return previewVersion.value
}

// 预览
const onPreview = (previewInfo: FilePreview, requestVersion?: number) => {
  if (requestVersion !== undefined && (requestVersion !== previewVersion.value || !visible.value)) return
  const version = ++previewVersion.value
  releaseBlobUrl()
  rendererEvents.value = {
    rendered: () => { if (version === previewVersion.value) renderedHandler() },
    error: () => { if (version === previewVersion.value) errorHandler() },
  }
  filePreview.fileInfo = previewInfo.fileInfo
  filePreview.excelConfig = previewInfo.excelConfig || {}
  visible.value = true
  startLoading()
}

const renderedHandler = () => {
  stopLoading()
}
const errorHandler = () => {
  stopLoading()
  Message.error('文件加载失败，请关闭预览后重试')
}

// 新标签页打开
const onOpen = () => {
  const data = filePreview.fileInfo?.data

  if (!data) {
    console.error('没有数据提供')
    return
  }

  let url: string | null = null

  if (typeof data === 'string') {
    // 如果是字符串，假设它是一个 URL，直接使用
    url = data
  } else if (data instanceof Blob || data instanceof ArrayBuffer) {
    // 如果之前创建了 Blob URL，先释放
    if (blobUrl.value) {
      URL.revokeObjectURL(blobUrl.value)
    }

    // 如果是 Blob 或 ArrayBuffer，则将其转换为 Blob
    const blob = data instanceof Blob ? data : new Blob([data])
    url = URL.createObjectURL(blob)
    blobUrl.value = url
  } else {
    console.error('不支持的类型')
    return
  }

  // 打开生成的 URL
  window.open(url)
}
// 关闭弹框
const onClose = (notify = true) => {
  previewVersion.value++
  if (notify) emit('close')
  Object.assign(filePreview, {
    fileInfo: {},
    excelConfig: {},
  })
  stopLoading()
  visible.value = false

  // 关闭时释放 Blob URL
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = ''
  }
}

onUnmounted(() => {
  previewVersion.value++
  clearLoadingTimer()
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
  }
})

const isSessionCurrent = (version?: number) => version !== undefined && visible.value && version === previewVersion.value
defineExpose({ onPreview, onLoading, onClose, isSessionCurrent })
</script>

<style scoped lang="scss">
.h-full {
  height: 100vh;
}

.card {
  background: white !important;
  padding-bottom: 5px;
}

.toolbar {
  position: absolute;
  float: right;
  right: 50px;
  top: 10px;
}

.preview-wrap {
  position: relative;
  width: 100%;
  min-height: 70vh;
}

.preview-loading {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 信创浏览器未必支持 color-mix，用固定半透明白底保证转圈可见 */
  background: rgba(255, 255, 255, 0.82);
  min-height: 70vh;
}

.preview-body {
  width: 100%;
  min-height: 70vh;
}
</style>
