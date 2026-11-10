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
        <a-input-search v-model="queryForm.keyword" placeholder="搜索编码/名称" allow-clear @search="search" />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:warehouse:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:warehouse:import']" @click="onImport">
          <template #icon><icon-upload /></template>导入
        </a-button>
        <a-button v-permission="['psi:warehouse:export']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #isDefault="{ record }">
        <a-tag v-if="record.isDefault" color="green">默认</a-tag>
        <span v-else>-</span>
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:warehouse:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['psi:warehouse:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
    <PsiImportDrawer
      ref="ImportDrawerRef"
      title="导入仓库"
      hint="是否默认仓填「是/否」；状态填「启用/禁用」；编码可留空自动生成。"
      :download-template-api="downloadWarehouseImportTemplate"
      :parse-api="parseImportWarehouse"
      :import-api="importWarehouse"
      @save-success="search"
    />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import PsiImportDrawer from '../components/PsiImportDrawer.vue'
import { PsiCol } from '../constants/table-col'
import AddModal from './AddModal.vue'
import {
  type WarehouseQuery,
  type WarehouseResp,
  deleteWarehouse,
  downloadWarehouseImportTemplate,
  exportWarehouse,
  importWarehouse,
  listWarehouse,
  parseImportWarehouse,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiWarehouse' })

const queryForm = reactive<WarehouseQuery>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listWarehouse({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '仓库编码', dataIndex: 'warehouseCode', minWidth: PsiCol.codeMinSm, ellipsis: false },
  { title: '仓库名称', dataIndex: 'warehouseName' },
  { title: '负责人', dataIndex: 'manager', width: PsiCol.status },
  { title: '默认仓', dataIndex: 'isDefault', slotName: 'isDefault', width: PsiCol.typeSm },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionS },
]

const reset = () => {
  queryForm.keyword = undefined
  search()
}
const AddModalRef = ref<InstanceType<typeof AddModal>>()
const ImportDrawerRef = ref<InstanceType<typeof PsiImportDrawer>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onImport = () => ImportDrawerRef.value?.onOpen()
const onExport = () => useDownload(() => exportWarehouse(queryForm as any))
const onUpdate = (record: WarehouseResp) => AddModalRef.value?.onUpdate(record.id)
const onDelete = (record: WarehouseResp) => {
  Modal.warning({
    title: '提示',
    content: `是否确定删除「${record.warehouseName}」？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteWarehouse(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
