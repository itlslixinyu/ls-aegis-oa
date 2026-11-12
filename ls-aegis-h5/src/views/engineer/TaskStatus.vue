<template>
  <div class="status-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="status-head" :class="`status-head--${status}`"><h1>{{ statusLabel }}</h1><p>{{ statusDescription }}</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" v-model:error="failed" error-text="加载失败，点击重试" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished && !failed" class="empty-panel"><van-empty image="search" :description="`暂无${statusLabel}`" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/engineer/task/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.orderNo }}</strong><van-tag type="primary">{{ getEngineerStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.customerName || '维修任务' }}</div><div class="item-desc">接单时间：{{ formatDateTime(item.acceptTime) }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <AppTabbar>
      <van-tabbar-item :to="`/engineer/tasks/status/${status}`" icon="orders-o">{{ statusNavLabel }}</van-tabbar-item>
    </AppTabbar>
  </div>
</template>
<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import { getEngineerStatusText, engineerStatusFilter } from '@/utils/engineerStatus'
import { computed, watch } from 'vue'
import { usePagedList } from '@/composables/usePagedList'
import { useRoute, useRouter } from 'vue-router'
import { getTaskList } from '@/api/engineer'
import { formatDateTime } from '@/utils/dict'
const route = useRoute(); const router = useRouter()
const labels = { WAIT_ACCEPT: '待接单任务', ACCEPTED: '已接单任务', PROCESSING: '维修中任务', COMPLETED: '已完成任务' }
const navLabels = { WAIT_ACCEPT: '待接单', ACCEPTED: '已接单', PROCESSING: '维修中', COMPLETED: '已完成' }
const status = computed(() => engineerStatusFilter(String(route.params.status || 'WAIT_ACCEPT'))); const statusLabel = computed(() => labels[status.value] || '任务列表')
const statusNavLabel = computed(() => navLabels[status.value] || '任务列表')
const descriptions = { WAIT_ACCEPT: '等待接单处理的维修任务', ACCEPTED: '已经接单、等待开始的维修任务', PROCESSING: '正在处理中的维修任务', COMPLETED: '已经完成维修服务的任务' }
const statusDescription = computed(() => descriptions[status.value] || '当前状态下的维修任务')
const {list,loading,finished,refreshing,failed,load,refresh}=usePagedList(params=>getTaskList({...params,status:status.value}))
watch(status,refresh)
function goWorkbench() { router.push('/engineer/workbench') }
</script>
<style scoped src="@/styles/service-list.css"></style>
