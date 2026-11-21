<template>
  <GiPageLayout>
    <BaseTable
      row-key="projectId"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search
          v-model="queryForm.projectName"
          placeholder="搜索项目名称"
          allow-clear
          @search="search"
        />
        <a-input
          v-model="queryForm.projectCode"
          placeholder="项目编号"
          allow-clear
          style="width: 160px"
          @press-enter="search"
        />
        <a-select
          v-model="queryForm.status"
          placeholder="项目状态"
          allow-clear
          style="width: 140px"
          :options="projectStatusOptions"
          @change="search"
        />
        <a-select
          v-model="queryForm.contractType"
          placeholder="已绑合同类型"
          allow-clear
          style="width: 140px"
          :options="contractTypeOptions"
          @change="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['sign:project:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
        <a-button v-permission="['sign:project:export']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </template>
      <template #status="{ record }">
        <GiCellTag :value="record.status" :dict="sign_project_status" />
      </template>
      <template #contract="{ record }">
        <span :title="contractTip(record)">
          {{ record.contractNo || '-' }}
        </span>
      </template>
      <template #action="{ record }">
        <a-space>
          <template v-if="record.status === '0'">
            <a-link v-permission="['sign:project:review']" status="success" @click="onReview(record, '1')">通过</a-link>
            <a-link v-permission="['sign:project:review']" status="danger" @click="onReview(record, '5')">驳回</a-link>
          </template>
          <a-link v-permission="['sign:project:edit']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['sign:project:remove']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { SignCol } from '../constants/table-col'
import AddModal from './AddModal.vue'
import { type ProjectResp, delProject, exportProject, listProject, reviewProject } from '@/apis/sign'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

defineOptions({ name: 'SignProject' })

const { sign_project_status, sign_contract_type } = useDict('sign_project_status', 'sign_contract_type')
const projectStatusOptions = computed(() => dictSelectOptions(sign_project_status.value))
const contractTypeOptions = computed(() => dictSelectOptions(sign_contract_type.value))

const queryForm = reactive<Record<string, any>>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listProject({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '项目名称', dataIndex: 'projectName', width: SignCol.nameLg, ellipsis: true, tooltip: true },
  { title: '项目编号', dataIndex: 'projectCode', minWidth: SignCol.codeMin, ellipsis: false },
  { title: '关联合同', dataIndex: 'contractNo', minWidth: SignCol.codeMin, ellipsis: true, tooltip: true, slotName: 'contract' },
  { title: '负责人', dataIndex: 'managerNames', width: SignCol.name },
  { title: '所属部门', dataIndex: 'deptName', width: SignCol.name },
  { title: '开始日期', dataIndex: 'startDate', width: SignCol.date },
  { title: '结束日期', dataIndex: 'endDate', width: SignCol.date },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: SignCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: SignCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: SignCol.actionXL },
]

const typeLabel = (type?: string) => {
  if (type === 'SALE') return '销售'
  if (type === 'PURCHASE') return '采购'
  if (type === 'REPAIR') return '维修'
  return type
}
const contractTip = (record: ProjectResp) => {
  const no = record.contractNo
    || record.saleContractNo
    || record.purchaseContractNo
    || record.repairContractNo
  const name = record.contractName
    || record.saleContractName
    || record.purchaseContractName
    || record.repairContractName
  const partner = record.partnerName
    || record.salePartnerName
    || record.purchasePartnerName
    || record.repairPartnerName
  return [typeLabel(record.contractType), no, name, partner].filter(Boolean).join(' · ') || undefined
}
const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onUpdate = (record: ProjectResp) => AddModalRef.value?.onUpdate(record.projectId)
const onDelete = (record: ProjectResp) =>
  handleDelete(() => delProject(record.projectId), { content: `是否删除项目「${record.projectName}」？` })
const onExport = () => useDownload(() => exportProject(queryForm))

const onReview = (record: ProjectResp, status: string) => {
  const msg = status === '1' ? '通过' : '驳回'
  Modal.warning({
    title: '提示',
    content: `确定要${msg}项目「${record.projectName}」吗？`,
    hideCancel: false,
    onOk: async () => {
      await reviewProject(record.projectId, status)
      Message.success(`${msg}成功`)
      search()
    },
  })
}
</script>
