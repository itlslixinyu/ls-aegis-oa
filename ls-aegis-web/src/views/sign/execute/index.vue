<template>
  <GiPageLayout :default-collapsed="false" :padding="false">
    <template #left>
      <div class="sign-sidebar">
        <section class="sidebar-block">
          <div class="sidebar-block__title">签章流程</div>
          <div class="sign-steps">
            <div
              v-for="(step, index) in flowSteps"
              :key="step.key"
              class="sign-step"
              :class="{ active: currentStep === index + 1, done: currentStep > index + 1 }"
            >
              <div class="sign-step__index">{{ index + 1 }}</div>
              <div class="sign-step__body">
                <div class="sign-step__label">{{ step.label }}</div>
                <div class="sign-step__desc">{{ step.desc }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="sidebar-block">
          <div class="sidebar-block__title">选择项目</div>
          <a-select
            v-model="form.projectId"
            placeholder="请选择签章项目"
            allow-search
            allow-clear
            :options="projectOptions"
            @change="onProjectChange"
          />
        </section>

        <section v-if="form.projectId" class="sidebar-block">
          <div class="sidebar-block__title">签章文件</div>
          <a-select
            v-model="form.fileId"
            placeholder="请选择项目绑定的 PDF"
            allow-clear
            allow-search
            :loading="fileLoading"
            :options="fileOptions"
            :disabled="fileLoading || !fileOptions.length"
            @change="onFileChange"
          />
          <div v-if="!fileLoading && !fileOptions.length" class="mode-tip">
            当前项目未绑定可用的 PDF，请先在项目管理中上传或关联合同附件
          </div>
        </section>

        <section class="sidebar-block">
          <div class="sidebar-block__title">放置模式</div>
          <a-radio-group v-model="placementMode" type="button" size="small" :disabled="!form.fileId">
            <a-radio value="general">普通章</a-radio>
            <a-radio value="batch">批量章</a-radio>
            <a-radio value="span">骑缝章</a-radio>
          </a-radio-group>
          <div class="mode-tip">{{ placementModeTip }}</div>
        </section>

        <section v-if="placementMode === 'batch' && form.fileId" class="sidebar-block">
          <div class="sidebar-block__title">批量页码</div>
          <a-select
            v-model="batchDefaults.mode"
            size="small"
            :options="batchModeOptions"
            @change="onBatchModeChange"
          />
          <a-space v-if="batchDefaults.mode === 'range'" wrap :size="8" style="margin-top: 8px">
            <a-input-number v-model="batchDefaults.startPage" :min="1" :max="pdfMeta.totalPages" size="small" style="width: 88px">
              <template #prefix>起</template>
            </a-input-number>
            <a-input-number v-model="batchDefaults.endPage" :min="batchDefaults.startPage" :max="pdfMeta.totalPages" size="small" style="width: 88px">
              <template #prefix>止</template>
            </a-input-number>
          </a-space>
          <a-space v-else wrap :size="8" style="margin-top: 8px">
            <a-input-number v-model="batchDefaults.startPage" :min="1" :max="pdfMeta.totalPages" size="small" style="width: 88px">
              <template #prefix>起</template>
            </a-input-number>
            <a-input-number v-model="batchDefaults.endPage" :min="batchDefaults.startPage" :max="pdfMeta.totalPages" size="small" style="width: 88px">
              <template #prefix>止</template>
            </a-input-number>
          </a-space>
          <div class="batch-preview">
            将覆盖 <strong>{{ pendingBatchPageCount }}</strong> 页
            <span v-if="batchDefaults.mode !== 'all'">（{{ batchModeLabel(batchDefaults.mode) }}）</span>
          </div>
          <a-space wrap :size="8" class="batch-presets">
            <a-button size="mini" @click="applyBatchPreset('all')">全部页</a-button>
            <a-button size="mini" @click="applyBatchPreset('odd')">奇数页</a-button>
            <a-button size="mini" @click="applyBatchPreset('even')">偶数页</a-button>
            <a-button size="mini" @click="applyBatchPreset('fromCurrent')">当前页起</a-button>
          </a-space>
        </section>

        <section class="sidebar-block sidebar-block--grow">
          <div class="sidebar-block__title">
            <span>可用印章</span>
            <span class="sidebar-block__count">{{ filteredSeals.length }} 个</span>
          </div>
          <a-input-search
            v-model="sealKeyword"
            placeholder="搜索印章"
            allow-clear
            :disabled="!form.projectId"
          />
          <div class="seal-list">
            <template v-if="!form.projectId">
              <div class="empty-box">
                <icon-info-circle :size="28" />
                <span>请先选择签章项目</span>
              </div>
            </template>
            <template v-else-if="sealLoading">
              <div class="empty-box">
                <a-spin />
              </div>
            </template>
            <template v-else-if="filteredSeals.length">
              <div
                v-for="seal in filteredSeals"
                :key="seal.sealId"
                class="seal-item"
                :class="{ disabled: !form.fileId }"
                @click="addSealTask(seal)"
              >
                <div class="seal-item__thumb">
                  <img v-if="sealImageMap[String(seal.sealId)]" :src="sealImageMap[String(seal.sealId)]" :alt="seal.sealName" />
                  <icon-stamp v-else :size="22" />
                </div>
                <div class="seal-item__info">
                  <div class="seal-item__name">{{ seal.sealName }}</div>
                  <div class="seal-item__meta">{{ seal.categoryLabel || '印章' }}</div>
                </div>
                <icon-plus-circle class="seal-item__add" />
              </div>
            </template>
            <template v-else>
              <div class="empty-box">
                <icon-file :size="28" />
                <span>暂无可用印章</span>
              </div>
            </template>
          </div>
        </section>
      </div>
    </template>

    <div class="sign-workspace">
      <div class="workspace-header">
        <div class="workspace-title">
          <span class="workspace-title__bar" />
          <span>发起签章</span>
        </div>
        <a-button @click="resetAll">
          <template #icon><icon-plus /></template>
          新建签章
        </a-button>
      </div>

      <div class="workspace-main">
        <section class="preview-panel">
          <div class="panel-head">
            <span>PDF 预览</span>
            <a-space v-if="form.fileId" :size="8">
              <span class="file-name">{{ selectedFileName }}</span>
              <a-link status="danger" @click="clearFile">清除</a-link>
            </a-space>
          </div>
          <div class="preview-body">
            <!-- 仅下载阶段盖遮罩；已挂载 SignPdfPlacer 后由其内部 loading 负责，避免 canvas 在遮罩下绘制后揭开白屏 -->
            <div v-if="pdfLoading && !pdfSrc" class="preview-spin-mask">
              <a-spin tip="PDF 加载中..." />
            </div>
            <SignPdfPlacer
              v-if="pdfSrc"
              ref="signPdfPlacerRef"
              :pdf-src="pdfSrc"
              :tasks="pendingTasks"
              :selected-task-id="selectedTaskId"
              :seal-image-map="sealImageMap"
              :placing-hint="placingHint"
              :placement-mode="placementMode"
              @update:selected-task-id="selectedTaskId = $event"
              @update:placement="onUpdatePlacement"
              @update:span-config="onUpdateSpanConfig"
              @remove-task="removeTask"
              @document-ready="onDocumentReady"
              @load-error="onPdfLoadError"
            />
            <div v-else-if="!pdfLoading" class="empty-preview">
              <icon-file :size="42" />
              <div class="empty-preview__title">{{ emptyPreviewTitle }}</div>
              <div class="empty-preview__sub">{{ emptyPreviewSub }}</div>
            </div>
          </div>
        </section>

        <section class="tasks-panel">
          <div class="panel-head">
            <span>待签章任务 <em>{{ pendingTasks.length }}</em></span>
            <a-link v-if="pendingTasks.length" status="danger" @click="clearTasks">清空全部</a-link>
          </div>
          <div class="tasks-body">
            <template v-if="pendingTasks.length">
              <div
                v-for="task in pendingTasks"
                :key="task.id"
                class="task-item"
                :class="{ active: selectedTaskId === task.id }"
                @click="selectedTaskId = task.id"
              >
                <div class="task-item__thumb">
                  <img v-if="sealImageMap[String(task.sealId)]" :src="sealImageMap[String(task.sealId)]" :alt="task.sealName" />
                  <icon-stamp v-else :size="18" />
                </div>
                <div class="task-item__info">
                  <div class="task-item__head">
                    <span class="task-item__name">{{ task.sealName }}</span>
                    <a-tag size="small" :color="taskTagColor(task)">
                      {{ taskTagLabel(task) }}
                    </a-tag>
                  </div>
                  <div v-if="task.type === 'general' && task.placement" class="task-item__meta">
                    第 {{ task.placement.page }} 页 · X {{ Math.round(task.placement.x) }} / Y {{ Math.round(task.placement.y) }}
                  </div>
                  <div v-else-if="task.type === 'batch' && task.placement && task.batchPages" class="task-item__meta">
                    {{ batchTaskSummary(task) }}
                  </div>
                  <div v-else-if="task.type === 'span' && task.spanConfig" class="task-item__meta">
                    第 {{ task.spanConfig.startPage }}-{{ task.spanConfig.endPage }} 页 · 尺寸 {{ task.spanConfig.sealSize }}mm
                  </div>
                  <div v-if="selectedTaskId === task.id" class="task-config" @click.stop>
                    <template v-if="task.type === 'general' && task.placement">
                      <a-space wrap :size="8">
                        <a-input-number
                          v-model="task.placement.page"
                          :min="1"
                          :max="pdfMeta.totalPages"
                          size="mini"
                          style="width: 92px"
                          @change="focusTaskPage(task)"
                        >
                          <template #prefix>页</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.placement.width"
                          :min="60"
                          :max="240"
                          size="mini"
                          style="width: 96px"
                        >
                          <template #prefix>宽</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.placement.height"
                          :min="60"
                          :max="240"
                          size="mini"
                          style="width: 96px"
                        >
                          <template #prefix>高</template>
                        </a-input-number>
                      </a-space>
                      <div class="task-config__actions">
                        <a-link @click="convertToBatch(task)">扩展为批量</a-link>
                      </div>
                    </template>
                    <template v-else-if="task.type === 'batch' && task.placement && task.batchPages">
                      <a-select
                        v-model="task.batchPages.mode"
                        size="mini"
                        :options="batchModeOptions"
                        style="width: 100%; margin-bottom: 8px"
                        @change="onBatchRangeChange(task)"
                      />
                      <a-space wrap :size="8">
                        <a-input-number
                          v-model="task.batchPages.startPage"
                          :min="1"
                          :max="pdfMeta.totalPages"
                          size="mini"
                          style="width: 92px"
                          :disabled="task.batchPages.mode === 'all'"
                          @change="onBatchRangeChange(task)"
                        >
                          <template #prefix>起</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.batchPages.endPage"
                          :min="task.batchPages.startPage"
                          :max="pdfMeta.totalPages"
                          size="mini"
                          style="width: 92px"
                          :disabled="task.batchPages.mode === 'all'"
                          @change="onBatchRangeChange(task)"
                        >
                          <template #prefix>止</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.placement.width"
                          :min="60"
                          :max="240"
                          size="mini"
                          style="width: 96px"
                        >
                          <template #prefix>宽</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.placement.height"
                          :min="60"
                          :max="240"
                          size="mini"
                          style="width: 96px"
                        >
                          <template #prefix>高</template>
                        </a-input-number>
                      </a-space>
                      <div class="task-config__actions">
                        <a-link @click="applyBatchAllPages(task)">设为全部页</a-link>
                        <a-link @click="setTaskBatchMode(task, 'odd')">奇数页</a-link>
                        <a-link @click="setTaskBatchMode(task, 'even')">偶数页</a-link>
                        <a-link @click="convertToSingle(task)">改为单页</a-link>
                      </div>
                      <div class="task-config__hint">
                        共 {{ countBatchPages(task.batchPages, pdfMeta.totalPages) }} 页同位置 · 翻页可预览各页效果
                      </div>
                    </template>
                    <template v-else-if="task.type === 'span' && task.spanConfig">
                      <a-space wrap :size="8">
                        <a-input-number
                          v-model="task.spanConfig.startPage"
                          :min="1"
                          :max="pdfMeta.totalPages"
                          size="mini"
                          style="width: 92px"
                          @change="focusTaskPage(task)"
                        >
                          <template #prefix>起</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.spanConfig.endPage"
                          :min="task.spanConfig.startPage"
                          :max="pdfMeta.totalPages"
                          size="mini"
                          style="width: 92px"
                          @change="focusTaskPage(task)"
                        >
                          <template #prefix>止</template>
                        </a-input-number>
                        <a-input-number
                          v-model="task.spanConfig.sealSize"
                          :min="20"
                          :max="80"
                          size="mini"
                          style="width: 108px"
                        >
                          <template #prefix>mm</template>
                        </a-input-number>
                      </a-space>
                      <div class="task-config__hint">Y {{ Math.round(task.spanConfig.yPosition) }} pt · 可在 PDF 右侧上下拖动</div>
                    </template>
                  </div>
                </div>
                <a-link status="danger" @click.stop="removeTask(task.id)">移除</a-link>
              </div>
            </template>
            <div v-else class="tasks-empty">
              <icon-drag-dot-vertical :size="22" />
              <span>在左侧选择印章，并在 PDF 上拖动定位</span>
            </div>
          </div>
        </section>
      </div>

      <div class="workspace-footer">
        <span class="footer-tip">{{ footerTip }}</span>
        <a-space>
          <a-button @click="resetAll">重置</a-button>
          <a-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">
            <template #icon><icon-check /></template>
            统一签章
          </a-button>
        </a-space>
      </div>
    </div>
  </GiPageLayout>
</template>

<script setup lang="ts">
import { Message, Spin } from '@arco-design/web-vue'
import { defineAsyncComponent } from 'vue'

// 未选择PDF前不加载定位画布及PDF引擎，加载失败沿用原有文件错误处理。
const SignPdfPlacer = defineAsyncComponent({
  loader: () => import('./components/SignPdfPlacer.vue'),
  loadingComponent: Spin,
  onError: (_error, _retry, fail) => {
    if (pdfSrc.value) onPdfLoadError('签章预览加载失败，请重新选择文件重试')
    fail()
  },
})
import {
  DEFAULT_SEAL_SIZE_PT,
  DEFAULT_SPAN_SEAL_SIZE_MM,
  batchModeLabel,
  countBatchPages,
  expandBatchPlacements,
  type BatchPageMode,
  type BatchPageRange,
  type PdfDocumentMeta,
  type PendingSealTask,
  type SealPlacement,
  type SealTaskType,
  type SpanSealTaskConfig,
} from './types'
import {
  type ProjectResp,
  type SealResp,
  availableSeals,
  fetchSignImageObjectUrl,
  getProject,
  getSealImageResource,
  getSignableProjects,
  unifiedSign,
} from '@/apis/sign'
import { getContract } from '@/apis/eoms/contract'
import { getContractPurchase, getContractSale } from '@/apis/psi'
import { downloadFile, getFileInfo } from '@/apis/system/common'

defineOptions({ name: 'SignExecute' })

const flowSteps = [
  { key: 'file', label: '选择文件', desc: '从项目绑定文件中选择' },
  { key: 'place', label: '放置印章', desc: '拖放定位 / 骑缝配置' },
  { key: 'confirm', label: '确认提交', desc: '统一签章输出' },
]

const form = reactive({
  projectId: undefined as string | number | undefined,
  fileId: undefined as string | undefined,
})

const projectOptions = ref<{ label: string, value: string | number }[]>([])
const fileOptions = ref<{ label: string, value: string }[]>([])
const fileLoading = ref(false)
const sealList = ref<SealResp[]>([])
const sealKeyword = ref('')
const sealLoading = ref(false)
const sealImageMap = reactive<Record<string, string>>({})
const pendingTasks = ref<PendingSealTask[]>([])
const selectedTaskId = ref<string>()
const placementMode = ref<SealTaskType>('general')
const submitting = ref(false)
const selectedFileName = ref('')
const pdfSrc = ref<ArrayBuffer | string>()
const pdfLoading = ref(false)
const pdfMeta = reactive<PdfDocumentMeta>({
  totalPages: 1,
  pages: [],
})
const signPdfPlacerRef = ref<InstanceType<typeof SignPdfPlacer>>()
/** 防止快速切换项目时旧请求覆盖新结果 */
let projectLoadSeq = 0
let fileLoadSeq = 0

const batchModeOptions = [
  { label: '连续范围', value: 'range' },
  { label: '全部页', value: 'all' },
  { label: '奇数页', value: 'odd' },
  { label: '偶数页', value: 'even' },
]

const batchDefaults = reactive<BatchPageRange>({
  mode: 'range',
  startPage: 1,
  endPage: 1,
})

const pendingBatchPageCount = computed(() => countBatchPages(batchDefaults, pdfMeta.totalPages))

const batchTaskSummary = (task: PendingSealTask) => {
  if (!task.batchPages) {
    return ''
  }
  const count = countBatchPages(task.batchPages, pdfMeta.totalPages)
  const mode = batchModeLabel(task.batchPages.mode)
  if (task.batchPages.mode === 'all') {
    return `全部 ${count} 页同位置`
  }
  return `第 ${task.batchPages.startPage}-${task.batchPages.endPage} 页 · ${mode} · 共 ${count} 页`
}

const filteredSeals = computed(() => {
  const keyword = sealKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return sealList.value
  }
  return sealList.value.filter((item) => {
    const name = (item.sealName || '').toLowerCase()
    const code = (item.sealCode || '').toLowerCase()
    return name.includes(keyword) || code.includes(keyword)
  })
})

const currentStep = computed(() => {
  if (!form.fileId) {
    return 1
  }
  if (!pendingTasks.value.length) {
    return 2
  }
  return 3
})

const canSubmit = computed(() => Boolean(
  form.projectId
  && form.fileId
  && pendingTasks.value.length
  && pendingTasks.value.every((task) => {
    if (task.type === 'span') {
      return !!task.spanConfig
    }
    if (task.type === 'batch') {
      return !!task.placement && !!task.batchPages && countBatchPages(task.batchPages, pdfMeta.totalPages) > 0
    }
    return !!task.placement
  }),
))

const taskTagLabel = (task: PendingSealTask) => {
  if (task.type === 'span') {
    return '骑缝章'
  }
  if (task.type === 'batch') {
    return '批量章'
  }
  return '普通章'
}

const taskTagColor = (task: PendingSealTask) => {
  if (task.type === 'span') {
    return 'orangered'
  }
  if (task.type === 'batch') {
    return 'purple'
  }
  return 'arcoblue'
}

const placementModeTip = computed(() => {
  if (placementMode.value === 'general') {
    return '点击印章后在当前页放置，可拖动调整位置'
  }
  if (placementMode.value === 'batch') {
    return '点击印章后按页码范围批量放置，各页同坐标'
  }
  return '点击印章后添加骑缝章，可在右侧边缘上下拖动'
})

const placingHint = computed(() => {
  if (!form.fileId) {
    return ''
  }
  if (placementMode.value === 'general') {
    return '普通章：拖动印章调整位置'
  }
  if (placementMode.value === 'batch') {
    return '批量章：拖动调整各页统一位置'
  }
  return '骑缝章：沿页面右侧上下拖动'
})

const emptyPreviewTitle = computed(() => {
  if (!form.projectId) {
    return '请先选择签章项目'
  }
  if (!fileOptions.value.length) {
    return '暂无可用 PDF'
  }
  return '请选择签章文件'
})

const emptyPreviewSub = computed(() => {
  if (!form.projectId) {
    return '选择项目后将自动带出已绑定的合同/其他文件'
  }
  if (!fileOptions.value.length) {
    return '请在「签章项目管理」中上传 PDF 或关联带附件的合同'
  }
  return '从左侧「签章文件」中选择一份 PDF 进行预览与签章'
})

const footerTip = computed(() => {
  if (!form.projectId) {
    return '请先选择签章项目'
  }
  if (!form.fileId) {
    return '请选择项目绑定的 PDF 文件'
  }
  if (!pendingTasks.value.length) {
    return '请在左侧选择印章并在 PDF 上定位'
  }
  const generalCount = pendingTasks.value.filter((item) => item.type === 'general').length
  const batchCount = pendingTasks.value.filter((item) => item.type === 'batch').length
  const spanCount = pendingTasks.value.filter((item) => item.type === 'span').length
  const batchStampCount = pendingTasks.value
    .filter((item) => item.type === 'batch')
    .reduce((sum, item) => sum + countBatchPages(item.batchPages, pdfMeta.totalPages), 0)
  const batchHint = batchCount ? `（批量共 ${batchStampCount} 处）` : ''
  return `已配置 ${generalCount} 个普通章、${batchCount} 个批量章${batchHint}、${spanCount} 个骑缝章，确认后可统一签章`
})

const parseFileIds = (raw?: string | null) => {
  if (raw == null || raw === '') {
    return [] as string[]
  }
  return String(raw)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const isPdfMeta = (name?: string, extension?: string) => {
  const ext = (extension || name?.split('.').pop() || '').toLowerCase()
  return ext === 'pdf'
}

const pickProjectContract = (project: ProjectResp) => {
  if (project.saleContractId) {
    return { type: 'SALE', id: String(project.saleContractId) }
  }
  if (project.purchaseContractId) {
    return { type: 'PURCHASE', id: String(project.purchaseContractId) }
  }
  if (project.repairContractId) {
    return { type: 'REPAIR', id: String(project.repairContractId) }
  }
  if (project.contractType && project.contractId != null && project.contractId !== '') {
    return { type: String(project.contractType), id: String(project.contractId) }
  }
  return undefined
}

const fetchContractFileIds = async (type: string, id: string) => {
  try {
    if (type === 'SALE') {
      const { data } = await getContractSale(id)
      return parseFileIds(data?.fileId)
    }
    if (type === 'PURCHASE') {
      const { data } = await getContractPurchase(id)
      return parseFileIds(data?.fileId)
    }
    if (type === 'REPAIR') {
      const { data } = await getContract(id)
      return parseFileIds(data?.fileId)
    }
  } catch {
    // 合同附件拉取失败不阻断其他文件
  }
  return [] as string[]
}

const assertBlobOk = async (blob: Blob) => {
  if (!blob.type.startsWith('application/json')) {
    return
  }
  const text = await blob.text()
  let msg = '文件加载失败'
  try {
    const json = JSON.parse(text)
    msg = json.msg || msg
  } catch {
    // ignore
  }
  throw new Error(msg)
}

const revokeSealImages = () => {
  Object.values(sealImageMap).forEach((url) => URL.revokeObjectURL(url))
  Object.keys(sealImageMap).forEach((key) => {
    delete sealImageMap[key]
  })
}

const loadSealImage = async (sealId: string | number) => {
  const key = String(sealId)
  if (sealImageMap[key]) {
    return
  }
  try {
    sealImageMap[key] = await fetchSignImageObjectUrl(getSealImageResource(String(sealId)))
  } catch {
    // 图片加载失败时保留占位图标
  }
}

const loadProjects = async () => {
  try {
    const res = await getSignableProjects()
    const list = Array.isArray(res.data) ? res.data : []
    projectOptions.value = list.map((item: any) => ({
      label: item.projectName,
      value: String(item.projectId),
    }))
  } catch {
    projectOptions.value = []
  }
}

const clearFile = () => {
  fileLoadSeq++
  form.fileId = undefined
  selectedFileName.value = ''
  pdfSrc.value = undefined
  pendingTasks.value = []
  selectedTaskId.value = undefined
  pdfMeta.totalPages = 1
  pdfMeta.pages = []
  pdfLoading.value = false
}

const clearProjectFiles = () => {
  fileOptions.value = []
  clearFile()
}

const loadPdfByFileId = async (fileId: string) => {
  const seq = ++fileLoadSeq
  form.fileId = undefined
  pdfLoading.value = true
  pdfSrc.value = undefined
  pendingTasks.value = []
  selectedTaskId.value = undefined
  try {
    const [infoRes, downloadRes] = await Promise.all([
      getFileInfo(fileId).catch(() => null),
      downloadFile(fileId),
    ])
    if (seq !== fileLoadSeq) {
      return
    }
    const blob = downloadRes.data instanceof Blob ? downloadRes.data : new Blob([downloadRes.data])
    await assertBlobOk(blob)
    const pdfBytes = await blob.arrayBuffer()
    if (seq !== fileLoadSeq) return
    selectedFileName.value = infoRes?.data?.originalName
      || infoRes?.data?.name
      || fileOptions.value.find((item) => item.value === fileId)?.label
      || `文件 #${fileId}`
    form.fileId = fileId
    pdfSrc.value = pdfBytes
    // loading 保持到 document-ready
  } catch (error: any) {
    if (seq !== fileLoadSeq) {
      return
    }
    pdfSrc.value = undefined
    pdfLoading.value = false
    form.fileId = undefined
    selectedFileName.value = ''
    Message.error(error?.msg || error?.message || 'PDF 预览加载失败')
  }
}

const resolveProjectBoundFiles = async (project: ProjectResp) => {
  const idSet = new Set<string>()
  parseFileIds(project.otherFileIds).forEach((id) => idSet.add(id))

  const contract = pickProjectContract(project)
  if (contract) {
    const contractIds = await fetchContractFileIds(contract.type, contract.id)
    contractIds.forEach((id) => idSet.add(id))
  }

  const ids = Array.from(idSet)
  if (!ids.length) {
    return [] as { label: string, value: string }[]
  }

  const options = await Promise.all(ids.map(async (id) => {
    try {
      const { data } = await getFileInfo(id)
      const name = data?.originalName || data?.name || `文件 #${id}`
      if (!isPdfMeta(name, data?.extension)) {
        return null
      }
      return { label: name, value: id }
    } catch {
      return null
    }
  }))
  return options.filter(Boolean) as { label: string, value: string }[]
}

const onFileChange = async (fileId?: string) => {
  if (!fileId) {
    clearFile()
    return
  }
  await loadPdfByFileId(fileId)
}

const onProjectChange = async (projectId?: string | number) => {
  const seq = ++projectLoadSeq
  fileLoadSeq += 1
  pendingTasks.value = []
  selectedTaskId.value = undefined
  sealKeyword.value = ''
  sealList.value = []
  revokeSealImages()
  clearProjectFiles()
  if (!projectId) {
    return
  }

  sealLoading.value = true
  fileLoading.value = true
  try {
    const [sealRes, projectRes] = await Promise.all([
      availableSeals(String(projectId)),
      getProject(String(projectId)),
    ])
    if (seq !== projectLoadSeq) {
      return
    }
    sealList.value = sealRes.data || []
    await Promise.all(sealList.value.map((item) => loadSealImage(item.sealId)))

    const options = await resolveProjectBoundFiles(projectRes.data || {})
    if (seq !== projectLoadSeq) {
      return
    }
    fileOptions.value = options
    if (options.length) {
      // 获取到签章文件后立即预览首个，无需再点选触发
      await loadPdfByFileId(options[0].value)
    } else {
      Message.warning('当前项目未绑定可用的 PDF 文件')
    }
  } catch {
    if (seq !== projectLoadSeq) {
      return
    }
    sealList.value = []
    fileOptions.value = []
  } finally {
    if (seq === projectLoadSeq) {
      sealLoading.value = false
      fileLoading.value = false
    }
  }
}

const syncBatchDefaultsEnd = () => {
  batchDefaults.endPage = pdfMeta.totalPages
  if (batchDefaults.startPage > batchDefaults.endPage) {
    batchDefaults.startPage = 1
  }
}

const onBatchModeChange = () => {
  if (batchDefaults.mode === 'all') {
    batchDefaults.startPage = 1
    batchDefaults.endPage = pdfMeta.totalPages
  }
}

const applyBatchPreset = (preset: 'all' | 'odd' | 'even' | 'fromCurrent') => {
  if (preset === 'all') {
    batchDefaults.mode = 'all'
    batchDefaults.startPage = 1
    batchDefaults.endPage = pdfMeta.totalPages
    return
  }
  if (preset === 'fromCurrent') {
    batchDefaults.mode = 'range'
    batchDefaults.startPage = signPdfPlacerRef.value?.currentPage ?? 1
    batchDefaults.endPage = pdfMeta.totalPages
    return
  }
  batchDefaults.mode = preset
  batchDefaults.startPage = 1
  batchDefaults.endPage = pdfMeta.totalPages
}

const onDocumentReady = (meta: PdfDocumentMeta) => {
  pdfLoading.value = false
  pdfMeta.totalPages = meta.totalPages
  pdfMeta.pages = meta.pages
  syncBatchDefaultsEnd()
  pendingTasks.value.forEach((task) => {
    if (task.type === 'span' && task.spanConfig) {
      task.spanConfig.endPage = Math.min(task.spanConfig.endPage, meta.totalPages)
      task.spanConfig.startPage = Math.min(task.spanConfig.startPage, meta.totalPages)
    }
    if (task.type === 'batch' && task.batchPages) {
      task.batchPages.endPage = Math.min(task.batchPages.endPage, meta.totalPages)
      task.batchPages.startPage = Math.min(task.batchPages.startPage, meta.totalPages)
    }
    if (task.type === 'general' && task.placement) {
      task.placement.page = Math.min(task.placement.page, meta.totalPages)
    }
  })
}

const onPdfLoadError = (message: string) => {
  pdfLoading.value = false
  pdfSrc.value = undefined
  form.fileId = undefined
  selectedFileName.value = ''
  Message.error(message || 'PDF 预览加载失败')
}

const getPageMeta = (page: number) => pdfMeta.pages.find((item) => item.page === page) || pdfMeta.pages[0]

const createDefaultPlacement = (page = signPdfPlacerRef.value?.currentPage ?? 1): SealPlacement => {
  const pageMeta = getPageMeta(page)
  const pageWidth = pageMeta?.width || 595
  const pageHeight = pageMeta?.height || 842
  const size = DEFAULT_SEAL_SIZE_PT
  return {
    page,
    x: Math.max(40, pageWidth - size - 80),
    y: Math.max(40, pageHeight - size - 120),
    width: size,
    height: size,
  }
}

const createDefaultSpanConfig = (): SpanSealTaskConfig => {
  const pageMeta = pdfMeta.pages[0]
  const pageHeight = pageMeta?.height || 842
  return {
    startPage: 1,
    endPage: pdfMeta.totalPages,
    yPosition: pageHeight / 2,
    sealSize: DEFAULT_SPAN_SEAL_SIZE_MM,
  }
}

const createDefaultBatchPages = (): BatchPageRange => ({
  mode: batchDefaults.mode,
  startPage: batchDefaults.mode === 'all' ? 1 : batchDefaults.startPage,
  endPage: batchDefaults.endPage,
})

const addSealTask = (seal: SealResp) => {
  if (!form.fileId) {
    Message.warning('请先选择签章文件')
    return
  }
  const taskId = `${seal.sealId}-${Date.now()}`
  const task: PendingSealTask = {
    id: taskId,
    sealId: seal.sealId,
    sealName: seal.sealName || `印章 #${seal.sealId}`,
    type: placementMode.value,
  }
  if (placementMode.value === 'general') {
    task.placement = createDefaultPlacement(signPdfPlacerRef.value?.currentPage ?? 1)
  } else if (placementMode.value === 'batch') {
    task.batchPages = createDefaultBatchPages()
    task.placement = createDefaultPlacement(task.batchPages.startPage)
  } else {
    task.spanConfig = createDefaultSpanConfig()
  }
  pendingTasks.value.push(task)
  selectedTaskId.value = taskId
  loadSealImage(seal.sealId)
  if (placementMode.value === 'general') {
    Message.success('已添加普通章，请拖动定位')
  } else if (placementMode.value === 'batch') {
    const count = countBatchPages(task.batchPages, pdfMeta.totalPages)
    Message.success(`已添加批量章，将覆盖 ${count} 页`)
  } else {
    Message.success('已添加骑缝章，请调整范围与位置')
  }
}

const onUpdatePlacement = ({ taskId, placement }: { taskId: string, placement?: SealPlacement }) => {
  const task = pendingTasks.value.find((item) => item.id === taskId)
  if (task && placement) {
    task.placement = placement
  }
}

const onUpdateSpanConfig = ({ taskId, spanConfig }: { taskId: string, spanConfig?: SpanSealTaskConfig }) => {
  const task = pendingTasks.value.find((item) => item.id === taskId)
  if (task && spanConfig) {
    task.spanConfig = spanConfig
  }
}

const focusTaskPage = (task: PendingSealTask) => {
  let page: number | undefined
  if (task.type === 'general') {
    page = task.placement?.page
  } else if (task.type === 'batch') {
    page = task.batchPages?.startPage
  } else {
    page = task.spanConfig?.startPage
  }
  if (page) {
    signPdfPlacerRef.value?.goToPage(page)
  }
}

const onBatchRangeChange = (task: PendingSealTask) => {
  if (!task.batchPages) {
    return
  }
  if (task.batchPages.mode === 'all') {
    task.batchPages.startPage = 1
    task.batchPages.endPage = pdfMeta.totalPages
  }
  if (task.batchPages.endPage < task.batchPages.startPage) {
    task.batchPages.endPage = task.batchPages.startPage
  }
  if (task.placement) {
    task.placement.page = task.batchPages.startPage
  }
  focusTaskPage(task)
}

const setTaskBatchMode = (task: PendingSealTask, mode: BatchPageMode) => {
  if (!task.batchPages) {
    return
  }
  task.batchPages.mode = mode
  onBatchRangeChange(task)
}

const applyBatchAllPages = (task: PendingSealTask) => {
  if (!task.batchPages) {
    return
  }
  setTaskBatchMode(task, 'all')
  Message.success(`已设为全部 ${pdfMeta.totalPages} 页`)
}

const convertToBatch = (task: PendingSealTask) => {
  if (!task.placement || task.type !== 'general') {
    return
  }
  task.type = 'batch'
  task.batchPages = {
    mode: 'range',
    startPage: task.placement.page,
    endPage: pdfMeta.totalPages,
  }
  Message.success('已扩展为批量章')
}

const convertToSingle = (task: PendingSealTask) => {
  if (!task.placement || task.type !== 'batch') {
    return
  }
  task.type = 'general'
  task.placement.page = task.batchPages?.startPage || task.placement.page
  task.batchPages = undefined
  Message.success('已改为单页普通章')
}

watch(selectedTaskId, (taskId) => {
  if (!taskId) {
    return
  }
  const task = pendingTasks.value.find((item) => item.id === taskId)
  if (task) {
    focusTaskPage(task)
  }
})

const removeTask = (taskId: string) => {
  pendingTasks.value = pendingTasks.value.filter((item) => item.id !== taskId)
  if (selectedTaskId.value === taskId) {
    selectedTaskId.value = pendingTasks.value[0]?.id
  }
}

const clearTasks = () => {
  pendingTasks.value = []
  selectedTaskId.value = undefined
}

const buildGeneralSeals = () => {
  const grouped = new Map<string, SealPlacement[]>()
  pendingTasks.value
    .filter((task) => (task.type === 'general' || task.type === 'batch') && task.placement)
    .forEach((task) => {
      const key = String(task.sealId)
      const list = grouped.get(key) || []
      if (task.type === 'batch') {
        list.push(...expandBatchPlacements(task, pdfMeta.totalPages))
      } else {
        list.push({
          page: task.placement!.page,
          x: task.placement!.x,
          y: task.placement!.y,
          width: task.placement!.width,
          height: task.placement!.height,
        })
      }
      grouped.set(key, list)
    })
  return Array.from(grouped.entries()).map(([sealId, positions]) => ({
    sealId,
    mode: 'custom' as const,
    positions,
  }))
}

const buildSpanSeals = () => pendingTasks.value
  .filter((task) => task.type === 'span' && task.spanConfig)
  .map((task) => ({
    sealId: String(task.sealId),
    startPage: task.spanConfig!.startPage,
    endPage: task.spanConfig!.endPage,
    xPosition: 0,
    yPosition: task.spanConfig!.yPosition,
    sealSize: task.spanConfig!.sealSize,
  }))

const resetAll = () => {
  projectLoadSeq++
  form.projectId = undefined
  clearProjectFiles()
  sealKeyword.value = ''
  sealList.value = []
  placementMode.value = 'general'
  revokeSealImages()
}

const onSubmit = async () => {
  if (submitting.value) return
  if (!form.projectId) {
    Message.warning('请选择签章项目')
    return
  }
  if (!form.fileId) {
    Message.warning('请选择签章文件')
    return
  }
  if (!pendingTasks.value.length) {
    Message.warning('请至少添加一个印章任务')
    return
  }
  const generalSeals = buildGeneralSeals()
  const spanSeals = buildSpanSeals()
  if (!generalSeals.length && !spanSeals.length) {
    Message.warning('请完善印章定位配置')
    return
  }
  submitting.value = true
  const projectId = String(form.projectId)
  const fileId = form.fileId
  const projectVersion = projectLoadSeq
  const fileVersion = fileLoadSeq
  try {
    const primarySealId = pendingTasks.value[0]?.sealId
    await unifiedSign({
      projectId,
      sysFileId: fileId,
      sealId: primarySealId == null ? undefined : String(primarySealId),
      generalSeals: generalSeals.length ? generalSeals : undefined,
      spanSeals: spanSeals.length ? spanSeals : undefined,
    })
    Message.success('签章成功')
    if (projectVersion === projectLoadSeq && fileVersion === fileLoadSeq
      && projectId === String(form.projectId) && fileId === form.fileId) resetAll()
  } catch (error: any) {
    Message.error(error?.msg || error?.message || '签章失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProjects()
})

onUnmounted(() => {
  projectLoadSeq++
  fileLoadSeq++
  revokeSealImages()
})
</script>

<style scoped lang="scss">
.sign-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

.sidebar-block {
  padding: 14px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-bg-2);

  &--grow {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    font-weight: 600;
    color: var(--color-text-1);
  }

  &__count {
    font-size: 12px;
    font-weight: 400;
    color: var(--color-text-3);
  }
}

.mode-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-3);
}

.batch-preview {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-2);

  strong {
    color: rgb(var(--primary-6));
  }
}

.batch-presets {
  margin-top: 8px;
}

.sign-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sign-step {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.2s ease;

  &.active {
    border-color: rgb(var(--primary-3));
    background: linear-gradient(90deg, rgba(var(--success-6), 0.12), rgba(var(--primary-6), 0.12));
  }

  &.done {
    opacity: 0.75;
  }

  &__index {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-3);
    background: var(--color-fill-2);
  }

  &.active &__index {
    color: #fff;
    background: rgb(var(--primary-6));
  }

  &__label {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-1);
  }

  &__desc {
    margin-top: 2px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.seal-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.seal-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(.disabled) {
    border-color: rgb(var(--primary-5));
    background: rgba(var(--primary-6), 0.04);
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }

  &__thumb {
    width: 40px;
    height: 40px;
    border-radius: 6px;
    background: var(--color-fill-2);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-1);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    margin-top: 2px;
    font-size: 12px;
    color: var(--color-text-3);
  }

  &__add {
    color: rgb(var(--primary-6));
    flex-shrink: 0;
  }
}

.empty-box {
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-text-3);
  font-size: 13px;
}

.sign-workspace {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.workspace-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-1);

  &__bar {
    width: 4px;
    height: 18px;
    border-radius: 2px;
    background: rgb(var(--primary-6));
  }
}

.workspace-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-panel,
.tasks-panel {
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-bg-2);
  overflow: hidden;
}

.preview-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tasks-panel {
  flex-shrink: 0;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--color-border-2);
  font-weight: 600;
  color: var(--color-text-1);

  em {
    margin-left: 4px;
    font-style: normal;
    color: rgb(var(--primary-6));
  }
}

.file-name {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-3);
}

.preview-body {
  flex: 1;
  min-height: 0;
  position: relative;
  background: var(--color-fill-1);
}

.preview-spin-mask {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--color-bg-1), 0.65);
}

.empty-preview {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 16px;
  border: 1px dashed var(--color-border-3);
  border-radius: 8px;
  color: var(--color-text-3);
  background: var(--color-bg-1);

  &__title {
    font-size: 15px;
    font-weight: 500;
    color: var(--color-text-1);
  }

  &__sub {
    font-size: 13px;
    color: var(--color-text-3);
    text-align: center;
    padding: 0 24px;
  }
}

.tasks-body {
  max-height: 220px;
  overflow: auto;
  padding: 10px 14px;
}

.task-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  margin-bottom: 8px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease;

  &.active {
    border-color: rgb(var(--primary-5));
    background: rgba(var(--primary-6), 0.04);
  }

  &:last-child {
    margin-bottom: 0;
  }

  &__thumb {
    width: 32px;
    height: 32px;
    border-radius: 6px;
    background: var(--color-fill-2);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-1);
  }

  &__meta {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.task-config {
  margin-top: 8px;

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 8px;
  }

  &__hint {
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.tasks-empty {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed var(--color-border-3);
  border-radius: 6px;
  color: var(--color-text-3);
  font-size: 13px;
}

.workspace-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.footer-tip {
  font-size: 13px;
  color: var(--color-text-3);
}
</style>
