<template>
  <TodoList :load-page="loadPage" :filters="filters" description="处理分配给你的接单、开工和维修事项" empty-text="当前状态暂无待办任务" :item-title="item => item.orderNo" :item-description="item => item.customerName || '点击查看任务详情'" :detail-path="item => `/engineer/task/${item.id}`" />
</template>
<script setup>
import TodoList from '@/components/TodoList.vue'
import { getTaskList } from '@/api/engineer'
const filters = [
  { value: 'WAIT_ACCEPT', label: '待接单' },
  { value: 'ACCEPTED', label: '待开工' },
  { value: 'PROCESSING', label: '维修中' }
]
const loadPage = params => getTaskList(params)
</script>
