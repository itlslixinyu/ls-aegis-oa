<template>
  <div class="status-page">
    <van-nav-bar :title="statusLabel" left-arrow @click-left="goWorkbench" />
    <div class="status-head"><span class="dot"></span><div><h1>{{ statusLabel }}</h1><p>仅显示当前状态的维修任务</p></div></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <van-empty v-if="!list.length && finished" image="search" :description="`暂无${statusLabel}`" />
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/engineer/task/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.orderNo }}</strong><van-tag type="primary">{{ getOrderStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.address || '未填写维修地址' }}</div><div class="item-desc">接单时间：{{ formatDateTime(item.acceptTime) }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <van-tabbar route><van-tabbar-item to="/engineer/workbench" icon="apps-o">工作台</van-tabbar-item><van-tabbar-item to="/engineer/tasks" icon="list-switching">{{ statusNavLabel }}</van-tabbar-item><van-tabbar-item to="/engineer/profile" icon="manager-o">个人中心</van-tabbar-item></van-tabbar>
  </div>
</template>
<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTaskList } from '@/api/engineer'
import { getOrderStatusText, formatDateTime } from '@/utils/dict'
const route = useRoute(); const router = useRouter(); const list = ref([]); const loading = ref(false); const finished = ref(false); const refreshing = ref(false); const page = ref(1)
const labels = { WAIT_ACCEPT: '待接单任务', ACCEPTED: '已接单任务', PROCESSING: '维修中任务', WAIT_SETTLEMENT: '待结算任务', FINISHED: '已完成任务' }
const navLabels = { WAIT_ACCEPT: '待接单', ACCEPTED: '已接单', PROCESSING: '维修中', WAIT_SETTLEMENT: '待结算', FINISHED: '已完成' }
const status = computed(() => String(route.params.status || 'WAIT_ACCEPT')); const statusLabel = computed(() => labels[status.value] || '任务列表')
const statusNavLabel = computed(() => navLabels[status.value] || '任务列表')
async function load() { loading.value = true; try { const res = await getTaskList({ page: page.value, size: 10, status: status.value }); const rows = res?.data?.list || []; list.value.push(...rows); if (rows.length < 10) finished.value = true; else page.value++ } finally { loading.value = false; finished.value = true } }
function refresh() { list.value = []; page.value = 1; finished.value = false; refreshing.value = false; load() }
function goWorkbench() { router.push('/engineer/workbench') }
</script>
<style scoped>
.status-page { min-height:100vh; padding-bottom:70px; background:#f4f6fa; color:#24324c; }.status-head { margin:10px 16px 18px; padding:22px 20px; display:flex; gap:14px; align-items:center; border-radius:22px; color:#fff; background:linear-gradient(135deg,#315ec4,#6a91e9); box-shadow:0 12px 24px rgba(49,94,196,.2); }.dot { width:12px; height:12px; flex:0 0 auto; border-radius:50%; background:#fff; box-shadow:0 0 0 6px rgba(255,255,255,.16); }.status-head h1 { margin:0; font-size:22px; letter-spacing:.01em; }.status-head p { margin:7px 0 0; color:rgba(255,255,255,.82); font-size:12px; }.list-wrap { min-height:calc(100vh - 190px); }.item-card { margin:10px 16px; border-radius:16px; box-shadow:0 6px 18px rgba(31,48,80,.05); }.item-title { display:flex; justify-content:space-between; align-items:center; }.item-meta,.item-desc { margin-top:7px; color:#7c8798; font-size:12px; }.item-desc { color:#4d5b71; }.status-page :deep(.van-tabbar-item__text) { white-space:nowrap; font-size:12px; }
</style>
