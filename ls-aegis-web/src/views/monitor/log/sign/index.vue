<template>
  <BaseTable
    row-key="logId"
    :data="dataList"
    :columns="columns"
    :loading="loading"
    :pagination="pagination"
    :disabled-tools="['size', 'setting']"
    @refresh="refresh"
  >
    <template #toolbar-left>
      <a-input
        v-model="queryForm.operator"
        placeholder="操作人"
        allow-clear
        style="width: 140px"
        @press-enter="search"
      />
      <a-select
        v-model="queryForm.actionType"
        placeholder="操作类型"
        allow-clear
        style="width: 160px"
        :options="auditActionOptions"
        @change="search"
      />
      <a-range-picker
        v-model="dateRange"
        show-time
        :format="DATETIME_FORMAT"
        :value-format="DATETIME_FORMAT"
        style="width: 380px"
        @change="search"
      />
      <a-button @click="reset">
        <template #icon><icon-refresh /></template>
        重置
      </a-button>
    </template>
    <template #toolbar-right>
      <a-button type="primary" @click="search">
        <template #icon><icon-refresh /></template>
        刷新
      </a-button>
    </template>
    <template #actionType="{ record }">
      <a-tag :color="actionColor(record.actionType)">{{ actionLabel(record.actionType) }}</a-tag>
    </template>
    <template #operator="{ record }">
      {{ record.nickName || record.userName || '-' }}
    </template>
    <template #target="{ record }">
      {{ record.projectName || '-' }}{{ record.sealName ? ` / ${record.sealName}` : '' }}
    </template>
    <template #result="{ record }">
      <a-tag :color="resultColor(record.resultCode)">{{ resultLabel(record.resultCode) }}</a-tag>
    </template>
  </BaseTable>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { SignCol } from '@/views/sign/constants/table-col'
import { listAuditLog } from '@/apis/sign'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions, findDictLabel, findDictTagColor } from '@/utils/dict'

defineOptions({ name: 'SignAuditLog' })

const { sign_audit_action } = useDict('sign_audit_action')
const auditActionOptions = computed(() => dictSelectOptions(sign_audit_action.value))

const queryForm = reactive<Record<string, any>>({})
const dateRange = ref<string[]>([])

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => {
  const params: Record<string, any> = {
    actionType: queryForm.actionType,
    ...page,
  }
  if (queryForm.operator) params.userName = queryForm.operator
  if (dateRange.value?.length === 2) {
    params.params = {
      beginTime: dateRange.value[0],
      endTime: dateRange.value[1],
    }
  }
  return listAuditLog(params)
}, { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '操作类型', dataIndex: 'actionType', slotName: 'actionType', width: 120 },
  { title: '操作人', dataIndex: 'nickName', slotName: 'operator', width: 100 },
  { title: '操作对象', dataIndex: 'projectName', slotName: 'target', ellipsis: true, tooltip: true },
  { title: 'IP地址', dataIndex: 'clientIp', width: 140 },
  { title: '操作地点', dataIndex: 'ipLocation', width: 120 },
  { title: '结果', dataIndex: 'resultCode', slotName: 'result', width: SignCol.status },
  { title: '操作时间', dataIndex: 'signTime', width: SignCol.date },
]

const actionLabel = (type?: string) => findDictLabel(sign_audit_action.value, type)

const actionColor = (type?: string) => findDictTagColor(sign_audit_action.value, type)

const resultLabel = (result?: string) => {
  if (!result) return '-'
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAIL: '失败',
    FAILED: '失败',
    '0': '成功',
    '1': '失败',
  }
  return map[result] || result
}

const resultColor = (result?: string) => {
  if (!result) return 'gray'
  if (result === 'SUCCESS' || result === '0') return 'green'
  if (result === 'FAIL' || result === 'FAILED' || result === '1') return 'red'
  return 'gray'
}

const reset = () => {
  queryForm.operator = undefined
  queryForm.actionType = undefined
  dateRange.value = []
  search()
}
</script>
