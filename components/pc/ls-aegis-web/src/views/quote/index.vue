<template>
  <GiPageLayout>
    <BaseTable
      row-key="id"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-button-group>
          <a-button :type="listScope === 'ACTIVE' ? 'primary' : 'secondary'" @click="switchScope('ACTIVE')">
            进行中
          </a-button>
          <a-button :type="listScope === 'HISTORY' ? 'primary' : 'secondary'" @click="switchScope('HISTORY')">
            历史
          </a-button>
        </a-button-group>
        <a-input-search
          v-model="queryForm.quoteNo"
          name="quoteNo"
          placeholder="搜索报价单号"
          allow-clear
          @search="search"
        />
        <a-input
          v-model="queryForm.projectName"
          name="projectName"
          placeholder="项目名称"
          allow-clear
          style="width: 140px"
          @press-enter="search"
        />
        <a-select
          v-model="queryForm.status"
          name="status"
          placeholder="状态"
          allow-clear
          style="width: 140px"
          :options="statusOptions"
          @change="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>

      <template #toolbar-right>
        <a-button v-permission="['quote:export']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
        <a-button
          v-if="listScope === 'ACTIVE'"
          v-permission="['quote:add']"
          type="primary"
          @click="onAdd"
        >
          <template #icon><icon-plus /></template>
          新增
        </a-button>
      </template>

      <template #totalAmount="{ record }">
        {{ formatAmount(record.totalAmount ?? record.amount) }}
      </template>

      <template #status="{ record }">
        <GiCellTag :value="record.status" :dict="eoms_quote_status" />
      </template>

      <template #action="{ record }">
        <a-space wrap>
          <a-link v-permission="['quote:update', 'quote:query']" @click="onUpdate(record)">
            {{ isLocked(record.status) ? '查看' : '编辑' }}
          </a-link>
          <a-link v-permission="['quote:export']" @click="onExportWord(record)">Word</a-link>
          <a-link v-permission="['quote:export']" @click="onExportPdf(record)">PDF</a-link>
          <a-link
            v-if="!isLocked(record.status)"
            v-permission="['quote:delete']"
            status="danger"
            @click="onDelete(record)"
          >
            删除
          </a-link>
        </a-space>
      </template>
    </BaseTable>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { useRouter } from 'vue-router'
import {
  type QuoteResp,
  deleteQuote,
  buildQuoteExportFileName,
  downloadQuoteDocx,
  downloadQuotePdf,
  exportQuote,
  listQuote,
} from '@/apis/quote'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { TableCol } from '@/constant/table-col'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'

defineOptions({ name: 'QuoteIndex' })

type ListScope = 'ACTIVE' | 'HISTORY'

const ACTIVE_STATUSES = new Set(['DRAFT', 'SUBMIT', 'CANCEL'])
const HISTORY_STATUSES = new Set(['CONFIRM'])

const router = useRouter()
const { eoms_quote_status } = useDict('eoms_quote_status')

const listScope = ref<ListScope>('ACTIVE')

const statusOptions = computed(() => {
  const allow = listScope.value === 'ACTIVE' ? ACTIVE_STATUSES : HISTORY_STATUSES
  return (eoms_quote_status.value || [])
    .filter((d) => allow.has(String(d.value || '').toUpperCase()))
    .map((d) => ({ label: d.label, value: d.value }))
})

const queryForm = reactive<Record<string, any>>({
  statusScope: 'ACTIVE' as ListScope,
})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listQuote({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '报价单号', dataIndex: 'quoteNo', minWidth: TableCol.codeMin, ellipsis: false },
  { title: '项目名称', dataIndex: 'projectName' },
  { title: '金额', dataIndex: 'totalAmount', slotName: 'totalAmount', width: 120 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: TableCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: TableCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: TableCol.actionL },
]

const isLocked = (status?: string) => {
  const s = String(status || '').toUpperCase()
  return s === 'CONFIRM' || s === 'CANCEL'
}

const formatAmount = (val?: number | string | null) => {
  if (val == null || val === '') return '-'
  const n = Number(val)
  return Number.isFinite(n) ? n.toFixed(2) : '-'
}

const switchScope = (scope: ListScope) => {
  if (listScope.value === scope) return
  listScope.value = scope
  queryForm.statusScope = scope
  queryForm.status = undefined
  search()
}

const reset = () => {
  queryForm.quoteNo = undefined
  queryForm.projectName = undefined
  queryForm.status = undefined
  queryForm.statusScope = listScope.value
  search()
}

const onExport = () => useDownload(() => exportQuote({ ...queryForm }))

const onAdd = () => router.push('/quote/form')
const onUpdate = (record: QuoteResp) => router.push({ path: '/quote/form', query: { id: String(record.id) } })

const onExportWord = async (record: QuoteResp) => {
  await useDownload(
    () => downloadQuoteDocx(record.id),
    buildQuoteExportFileName(record.projectName, record.quoteNo, '.docx'),
    '.docx',
  )
}

const onExportPdf = async (record: QuoteResp) => {
  await useDownload(
    () => downloadQuotePdf(record.id),
    buildQuoteExportFileName(record.projectName, record.quoteNo, '.pdf'),
    '.pdf',
  )
}

const onDelete = (record: QuoteResp) =>
  handleDelete(() => deleteQuote(record.id), { content: `是否删除报价单「${record.quoteNo}」？` })
</script>
