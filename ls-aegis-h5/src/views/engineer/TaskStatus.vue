<template>
  <div class="status-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="status-head" :class="`status-head--${status}`"><h1>{{ statusLabel }}</h1><p>{{ statusDescription }}</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished" class="empty-panel"><van-empty image="search" :description="`暂无${statusLabel}`" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/engineer/task/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.orderNo }}</strong><van-tag type="primary">{{ getOrderStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.address || '未填写维修地址' }}</div><div class="item-desc">接单时间：{{ formatDateTime(item.acceptTime) }}</div></template>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTaskList } from '@/api/engineer'
import { getOrderStatusText, formatDateTime } from '@/utils/dict'
const route = useRoute(); const router = useRouter(); const list = ref([]); const loading = ref(false); const finished = ref(false); const refreshing = ref(false); const page = ref(1)
const labels = { WAIT_ACCEPT: '待接单任务', ACCEPTED: '已接单任务', PROCESSING: '维修中任务', WAIT_SETTLEMENT: '待结算任务', FINISHED: '已完成任务' }
const navLabels = { WAIT_ACCEPT: '待接单', ACCEPTED: '已接单', PROCESSING: '维修中', WAIT_SETTLEMENT: '待结算', FINISHED: '已完成' }
const status = computed(() => String(route.params.status || 'WAIT_ACCEPT')); const statusLabel = computed(() => labels[status.value] || '任务列表')
const statusNavLabel = computed(() => navLabels[status.value] || '任务列表')
const descriptions = { WAIT_ACCEPT: '等待接单处理的维修任务', ACCEPTED: '已经接单、等待开始的维修任务', PROCESSING: '正在处理中的维修任务', WAIT_SETTLEMENT: '维修完成、等待结算的任务', FINISHED: '已经完成的维修任务' }
const statusDescription = computed(() => descriptions[status.value] || '当前状态下的维修任务')
async function load() { loading.value = true; try { const res = await getTaskList({ page: page.value, size: 10, status: status.value }); const rows = res?.data?.list || []; list.value.push(...rows); if (rows.length < 10) finished.value = true; else page.value++ } finally { loading.value = false; finished.value = true } }
function refresh() { list.value = []; page.value = 1; finished.value = false; refreshing.value = false; load() }
function goWorkbench() { router.push('/engineer/workbench') }
</script>
<style scoped src="@/styles/service-list.css"></style>
