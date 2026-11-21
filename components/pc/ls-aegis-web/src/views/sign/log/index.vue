<template>
  <GiPageLayout>
    <BaseTable
      row-key="recordId"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-select
          v-model="queryForm.projectId"
          placeholder="选择项目"
          allow-clear
          allow-search
          style="width: 180px"
          :options="projectOptions"
          @change="search"
        />
        <a-select
          v-model="queryForm.sealId"
          placeholder="选择印章"
          allow-clear
          allow-search
          style="width: 180px"
          :options="sealOptions"
          @change="search"
        />
        <a-select
          v-model="queryForm.signMode"
          placeholder="签章模式"
          allow-clear
          style="width: 140px"
          :options="signModeOptions"
          @change="search"
        />
        <a-checkbox v-model="queryForm.hasHash" @change="search">包含哈希</a-checkbox>
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['sign:record:export']" @click="onExport">
          <template #icon><icon-download /></template>
          导出 CSV
        </a-button>
      </template>
      <template #fileName="{ record }">
        {{ record.signedFileName || record.fileName || '-' }}
      </template>
      <template #operator="{ record }">
        {{ record.nickName || record.userName || '-' }}
      </template>
      <template #signMode="{ record }">
        <a-tag>{{ modeLabel(record.signMode) }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link @click="onDetail(record)">详情</a-link>
          <a-link v-if="record.recordId" @click="onReceipt(record)">回单</a-link>
          <a-link v-if="record.signedFileId" status="success" @click="onDownload(record)">下载</a-link>
        </a-space>
      </template>
    </BaseTable>

    <a-modal v-model:visible="detailVisible" title="签章日志详情" :width="640" :footer="false">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item v-for="item in detailRows" :key="item.label" :label="item.label">
          {{ item.value ?? '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { SignCol } from '../constants/table-col'
import {
  createDownloadFileName,
  createReceiptDownloadFileName,
} from './downloadUtils'
import {
  type SignRecordResp,
  downloadSignFile,
  downloadVerifyReceiptPdf,
  exportSignRecord,
  listProject,
  listSeal,
  listSignRecord,
} from '@/apis/sign'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions, findDictLabel } from '@/utils/dict'

defineOptions({ name: 'SignLog' })

const { sign_mode, sign_record_status } = useDict('sign_mode', 'sign_record_status')
const signModeOptions = computed(() => dictSelectOptions(sign_mode.value))

const queryForm = reactive<Record<string, any>>({
  hasHash: false,
})

const projectOptions = ref<{ label: string, value: string | number }[]>([])
const sealOptions = ref<{ label: string, value: string | number }[]>([])

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable(async (page) => {
  const res = await listSignRecord({
    projectId: queryForm.projectId,
    sealId: queryForm.sealId,
    signMode: queryForm.signMode,
    ...page,
  })
  if (queryForm.hasHash) {
    const list = (res.data?.list || []).filter((r) => r.signedFileId)
    return { ...res, data: { list, total: list.length } }
  }
  return res
}, { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '项目名称', dataIndex: 'projectName', width: SignCol.nameLg, ellipsis: true, tooltip: true },
  { title: '印章名称', dataIndex: 'sealName', width: SignCol.name },
  { title: '文件名称', dataIndex: 'signedFileName', slotName: 'fileName', ellipsis: true, tooltip: true },
  { title: '操作人', dataIndex: 'nickName', slotName: 'operator', width: 100 },
  { title: '签章模式', dataIndex: 'signMode', slotName: 'signMode', width: SignCol.type },
  { title: '签章时间', dataIndex: 'signTime', width: SignCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: SignCol.actionL },
]

const modeLabel = (mode?: string) => findDictLabel(sign_mode.value, mode)

const loadOptions = async () => {
  try {
    const [pRes, sRes] = await Promise.all([
      listProject({ page: 1, size: 1000 }),
      listSeal({ page: 1, size: 1000, status: '0' }),
    ])
    projectOptions.value = (pRes.data?.list || []).map((p) => ({
      label: p.projectName || String(p.projectId),
      value: String(p.projectId),
    }))
    sealOptions.value = (sRes.data?.list || []).map((s) => ({
      label: s.sealName || String(s.sealId),
      value: String(s.sealId),
    }))
  } catch { /* ignore */ }
}

const reset = () => {
  queryForm.projectId = undefined
  queryForm.sealId = undefined
  queryForm.signMode = undefined
  queryForm.hasHash = false
  search()
}

const detailVisible = ref(false)
const detail = ref<SignRecordResp>({})
const detailRows = computed(() => [
  { label: '记录ID', value: detail.value.recordId },
  { label: '项目名称', value: detail.value.projectName },
  { label: '印章名称', value: detail.value.sealName },
  { label: '操作人', value: detail.value.nickName || detail.value.userName || '-' },
  { label: '签章模式', value: modeLabel(detail.value.signMode) },
  { label: '签章时间', value: detail.value.signTime },
  { label: '客户端IP', value: detail.value.clientIp },
  { label: 'IP归属地', value: detail.value.ipLocation },
  { label: '操作系统', value: detail.value.osVersion },
  { label: '浏览器', value: detail.value.browserVersion },
  { label: '是否时间戳', value: detail.value.hasTimestamp === '1' ? '是' : '否' },
  { label: '原文件名称', value: detail.value.fileName },
  { label: '签章后文件', value: detail.value.signedFileName },
  { label: '签章位置', value: detail.value.positions },
  {
    label: '状态',
    value: findDictLabel(sign_record_status.value, detail.value.status),
  },
])

const onDetail = (row: SignRecordResp) => {
  detail.value = row
  detailVisible.value = true
}

const onDownload = (row: SignRecordResp) => {
  if (!row.signedFileId) {
    Message.warning('暂无签章文件')
    return
  }
  useDownload(() => downloadSignFile(row.signedFileId!), createDownloadFileName(row))
}

const onReceipt = (row: SignRecordResp) => {
  if (!row.recordId) {
    Message.warning('暂无回单记录')
    return
  }
  useDownload(() => downloadVerifyReceiptPdf(row.recordId!), createReceiptDownloadFileName(row))
}

const onExport = () => {
  Modal.info({
    title: '导出提示',
    content: '确认按当前筛选条件导出签章日志为 CSV 文件？',
    hideCancel: false,
    onOk: () => {
      useDownload(() => exportSignRecord({
        projectId: queryForm.projectId,
        sealId: queryForm.sealId,
        signMode: queryForm.signMode,
      }))
    },
  })
}

onMounted(() => loadOptions())
</script>
