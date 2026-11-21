<template>
  <TodoList :load-page="loadPage" description="请确认已完成维修的处理结果" empty-text="暂无待确认的维修" :item-title="item => item.repairNo" :item-description="item => item.faultDesc || '点击查看维修结果'" :detail-path="item => `/customer/repair/${item.id}`" />
</template>
<script setup>
import TodoList from '@/components/TodoList.vue'
import { getPendingConfirmations } from '@/api/customer'
const loadPage = params => getPendingConfirmations(params)
</script>
