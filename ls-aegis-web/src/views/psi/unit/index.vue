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
        <a-input-search v-model="queryForm.keyword" placeholder="搜索编号/名称" allow-clear @search="search" />
        <a-select
          v-model="queryForm.unitType"
          :options="unitTypeOptions"
          placeholder="单位类型"
          allow-clear
          style="width: 140px"
          @change="search"
        />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:unit:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:unit:import']" @click="onImport">
          <template #icon><icon-upload /></template>导入
        </a-button>
        <a-button v-permission="['psi:unit:export']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #unitType="{ record }">
        <GiCellTag :value="record.unitType" :dict="psi_unit_type" />
      </template>
      <template #unitNature="{ record }">
        <GiCellTag :value="record.unitNature" :dict="unitNatureDict" />
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #action="{ record }">
        <a-space wrap>
          <a-link v-permission="['psi:unit:get']" @click="onSub(record)">子资料</a-link>
          <a-link v-permission="['psi:unit:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="record.status === 1 && ['CUSTOMER', 'BOTH'].includes(record.unitType)" v-permission="['psi:unit:update']" @click="onInvitation(record)">App成员</a-link>
          <a-link v-permission="['psi:unit:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
    <UnitSubDrawer ref="SubDrawerRef" />
    <CustomerMembersDrawer ref="MembersRef" />
    <PsiImportDrawer
      ref="ImportDrawerRef"
      title="导入往来单位"
      hint="单位类型填「客户/供应商/两者」或 CUSTOMER/SUPPLIER/BOTH；性质填「企业/个人」；编码可留空自动生成。"
      :download-template-api="downloadUnitImportTemplate"
      :parse-api="parseImportUnit"
      :import-api="importUnit"
      @save-success="search"
    />
  </GiPageLayout>
</template>

<script setup lang="ts">
/**
 * 业务列约定（配合 BaseTable / normalizeTableColumns）：
 * - 短列：手动 width（类型/性质/状态/时间）
 * - 编号：minWidth + ellipsis:false（工具锁宽）
 * - 名称：width: name / nameLg（超长省略）
 * - 操作列：dataIndex 固定为 action
 */
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import PsiImportDrawer from '../components/PsiImportDrawer.vue'
import { PsiCol } from '../constants/table-col'
import AddModal from './AddModal.vue'
import UnitSubDrawer from './UnitSubDrawer.vue'
import {
  type UnitQuery,
  type UnitResp,
  deleteUnit,
  downloadUnitImportTemplate,
  exportUnit,
  importUnit,
  listUnit,
  parseImportUnit,
} from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import CustomerMembersDrawer from './CustomerMembersDrawer.vue'
import type { LabelValueState } from '@/types/global'

defineOptions({ name: 'PsiUnit' })

const route = useRoute()
const { psi_unit_type } = useDict('psi_unit_type')
const unitNatureDict: LabelValueState[] = [
  { label: '企业', value: 'ENTERPRISE' },
  { label: '个人', value: 'PERSON' },
]
const unitTypeOptions = computed(() =>
  (psi_unit_type.value || []).map((d) => ({ label: d.label, value: d.value })),
)

const initialUnitType = (() => {
  const t = String(route.query.unitType || route.query.type || '').toUpperCase()
  return ['CUSTOMER', 'SUPPLIER', 'BOTH'].includes(t) ? t : undefined
})()

const queryForm = reactive<UnitQuery>({
  sort: ['id,desc'],
  unitType: initialUnitType,
})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listUnit({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '单位编号', dataIndex: 'unitCode', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '单位全称', dataIndex: 'unitName', width: PsiCol.nameLg },
  { title: '类型', dataIndex: 'unitType', slotName: 'unitType', width: PsiCol.type },
  { title: '性质', dataIndex: 'unitNature', slotName: 'unitNature', width: PsiCol.typeSm },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: PsiCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: PsiCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionL },
]

const reset = () => {
  queryForm.keyword = undefined
  queryForm.unitType = undefined
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const SubDrawerRef = ref<InstanceType<typeof UnitSubDrawer>>()
const ImportDrawerRef = ref<InstanceType<typeof PsiImportDrawer>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onImport = () => ImportDrawerRef.value?.onOpen()
const onExport = () => useDownload(() => exportUnit(queryForm as any))
const onUpdate = (record: UnitResp) => AddModalRef.value?.onUpdate(record.id)
const onSub = (record: UnitResp) => SubDrawerRef.value?.open(record.id, record.unitName)
const MembersRef = ref<InstanceType<typeof CustomerMembersDrawer>>()
const onInvitation = (record: UnitResp) => MembersRef.value?.open(record.id, record.unitName)
const onDelete = (record: UnitResp) => {
  Modal.warning({
    title: '提示',
    content: `是否确定删除「${record.unitName}」？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteUnit(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
