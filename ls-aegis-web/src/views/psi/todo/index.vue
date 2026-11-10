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
        <a-input-search v-model="queryForm.title" placeholder="标题" allow-clear @search="search" />
        <a-select v-model="queryForm.todoType" placeholder="待办类型" allow-clear style="width: 150px" @change="search">
          <a-option value="RED_INVOICE">待冲红</a-option>
          <a-option value="REVERSE_WRITEOFF">待反核销</a-option>
          <a-option value="REFUND">待退款</a-option>
          <a-option value="RECEIVE_REFUND">待收退款</a-option>
        </a-select>
        <a-select v-model="queryForm.done" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option :value="false">未完成</a-option>
          <a-option :value="true">已完成</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #todoType="{ record }">
        {{ todoTypeLabel(record.todoType) }}
      </template>
      <template #bizType="{ record }">
        {{ bizTypeLabel(record.bizType) }}
      </template>
      <template #done="{ record }">
        <a-tag v-if="record.done" color="green">已完成</a-tag>
        <a-tag v-else color="orangered">未完成</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link @click="goBiz(record)">{{ todoActionLabel(record.todoType) }}</a-link>
          <a-link v-if="!record.done" v-permission="['psi:todo:done']" @click="onDone(record)">完成</a-link>
          <a-link v-else v-permission="['psi:todo:done']" @click="onReopen(record)">重开</a-link>
        </a-space>
      </template>
    </BaseTable>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import { type PsiTodoPageQuery, donePsiTodo, listPsiTodo, reopenPsiTodo } from '@/apis/psi'
import { useTable } from '@/hooks'
import { bizTypeLabel, resolvePsiTodoRoute, todoActionLabel, todoTypeLabel } from '@/utils/psi-todo-nav'

defineOptions({ name: 'PsiTodo' })

const router = useRouter()
const queryForm = reactive<Partial<PsiTodoPageQuery>>({ done: false, sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listPsiTodo({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '类型', dataIndex: 'todoType', slotName: 'todoType', width: PsiCol.type },
  { title: '标题', dataIndex: 'title' },
  { title: '业务类型', dataIndex: 'bizType', slotName: 'bizType', width: PsiCol.type },
  { title: '业务单号', dataIndex: 'bizNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '状态', dataIndex: 'done', slotName: 'done', width: PsiCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: PsiCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionM },
]

const reset = () => {
  queryForm.title = undefined
  queryForm.todoType = undefined
  queryForm.done = false
  search()
}

const goBiz = (record: any) => {
  const route = resolvePsiTodoRoute(record)
  router.push(route)
}

const onDone = (record: any) => {
  Modal.warning({
    title: '标记完成',
    content: `确认将待办「${record.title}」标记为已完成？`,
    hideCancel: false,
    onOk: async () => {
      await donePsiTodo(record.id)
      Message.success('已完成')
      search()
    },
  })
}

const onReopen = (record: any) => {
  Modal.warning({
    title: '重新打开',
    content: `确认重新打开待办「${record.title}」？`,
    hideCancel: false,
    onOk: async () => {
      await reopenPsiTodo(record.id)
      Message.success('已重开')
      search()
    },
  })
}
</script>
