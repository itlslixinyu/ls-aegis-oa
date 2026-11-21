<template>
  <GiPageLayout>
    <a-tabs v-model:active-key="tab" type="rounded" @change="onTabChange">
      <a-tab-pane key="STOCK" title="库存期初" />
      <a-tab-pane key="AR" title="应收期初" />
      <a-tab-pane key="AP" title="应付期初" />
      <a-tab-pane key="PREPAY" title="预付期初" />
    </a-tabs>
    <BaseTable
      :key="tab"
      row-key="id"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search v-model="queryForm.docNo" placeholder="单号" allow-clear @search="search" />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button
          v-if="tab === 'STOCK'"
          v-permission="['psi:init:import']"
          @click="onImport"
        >
          <template #icon><icon-upload /></template>导入库存期初
        </a-button>
        <a-button v-permission="['psi:init:confirm']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else>{{ record.docStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:init:list']" @click="onView(record)">详情</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:init:confirm']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:init:confirm']" @click="onConfirm(record)">确认</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:init:confirm']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <EditModal ref="EditModalRef" @save-success="search" />
    <PsiImportDrawer
      ref="ImportDrawerRef"
      title="导入库存期初"
      hint="按「仓库编码+期初日期」分组生成草稿单；商品须已建档；导入后请人工确认。"
      :download-template-api="downloadInitStockImportTemplate"
      :parse-api="parseImportInitStock"
      :import-api="importInitStock"
      @save-success="search"
    />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import PsiImportDrawer from '../components/PsiImportDrawer.vue'
import { PsiCol } from '../constants/table-col'
import EditModal from './EditModal.vue'
import {
  type InitDocQuery,
  type InitDocResp,
  type InitStockResp,
  confirmInitDoc,
  confirmInitStock,
  deleteInitDoc,
  deleteInitStock,
  downloadInitStockImportTemplate,
  importInitStock,
  listInitDoc,
  listInitStock,
  parseImportInitStock,
} from '@/apis/psi'
import { useTable } from '@/hooks'

defineOptions({ name: 'PsiInit' })

const tab = ref('STOCK')
const queryForm = reactive<InitDocQuery>({ type: 'STOCK', sort: ['id,desc'] })

const fetchList = (page: any) => {
  if (tab.value === 'STOCK') {
    return listInitStock({ ...queryForm, ...page })
  }
  return listInitDoc({ ...queryForm, type: tab.value, ...page })
}

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable<InitDocResp | InitStockResp>(fetchList, { immediate: true })

const columns = computed<TableInstance['columns']>(() => {
  /**
   * 期初建账示例：
   * - 单号：弹性 + minWidth codeMin + ellipsis:false；仓库：弹性
   * - 期初日期 / 状态 / 操作：固定 width
   */
  const base: TableInstance['columns'] = [
    { title: '单号', dataIndex: 'docNo', minWidth: PsiCol.codeMin, ellipsis: false },
    { title: '期初日期', dataIndex: 'initDate', width: 200 },
  ]
  if (tab.value === 'STOCK') {
    base.push({ title: '仓库', dataIndex: 'warehouseName', width: PsiCol.name })
  }
  base.push(
    { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: 100 },
    { title: '操作', dataIndex: 'action', slotName: 'action', width: 240 },
  )
  return base
})

const onTabChange = () => {
  queryForm.type = tab.value
  search()
}

const reset = () => {
  queryForm.docNo = undefined
  queryForm.docStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const ImportDrawerRef = ref<InstanceType<typeof PsiImportDrawer>>()
const onAdd = () => EditModalRef.value?.onAdd(tab.value)
const onImport = () => ImportDrawerRef.value?.onOpen()
const onUpdate = (record: any) => EditModalRef.value?.onUpdate(tab.value, record.id)
const onView = (record: any) => EditModalRef.value?.onView(tab.value, record.id)

const onConfirm = (record: any) => {
  Modal.warning({
    title: '确认期初',
    content: `确认后将写入台账/库存，单号 ${record.docNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      const key = crypto.randomUUID()
      if (tab.value === 'STOCK') {
        await confirmInitStock(record.id, key)
      } else {
        await confirmInitDoc(tab.value, record.id, key)
      }
      Message.success('确认成功')
      search()
    },
  })
}

const onDelete = (record: any) => {
  Modal.warning({
    title: '提示',
    content: `是否删除草稿 ${record.docNo}？`,
    hideCancel: false,
    onOk: async () => {
      if (tab.value === 'STOCK') {
        await handleDelete(() => deleteInitStock(record.id))
      } else {
        await handleDelete(() => deleteInitDoc(tab.value, record.id))
      }
      Message.success('删除成功')
    },
  })
}
</script>
