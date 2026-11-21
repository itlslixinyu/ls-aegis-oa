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
        <a-input-search v-model="queryForm.accountName" placeholder="户名" allow-clear @search="search" />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:account:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:account:import']" @click="onImport">
          <template #icon><icon-upload /></template>导入
        </a-button>
        <a-button v-permission="['psi:account:export']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:account:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['psi:account:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
    <PsiImportDrawer
      ref="ImportDrawerRef"
      title="导入公司账户"
      hint="户名必填；账号会按国密 SM4 存储；状态填「启用/禁用」。"
      :download-template-api="downloadAccountImportTemplate"
      :parse-api="parseImportAccount"
      :import-api="importAccount"
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
  type CompanyAccountQuery,
  type CompanyAccountResp,
  deleteAccount,
  downloadAccountImportTemplate,
  exportAccount,
  importAccount,
  listAccount,
  parseImportAccount,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiAccount' })

const queryForm = reactive<CompanyAccountQuery>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listAccount({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '户名', dataIndex: 'accountName' },
  { title: '开户行', dataIndex: 'bankName', width: PsiCol.name },
  { title: '账号', dataIndex: 'accountNo', minWidth: PsiCol.codeMinLg, ellipsis: false },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionS },
]

const reset = () => {
  queryForm.accountName = undefined
  search()
}
const AddModalRef = ref<InstanceType<typeof AddModal>>()
const ImportDrawerRef = ref<InstanceType<typeof PsiImportDrawer>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onImport = () => ImportDrawerRef.value?.onOpen()
const onExport = () => useDownload(() => exportAccount(queryForm as any))
const onUpdate = (record: CompanyAccountResp) => AddModalRef.value?.onUpdate(record.id)
const onDelete = (record: CompanyAccountResp) => {
  Modal.warning({
    title: '提示',
    content: `是否确定删除「${record.accountName}」？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteAccount(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
