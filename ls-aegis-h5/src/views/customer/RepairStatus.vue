<template>
  <div class="status-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="status-head" :class="`status-head--${status}`"><h1>{{ statusLabel }}</h1><p>{{ statusDescription }}</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished" class="empty-panel"><van-empty image="search" :description="`暂无${statusLabel}记录`" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/customer/repair/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.repairNo }}</strong><van-tag type="primary">{{ getApplyStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.faultType || '未填写故障类型' }} · {{ formatDateTime(item.applyTime) }}</div><div class="item-desc">{{ item.faultDesc || '暂无描述' }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <van-tabbar route><van-tabbar-item to="/customer/workbench" icon="apps-o">工作台</van-tabbar-item><van-tabbar-item :to="`/customer/repairs/status/${status}`" icon="orders-o">{{ statusNavLabel }}</van-tabbar-item><van-tabbar-item to="/customer/profile" icon="manager-o">我的</van-tabbar-item></van-tabbar>
  </div>
</template>
<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRepairList } from '@/api/customer'
import { getApplyStatusText, formatDateTime } from '@/utils/dict'
const route = useRoute(); const router = useRouter(); const list = ref([]); const loading = ref(false); const finished = ref(false); const refreshing = ref(false); const page = ref(1)
const labels = { WAIT: '待处理报修', CONFIRM: '已确认报修', ORDER: '已转工单', CANCEL: '已取消报修' }
const navLabels = { WAIT: '待处理', CONFIRM: '已确认', ORDER: '已转工单', CANCEL: '已取消' }
const descriptions = { WAIT: '等待平台确认并安排处理的报修', CONFIRM: '平台已经确认的报修申请', ORDER: '已经转入维修工单的报修', CANCEL: '已经取消或关闭的报修申请' }
const status = computed(() => String(route.params.status || 'WAIT')); const statusLabel = computed(() => labels[status.value] || '报修记录')
const statusNavLabel = computed(() => navLabels[status.value] || '当前状态')
const statusDescription = computed(() => descriptions[status.value] || '当前状态下的报修记录')
async function load() { loading.value = true; try { const res = await getRepairList({ page: page.value, size: 10, status: status.value }); const rows = res?.data?.list || []; list.value.push(...rows); if (rows.length < 10) finished.value = true; else page.value++ } finally { loading.value = false; finished.value = true } }
function refresh() { list.value = []; page.value = 1; finished.value = false; refreshing.value = false; load() }
function goWorkbench() { router.push('/customer/workbench') }
</script>
<style scoped>
.status-page { min-height:100vh; padding-bottom:70px; background:#f4f6fa; color:#24324c; }.status-head { margin:10px 16px 8px; padding:22px 20px 20px; display:flex; flex-direction:column; align-items:center; justify-content:center; border-radius:22px; color:#fff; box-shadow:0 12px 24px rgba(49,94,196,.2); }.status-head--WAIT { background:linear-gradient(135deg,#e99b39,#f3bd67); box-shadow:0 12px 24px rgba(233,155,57,.2); }.status-head--CONFIRM { background:linear-gradient(135deg,#4b7ee1,#79a0ed); }.status-head--ORDER { background:linear-gradient(135deg,#34a978,#61c79a); box-shadow:0 12px 24px rgba(52,169,120,.2); }.status-head--CANCEL { background:linear-gradient(135deg,#7f8a9a,#aab3c0); box-shadow:0 12px 24px rgba(127,138,154,.2); }.status-head h1 { margin:0; font-size:22px; letter-spacing:.01em; text-align:center; }.status-head p { margin:8px 0 0; color:rgba(255,255,255,.84); font-size:12px; text-align:center; }.list-wrap { min-height:calc(100vh - 220px); margin:0 16px; padding:8px 0 24px; background:#fff; border-radius:20px; }.empty-panel { min-height:calc(100vh - 260px); margin:0; padding:24px 8px; display:flex; align-items:flex-start; justify-content:center; background:#fff; }.empty-panel :deep(.van-empty) { padding:32px 0; }.item-card { margin:10px 16px; border-radius:16px; box-shadow:0 6px 18px rgba(31,48,80,.05); }.item-title { display:flex; justify-content:space-between; align-items:center; }.item-meta,.item-desc { margin-top:7px; color:#7c8798; font-size:12px; }.item-desc { color:#4d5b71; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
</style>
