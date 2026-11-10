<template>
  <div class="sign-pdf-placer">
    <div class="sign-pdf-placer__toolbar">
      <a-space :size="8">
        <a-button size="mini" :disabled="currentPage <= 1" @click="changePage(-1)">
          <template #icon><icon-left /></template>
        </a-button>
        <a-input-number
          v-model="currentPage"
          :min="1"
          :max="totalPages"
          size="mini"
          hide-button
          style="width: 56px"
          @change="onPageInputChange"
        />
        <span class="page-indicator">/ {{ totalPages }} 页</span>
        <a-button size="mini" :disabled="currentPage >= totalPages" @click="changePage(1)">
          <template #icon><icon-right /></template>
        </a-button>
      </a-space>
      <div v-if="pageStrip.length" class="page-strip">
        <button
          v-for="item in pageStrip"
          :key="item.page"
          type="button"
          class="page-strip__dot"
          :class="{
            active: item.page === currentPage,
            covered: item.covered,
            batch: item.batch,
          }"
          :title="item.title"
          @click="goToPage(item.page)"
        />
      </div>
      <span v-if="placingHint" class="placing-hint">{{ placingHint }}</span>
    </div>

    <div ref="viewportRef" class="sign-pdf-placer__viewport">
      <!-- spin 覆盖层与 page-stage 解耦：loading 期间也可挂载 canvas，避免首屏 render 时 canvasRef 为空 -->
      <a-spin v-if="loading" class="viewport-spin" />
      <div
        v-if="renderReady"
        class="page-stage"
        :style="stageStyle"
      >
        <canvas ref="canvasRef" class="page-canvas" />
        <div
          class="overlay-layer"
          :style="overlayStyle"
          @mousedown.self="onOverlayClick"
        >
          <div
            v-if="placementMode === 'span'"
            class="span-edge-guide"
            :style="{ width: `${ptToPx(pageWidthPt * 0.08)}px` }"
          />

          <div
            v-for="marker in generalMarkers"
            :key="marker.id"
            class="seal-marker seal-marker--general"
            :class="{
              active: marker.id === selectedTaskId,
              'seal-marker--batch': marker.batch,
            }"
            :style="marker.style"
            @mousedown.stop="startDrag($event, marker.id, marker.batch ? 'batch' : 'general')"
            @click.stop="selectTask(marker.id)"
          >
            <div v-if="marker.batch" class="batch-marker__label">批量</div>
            <img v-if="marker.imageUrl" :src="marker.imageUrl" :alt="marker.name" draggable="false" />
            <span v-else class="seal-marker__fallback">{{ marker.name }}</span>
            <button type="button" class="seal-marker__close" @mousedown.stop @click.stop="emit('remove-task', marker.id)">×</button>
            <span
              v-if="marker.id === selectedTaskId"
              class="seal-marker__resize"
              @mousedown.stop="startResize($event, marker.id)"
            />
          </div>

          <div
            v-for="marker in spanMarkers"
            :key="marker.id"
            class="seal-marker seal-marker--span"
            :class="{ active: marker.id === selectedTaskId }"
            :style="marker.style"
            @mousedown.stop="startDrag($event, marker.id, 'span')"
            @click.stop="selectTask(marker.id)"
          >
            <div class="span-marker__label">骑缝</div>
            <img v-if="marker.imageUrl" :src="marker.imageUrl" :alt="marker.name" draggable="false" />
            <button type="button" class="seal-marker__close" @mousedown.stop @click.stop="emit('remove-task', marker.id)">×</button>
          </div>
        </div>
      </div>
      <div v-else-if="!loading" class="viewport-empty">PDF 加载失败</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as pdfjsLib from 'pdfjs-dist'
import pdfjsWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import type { CSSProperties } from 'vue'
import type { PendingSealTask, PdfDocumentMeta, SealTaskType } from '../types'
import { DEFAULT_SEAL_SIZE_PT, DEFAULT_SPAN_SEAL_SIZE_MM, resolveBatchPages } from '../types'

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorker

const props = defineProps<{
  pdfSrc?: ArrayBuffer | string
  tasks: PendingSealTask[]
  selectedTaskId?: string
  sealImageMap: Record<string, string>
  placingHint?: string
  placementMode?: SealTaskType
}>()

const emit = defineEmits<{
  (e: 'update:selectedTaskId', id?: string): void
  (e: 'update:placement', payload: { taskId: string, placement: PendingSealTask['placement'] }): void
  (e: 'update:span-config', payload: { taskId: string, spanConfig: PendingSealTask['spanConfig'] }): void
  (e: 'remove-task', taskId: string): void
  (e: 'document-ready', meta: PdfDocumentMeta): void
  (e: 'load-error', message: string): void
  (e: 'page-change', page: number): void
}>()

const loading = ref(false)
const renderReady = ref(false)
const totalPages = ref(1)
const currentPage = ref(1)
const pageWidthPt = ref(595)
const pageHeightPt = ref(842)
const displayScale = ref(1)
const viewportRef = ref<HTMLElement>()
const canvasRef = ref<HTMLCanvasElement>()

let pdfDoc: pdfjsLib.PDFDocumentProxy | null = null
/** 当前页渲染任务；翻页/缩放/重载时必须先 cancel，否则同一 canvas 并发 render 会抛错 */
let activeRenderTask: pdfjsLib.RenderTask | null = null
let renderSeq = 0
let dragging:
  | { taskId: string, type: 'general' | 'batch' | 'span', startX: number, startY: number, originX: number, originY: number }
  | null = null
let resizing:
  | { taskId: string, startX: number, startY: number, originWidth: number, originHeight: number }
  | null = null

const stageStyle = computed<CSSProperties>(() => ({
  width: `${pageWidthPt.value * displayScale.value}px`,
  height: `${pageHeightPt.value * displayScale.value}px`,
}))

const overlayStyle = computed<CSSProperties>(() => ({
  width: `${pageWidthPt.value * displayScale.value}px`,
  height: `${pageHeightPt.value * displayScale.value}px`,
}))

const ptToPx = (value: number) => value * displayScale.value
const pxToPt = (value: number) => value / displayScale.value
const mmToPt = (mm: number) => mm * 72 / 25.4

const isGeneralLikeTask = (task: PendingSealTask) => task.type === 'general' || task.type === 'batch'

const isTaskVisibleOnPage = (task: PendingSealTask, page: number) => {
  if (!task.placement) {
    return false
  }
  if (task.type === 'batch' && task.batchPages) {
    return resolveBatchPages(task.batchPages, totalPages.value).includes(page)
  }
  if (task.type === 'general') {
    return task.placement.page === page
  }
  return false
}

const selectedBatchTask = computed(() => {
  if (!props.selectedTaskId) {
    return undefined
  }
  const task = props.tasks.find((item) => item.id === props.selectedTaskId)
  return task?.type === 'batch' ? task : undefined
})

const pageStrip = computed(() => {
  if (totalPages.value <= 1 || totalPages.value > 40) {
    return [] as Array<{ page: number, covered: boolean, batch: boolean, title: string }>
  }
  const batchPages = selectedBatchTask.value?.batchPages
  const coveredSet = new Set(batchPages ? resolveBatchPages(batchPages, totalPages.value) : [])
  return Array.from({ length: totalPages.value }, (_, index) => {
    const page = index + 1
    const covered = coveredSet.has(page)
    return {
      page,
      covered,
      batch: Boolean(batchPages),
      title: covered ? `第 ${page} 页（批量覆盖）` : `第 ${page} 页`,
    }
  })
})

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    emit('page-change', page)
    renderPage()
  }
}

const generalMarkers = computed(() => props.tasks
  .filter((task) => isTaskVisibleOnPage(task, currentPage.value))
  .map((task) => {
    const placement = task.placement!
    return {
      id: task.id,
      name: task.sealName,
      batch: task.type === 'batch',
      imageUrl: props.sealImageMap[String(task.sealId)],
      style: {
        left: `${ptToPx(placement.x)}px`,
        top: `${ptToPx(placement.y)}px`,
        width: `${ptToPx(placement.width)}px`,
        height: `${ptToPx(placement.height)}px`,
      } as CSSProperties,
    }
  }))

const spanMarkers = computed(() => props.tasks
  .filter((task) => {
    if (task.type !== 'span' || !task.spanConfig) {
      return false
    }
    const { startPage, endPage } = task.spanConfig
    return currentPage.value >= startPage && currentPage.value <= endPage
  })
  .map((task) => {
    const config = task.spanConfig!
    const sealSizePt = mmToPt(config.sealSize || DEFAULT_SPAN_SEAL_SIZE_MM)
    const markerWidth = ptToPx(Math.min(sealSizePt, pageWidthPt.value * 0.08))
    const markerHeight = ptToPx(sealSizePt)
    const centerY = config.yPosition > 0 ? config.yPosition : pageHeightPt.value / 2
    const top = ptToPx(centerY) - markerHeight / 2
    return {
      id: task.id,
      name: task.sealName,
      imageUrl: props.sealImageMap[String(task.sealId)],
      style: {
        left: `${ptToPx(pageWidthPt.value) - markerWidth}px`,
        top: `${Math.max(0, top)}px`,
        width: `${markerWidth}px`,
        height: `${markerHeight}px`,
      } as CSSProperties,
    }
  }))

const selectTask = (taskId: string) => {
  emit('update:selectedTaskId', taskId)
}

const clearSelection = () => {
  emit('update:selectedTaskId', undefined)
}

const getOverlayPoint = (event: MouseEvent) => {
  const overlay = event.currentTarget as HTMLElement
  const rect = overlay.getBoundingClientRect()
  return {
    x: pxToPt(event.clientX - rect.left),
    y: pxToPt(event.clientY - rect.top),
  }
}

const onOverlayClick = (event: MouseEvent) => {
  if (!props.selectedTaskId) {
    clearSelection()
    return
  }
  const task = props.tasks.find((item) => item.id === props.selectedTaskId)
  if (!task) {
    return
  }
  const point = getOverlayPoint(event)
  if (task.type === 'general' && task.placement) {
    const width = task.placement.width
    const height = task.placement.height
    emit('update:placement', {
      taskId: task.id,
      placement: {
        ...task.placement,
        page: currentPage.value,
        x: Math.max(0, Math.min(pageWidthPt.value - width, point.x - width / 2)),
        y: Math.max(0, Math.min(pageHeightPt.value - height, point.y - height / 2)),
      },
    })
  } else if (task.type === 'batch' && task.placement) {
    const width = task.placement.width
    const height = task.placement.height
    emit('update:placement', {
      taskId: task.id,
      placement: {
        ...task.placement,
        x: Math.max(0, Math.min(pageWidthPt.value - width, point.x - width / 2)),
        y: Math.max(0, Math.min(pageHeightPt.value - height, point.y - height / 2)),
      },
    })
  } else if (task.type === 'span' && task.spanConfig) {
    const sealSizePt = mmToPt(task.spanConfig.sealSize || DEFAULT_SPAN_SEAL_SIZE_MM)
    const minY = sealSizePt / 2
    const maxY = pageHeightPt.value - sealSizePt / 2
    emit('update:span-config', {
      taskId: task.id,
      spanConfig: {
        ...task.spanConfig,
        yPosition: Math.max(minY, Math.min(maxY, point.y)),
      },
    })
  }
}

const changePage = (delta: number) => {
  const next = currentPage.value + delta
  if (next < 1 || next > totalPages.value) {
    return
  }
  currentPage.value = next
  emit('page-change', next)
  renderPage()
}

const onPageInputChange = (value: number | undefined) => {
  if (!value || value < 1 || value > totalPages.value) {
    return
  }
  currentPage.value = value
  emit('page-change', value)
  renderPage()
}

const calcDisplayScale = () => {
  const viewport = viewportRef.value
  if (!viewport) {
    displayScale.value = 1
    return
  }
  // 按宽度铺满预览区，文字更清晰；整页可纵向滚动，不再被高度压小
  const padding = 24
  const maxWidth = Math.max(viewport.clientWidth - padding, 280)
  displayScale.value = Math.min(maxWidth / pageWidthPt.value, 2)
}

const waitFrames = (count = 2) => new Promise<void>((resolve) => {
  const step = (left: number) => {
    if (left <= 0) {
      resolve()
      return
    }
    requestAnimationFrame(() => step(left - 1))
  }
  step(count)
})

const waitForCanvas = async (tries = 20) => {
  for (let i = 0; i < tries; i++) {
    if (canvasRef.value?.getContext('2d')) {
      return canvasRef.value
    }
    await nextTick()
    await waitFrames(1)
  }
  return canvasRef.value
}

const cancelActiveRender = async () => {
  const task = activeRenderTask
  if (!task) {
    return
  }
  activeRenderTask = null
  try {
    task.cancel()
  } catch {
    // ignore
  }
  try {
    await task.promise
  } catch {
    // RenderingCancelledException 等，忽略
  }
}

const destroyDocument = async () => {
  renderSeq += 1
  await cancelActiveRender()
  if (!pdfDoc) {
    return
  }
  try {
    await pdfDoc.destroy()
  } catch {
    // ignore
  }
  pdfDoc = null
}

const loadDocument = async () => {
  if (!props.pdfSrc) {
    await destroyDocument()
    renderReady.value = false
    return
  }
  loading.value = true
  renderReady.value = false
  await destroyDocument()
  try {
    // slice 拷贝，避免 pdf.js transfer 后 ArrayBuffer 被 detach
    const data = props.pdfSrc instanceof ArrayBuffer
      ? new Uint8Array(props.pdfSrc.slice(0))
      : props.pdfSrc
    pdfDoc = await pdfjsLib.getDocument({ data }).promise
    totalPages.value = pdfDoc.numPages
    currentPage.value = 1

    const pages = await Promise.all(Array.from({ length: totalPages.value }, async (_, index) => {
      const page = await pdfDoc!.getPage(index + 1)
      const viewport = page.getViewport({ scale: 1 })
      return {
        page: index + 1,
        width: viewport.width,
        height: viewport.height,
      }
    }))
    pageWidthPt.value = pages[0]?.width || 595
    pageHeightPt.value = pages[0]?.height || 842
    // 先挂载 canvas；再关掉 spin，避免「遮罩下绘完 → 揭开白屏、点击才出字」
    renderReady.value = true
    await nextTick()
    loading.value = false
    emit('document-ready', { totalPages: totalPages.value, pages })
    await nextTick()
    await waitFrames(2)
    calcDisplayScale()
    const canvas = await waitForCanvas()
    if (!canvas) {
      renderReady.value = false
      emit('load-error', 'PDF 画布未就绪')
      return
    }
    await renderPage()
    // 再刷一帧，规避合成层未刷新
    await waitFrames(1)
    await renderPage()
  } catch (error: any) {
    renderReady.value = false
    emit('load-error', error?.message || 'PDF 加载失败')
  } finally {
    loading.value = false
  }
}

const renderPage = async () => {
  if (!pdfDoc || !canvasRef.value) {
    return
  }
  const seq = ++renderSeq
  await cancelActiveRender()
  if (seq !== renderSeq || !pdfDoc || !canvasRef.value) {
    return
  }

  const page = await pdfDoc.getPage(currentPage.value)
  if (seq !== renderSeq || !canvasRef.value) {
    return
  }

  const baseViewport = page.getViewport({ scale: 1 })
  pageWidthPt.value = baseViewport.width
  pageHeightPt.value = baseViewport.height
  const viewport = page.getViewport({ scale: displayScale.value })

  const canvas = canvasRef.value
  const context = canvas.getContext('2d', { alpha: false })
  if (!context) {
    return
  }
  canvas.width = Math.max(1, Math.floor(viewport.width))
  canvas.height = Math.max(1, Math.floor(viewport.height))
  // 先铺白底，避免透明缓冲在部分浏览器上呈现为「空白页」
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  const renderTask = page.render({ canvasContext: context, viewport })
  activeRenderTask = renderTask
  try {
    await renderTask.promise
  } catch (error: any) {
    // 被后续 render / destroy 取消属正常，不向上抛
    if (error?.name === 'RenderingCancelledException' || /cancel/i.test(String(error?.message || ''))) {
      return
    }
    throw error
  } finally {
    if (activeRenderTask === renderTask) {
      activeRenderTask = null
    }
  }
}

const startDrag = (event: MouseEvent, taskId: string, type: 'general' | 'batch' | 'span') => {
  const task = props.tasks.find((item) => item.id === taskId)
  if (!task) {
    return
  }
  selectTask(taskId)
  if ((type === 'general' || type === 'batch') && task.placement) {
    dragging = {
      taskId,
      type,
      startX: event.clientX,
      startY: event.clientY,
      originX: task.placement.x,
      originY: task.placement.y,
    }
  } else if (type === 'span' && task.spanConfig) {
    dragging = {
      taskId,
      type,
      startX: event.clientX,
      startY: event.clientY,
      originX: 0,
      originY: task.spanConfig.yPosition,
    }
  }
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

const startResize = (event: MouseEvent, taskId: string) => {
  const task = props.tasks.find((item) => item.id === taskId)
  if (!task?.placement) {
    return
  }
  selectTask(taskId)
  resizing = {
    taskId,
    startX: event.clientX,
    startY: event.clientY,
    originWidth: task.placement.width,
    originHeight: task.placement.height,
  }
  window.addEventListener('mousemove', onResizeMove)
  window.addEventListener('mouseup', onResizeEnd)
}

const onDragMove = (event: MouseEvent) => {
  if (!dragging) {
    return
  }
  const deltaX = pxToPt(event.clientX - dragging.startX)
  const deltaY = pxToPt(event.clientY - dragging.startY)
  const task = props.tasks.find((item) => item.id === dragging!.taskId)
  if (!task) {
    return
  }
  if ((dragging.type === 'general' || dragging.type === 'batch') && task.placement) {
    const width = task.placement.width
    const height = task.placement.height
    const maxX = pageWidthPt.value - width
    const maxY = pageHeightPt.value - height
    const nextPlacement = {
      ...task.placement,
      x: Math.max(0, Math.min(maxX, dragging.originX + deltaX)),
      y: Math.max(0, Math.min(maxY, dragging.originY + deltaY)),
    }
    if (dragging.type === 'general') {
      nextPlacement.page = currentPage.value
    }
    emit('update:placement', {
      taskId: task.id,
      placement: nextPlacement,
    })
  } else if (dragging.type === 'span' && task.spanConfig) {
    const sealSizePt = mmToPt(task.spanConfig.sealSize || DEFAULT_SPAN_SEAL_SIZE_MM)
    const minY = sealSizePt / 2
    const maxY = pageHeightPt.value - sealSizePt / 2
    emit('update:span-config', {
      taskId: task.id,
      spanConfig: {
        ...task.spanConfig,
        yPosition: Math.max(minY, Math.min(maxY, dragging.originY + deltaY)),
      },
    })
  }
}

const onResizeMove = (event: MouseEvent) => {
  if (!resizing) {
    return
  }
  const task = props.tasks.find((item) => item.id === resizing!.taskId)
  if (!task?.placement) {
    return
  }
  const delta = Math.max(pxToPt(event.clientX - resizing.startX), pxToPt(event.clientY - resizing.startY))
  const nextSize = Math.max(60, Math.min(240, Math.round(resizing.originWidth + delta)))
  const maxX = pageWidthPt.value - nextSize
  const maxY = pageHeightPt.value - nextSize
  emit('update:placement', {
    taskId: task.id,
    placement: {
      ...task.placement,
      x: Math.min(task.placement.x, maxX),
      y: Math.min(task.placement.y, maxY),
      width: nextSize,
      height: nextSize,
    },
  })
}

const onDragEnd = () => {
  dragging = null
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

const onResizeEnd = () => {
  resizing = null
  window.removeEventListener('mousemove', onResizeMove)
  window.removeEventListener('mouseup', onResizeEnd)
}

const handleResize = () => {
  calcDisplayScale()
  renderPage()
}

const syncTaskPage = (taskId?: string) => {
  if (!taskId) {
    return
  }
  const task = props.tasks.find((item) => item.id === taskId)
  if (!task) {
    return
  }
  let targetPage: number | undefined
  if (task.type === 'general') {
    targetPage = task.placement?.page
  } else if (task.type === 'batch') {
    targetPage = task.batchPages?.startPage
  } else {
    targetPage = task.spanConfig?.startPage
  }
  if (targetPage && targetPage !== currentPage.value) {
    currentPage.value = targetPage
    emit('page-change', targetPage)
    renderPage()
  }
}

watch(() => props.pdfSrc, () => {
  loadDocument()
}, { immediate: true })

watch(() => props.selectedTaskId, (taskId) => {
  syncTaskPage(taskId)
})

watch(() => props.tasks.map((task) => {
  if (task.type === 'batch') {
    return `${task.id}:${task.batchPages?.startPage}-${task.batchPages?.endPage}`
  }
  if (task.type === 'general') {
    return `${task.id}:${task.placement?.page}`
  }
  return `${task.id}:${task.spanConfig?.startPage}`
}).join('|'), () => {
  syncTaskPage(props.selectedTaskId)
})

let viewportResizeObserver: ResizeObserver | null = null

onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (typeof ResizeObserver === 'undefined') {
    return
  }
  nextTick(() => {
    if (!viewportRef.value) {
      return
    }
    viewportResizeObserver = new ResizeObserver(() => {
      if (!pdfDoc || !renderReady.value || loading.value) {
        return
      }
      handleResize()
    })
    viewportResizeObserver.observe(viewportRef.value)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  window.removeEventListener('mousemove', onResizeMove)
  window.removeEventListener('mouseup', onResizeEnd)
  viewportResizeObserver?.disconnect()
  viewportResizeObserver = null
  destroyDocument()
})

defineExpose({
  currentPage,
  totalPages,
  pageWidthPt,
  pageHeightPt,
  goToPage,
})
</script>

<style scoped lang="scss">
.sign-pdf-placer {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.sign-pdf-placer__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border-2);
  background: var(--color-bg-1);
  flex-wrap: wrap;
}

.page-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 360px;
}

.page-strip__dot {
  width: 10px;
  height: 10px;
  padding: 0;
  border: 1px solid var(--color-border-3);
  border-radius: 2px;
  background: var(--color-fill-2);
  cursor: pointer;

  &.active {
    border-color: rgb(var(--primary-6));
    background: rgb(var(--primary-6));
  }

  &.covered {
    border-color: #722ed1;
    background: rgba(114, 46, 209, 0.35);
  }

  &.covered.active {
    background: #722ed1;
  }
}

.page-indicator {
  font-size: 13px;
  color: var(--color-text-2);
}

.placing-hint {
  font-size: 12px;
  color: rgb(var(--primary-6));
}

.sign-pdf-placer__viewport {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 12px;
  background: var(--color-fill-2);
  position: relative;
}

.viewport-spin,
.viewport-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
}

.page-stage {
  position: relative;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  background: #fff;
}

.page-canvas {
  display: block;
}

.overlay-layer {
  position: absolute;
  inset: 0;
  pointer-events: auto;
  cursor: crosshair;
}

.span-edge-guide {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(var(--warning-6), 0.06);
  border-left: 1px dashed rgba(var(--warning-6), 0.45);
  pointer-events: none;
}

.seal-marker {
  position: absolute;
  border: 2px dashed rgb(var(--primary-5));
  background: rgba(var(--primary-6), 0.08);
  cursor: move;
  user-select: none;
  box-sizing: border-box;
  overflow: hidden;

  &.active {
    border-style: solid;
    border-color: rgb(var(--primary-6));
    box-shadow: 0 0 0 2px rgba(var(--primary-6), 0.18);
    z-index: 2;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    pointer-events: none;
    opacity: 0.85;
  }

  &__fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    font-size: 11px;
    color: var(--color-text-2);
    padding: 4px;
    text-align: center;
  }

  &__close {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 18px;
    height: 18px;
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.45);
    color: #fff;
    line-height: 16px;
    font-size: 14px;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.15s ease;
  }

  &__resize {
    position: absolute;
    right: -1px;
    bottom: -1px;
    width: 12px;
    height: 12px;
    border: 2px solid rgb(var(--primary-6));
    border-radius: 2px;
    background: #fff;
    cursor: nwse-resize;
  }

  &:hover &__close,
  &.active &__close {
    opacity: 1;
  }
}

.seal-marker--batch {
  border-color: #722ed1;
  background: rgba(114, 46, 209, 0.08);

  &.active {
    border-color: #722ed1;
    box-shadow: 0 0 0 2px rgba(114, 46, 209, 0.18);
  }
}

.batch-marker__label {
  position: absolute;
  top: 2px;
  left: 2px;
  padding: 0 4px;
  font-size: 10px;
  line-height: 16px;
  border-radius: 2px;
  color: #fff;
  background: #722ed1;
  z-index: 1;
}

.seal-marker--span {
  border-color: rgb(var(--warning-6));
  background: rgba(var(--warning-6), 0.08);
  cursor: ns-resize;

  &.active {
    border-color: rgb(var(--warning-6));
    box-shadow: 0 0 0 2px rgba(var(--warning-6), 0.18);
  }
}

.span-marker__label {
  position: absolute;
  top: 2px;
  left: 2px;
  padding: 0 4px;
  font-size: 10px;
  line-height: 16px;
  border-radius: 2px;
  color: #fff;
  background: rgb(var(--warning-6));
  z-index: 1;
}
</style>
