<template>
  <GiPageLayout>
    <BaseTable
      ref="tableRef"
      row-key="id"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search v-model="queryForm.name" placeholder="搜索分类名称" allow-clear @search="search" />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:goodsCategory:create']" type="primary" @click="onAdd()">
          <template #icon><icon-plus /></template>新增一级
        </a-button>
        <a-button v-permission="['psi:goodsCategory:import']" @click="onImport">
          <template #icon><icon-upload /></template>导入
        </a-button>
        <a-button v-permission="['psi:goodsCategory:export']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #level="{ record }">
        <a-tag :color="record.level === 1 ? 'arcoblue' : 'green'" size="small">
          {{ record.levelName || (record.level === 1 ? '一级' : '二级') }}
        </a-tag>
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:goodsCategory:update']" @click="onUpdate(record)">修改</a-link>
          <a-link
            v-if="isLevel1(record)"
            v-permission="['psi:goodsCategory:create']"
            @click="onAdd(record.id)"
          >
            新增二级
          </a-link>
          <a-link v-permission="['psi:goodsCategory:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" :categories="dataList" @save-success="search" />
    <PsiImportDrawer
      ref="ImportDrawerRef"
      title="导入商品分类"
      hint="上级分类编码留空表示一级；二级须填已存在或本表先导入的一级编码。编码自动生成，勿手填。"
      :download-template-api="downloadGoodsCategoryImportTemplate"
      :parse-api="parseImportGoodsCategory"
      :import-api="importGoodsCategory"
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
  type GoodsCategoryQuery,
  type GoodsCategoryResp,
  deleteGoodsCategory,
  downloadGoodsCategoryImportTemplate,
  exportGoodsCategory,
  importGoodsCategory,
  listGoodsCategory,
  parseImportGoodsCategory,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiGoodsCategory' })

const queryForm = reactive<GoodsCategoryQuery>({})
const tableRef = ref()
const {
  tableData: dataList,
  loading,
  search,
  refresh,
  handleDelete,
} = useTable(() => listGoodsCategory(queryForm), {
  immediate: true,
  onSuccess: () => {
    nextTick(() => {
      tableRef.value?.tableRef?.expandAll?.(true)
    })
  },
})

const isLevel1 = (record: GoodsCategoryResp) => {
  if (record.level != null) return Number(record.level) === 1
  return record.parentId == null || String(record.parentId) === '0'
}

const columns: TableInstance['columns'] = [
  { title: '分类编码', dataIndex: 'code', minWidth: PsiCol.codeMinSm, ellipsis: false, align: 'left' },
  { title: '分类名称', dataIndex: 'name', width: PsiCol.nameLg, align: 'left' },
  { title: '级别', dataIndex: 'level', slotName: 'level', width: 80 },
  { title: '排序', dataIndex: 'sort', width: PsiCol.qty },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: PsiCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: PsiCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionL },
]

const reset = () => {
  queryForm.name = undefined
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const ImportDrawerRef = ref<InstanceType<typeof PsiImportDrawer>>()
const onAdd = (parentId?: string) => AddModalRef.value?.onAdd(parentId)
const onImport = () => ImportDrawerRef.value?.onOpen()
const onExport = () => useDownload(() => exportGoodsCategory(queryForm as any))
const onUpdate = (record: GoodsCategoryResp) => AddModalRef.value?.onUpdate(record.id)
const onDelete = (record: GoodsCategoryResp) => {
  const levelText = isLevel1(record) ? '一级' : '二级'
  Modal.warning({
    title: '提示',
    content: `是否确定删除${levelText}分类「${record.name}」？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteGoodsCategory(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
