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
        <a-input-search v-model="queryForm.templateName" placeholder="模板名称" allow-clear @search="search" />
        <a-select
          v-model="queryForm.bizType"
          placeholder="业务类型"
          allow-clear
          style="width: 140px"
          :options="printBizTypeOptions"
          @change="search"
        />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:printTemplate:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
      </template>
      <template #bizType="{ record }">
        <GiCellTag :value="record.bizType" :dict="psi_print_biz_type" />
      </template>
      <template #isDefault="{ record }">
        <a-tag v-if="Number(record.isDefault) === 1" color="green">默认</a-tag>
        <span v-else>—</span>
      </template>
      <template #status="{ record }">
        <GiCellStatus :status="record.status" />
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:printTemplate:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['psi:printTemplate:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <EditModal ref="EditModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import EditModal from './EditModal.vue'
import { type PrintTemplateQuery, type PrintTemplateResp, deletePrintTemplate, listPrintTemplate } from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

defineOptions({ name: 'PsiPrintTemplate' })

const { psi_print_biz_type } = useDict('psi_print_biz_type')
const printBizTypeOptions = computed(() => dictSelectOptions(psi_print_biz_type.value))

const queryForm = reactive<PrintTemplateQuery>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listPrintTemplate({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '模板名称', dataIndex: 'templateName' },
  { title: '业务类型', dataIndex: 'bizType', slotName: 'bizType', width: PsiCol.type },
  { title: '默认', dataIndex: 'isDefault', slotName: 'isDefault', width: PsiCol.unit },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: PsiCol.status },
  { title: '备注', dataIndex: 'remark', width: PsiCol.name },
  { title: '创建时间', dataIndex: 'createTime', width: PsiCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionS },
]

const reset = () => {
  queryForm.templateName = undefined
  queryForm.bizType = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onUpdate = (record: PrintTemplateResp) => EditModalRef.value?.onUpdate(record.id)
const onDelete = (record: PrintTemplateResp) => {
  Modal.warning({
    title: '提示',
    content: `是否删除模板「${record.templateName}」？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deletePrintTemplate(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
