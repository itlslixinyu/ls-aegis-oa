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
        <a-tree-select
          v-model="queryForm.categoryId"
          :data="categoryTree"
          placeholder="商品分类"
          allow-clear
          allow-search
          :field-names="{ key: 'key', title: 'title', children: 'children' }"
          style="width: 180px"
          @change="search"
        />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:goods:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:goods:import']" @click="onImport">
          <template #icon><icon-upload /></template>导入
        </a-button>
        <a-button v-permission="['psi:goods:export']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:goods:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['psi:goods:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
    <PsiImportDrawer
      ref="ImportDrawerRef"
      title="导入商品"
      hint="分类编码须为二级（如 1001）；名称与分类、单位、税率必填；商品编号可留空自动生成。"
      :download-template-api="downloadGoodsImportTemplate"
      :parse-api="parseImportGoods"
      :import-api="importGoods"
      @save-success="search"
    />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import mapTree from 'xe-utils/mapTree'
import PsiImportDrawer from '../components/PsiImportDrawer.vue'
import { PsiCol } from '../constants/table-col'
import AddModal from './AddModal.vue'
import {
  type GoodsQuery,
  type GoodsResp,
  deleteGoods,
  downloadGoodsImportTemplate,
  exportGoods,
  importGoods,
  listGoods,
  listGoodsCategory,
  parseImportGoods,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiGoods' })

const queryForm = reactive<GoodsQuery>({ sort: ['id,desc'] })
const categoryTree = ref<any[]>([])

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listGoods({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '商品编号', dataIndex: 'goodsCode', minWidth: PsiCol.codeMinSm, ellipsis: false },
  { title: '商品名称', dataIndex: 'goodsName' },
  { title: '分类', dataIndex: 'categoryName', width: PsiCol.type },
  { title: '规格', dataIndex: 'spec', width: PsiCol.spec },
  { title: '单位', dataIndex: 'unit', width: PsiCol.unit },
  { title: '税率%', dataIndex: 'defaultTaxRate', width: PsiCol.typeSm },
  { title: '下限', dataIndex: 'stockLower', width: PsiCol.qty },
  { title: '上限', dataIndex: 'stockUpper', width: PsiCol.qty },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionS },
]

const loadCategories = async () => {
  const { data } = await listGoodsCategory({})
  categoryTree.value = mapTree(data || [], (i: any) => {
    const isLevel1 = Number(i.level) === 1 || i.parentId == null || String(i.parentId) === '0'
    return {
      key: String(i.id),
      title: `${i.levelName || (isLevel1 ? '一级' : '二级')} ${i.code ? `${i.code} ` : ''}${i.name}`,
      // 筛选也可点一级：表示该一级下全部（由后端扩展时再支持）；此处仍允许点选便于浏览
      children: i.children,
    }
  })
}

const reset = () => {
  queryForm.keyword = undefined
  queryForm.categoryId = undefined
  search()
}
const AddModalRef = ref<InstanceType<typeof AddModal>>()
const ImportDrawerRef = ref<InstanceType<typeof PsiImportDrawer>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onImport = () => ImportDrawerRef.value?.onOpen()
const onExport = () => useDownload(() => exportGoods(queryForm as any))
const onUpdate = (record: GoodsResp) => AddModalRef.value?.onUpdate(record.id)
const onDelete = (record: GoodsResp) => {
  Modal.warning({
    title: '提示',
    content: `是否确定删除「${record.goodsName}」？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteGoods(record.id))
      Message.success('删除成功')
    },
  })
}

onMounted(loadCategories)
</script>
