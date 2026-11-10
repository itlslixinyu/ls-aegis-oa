<template>
  <div class="all-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="all-head"><h1>我的报修</h1><p>查看你提交的全部报修记录</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished" class="empty-panel"><van-empty image="search" description="暂无报修记录" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/customer/repair/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.repairNo }}</strong><van-tag :type="tagType(item.status)">{{ getApplyStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.faultType || '未填写故障类型' }} · {{ formatDateTime(item.applyTime) }}</div><div class="item-desc">{{ item.faultDesc || '暂无描述' }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <van-tabbar route><van-tabbar-item to="/customer/workbench" icon="apps-o">工作台</van-tabbar-item><van-tabbar-item to="/customer/repairs/all" icon="orders-o">我的报修</van-tabbar-item><van-tabbar-item to="/customer/profile" icon="manager-o">我的</van-tabbar-item></van-tabbar>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRepairList } from '@/api/customer'
import { getApplyStatusText, formatDateTime } from '@/utils/dict'
const router = useRouter(); const list = ref([]); const loading = ref(false); const finished = ref(false); const refreshing = ref(false); const page = ref(1)
function tagType(status) { return ({ WAIT: 'warning', CONFIRM: 'primary', ORDER: 'success', CANCEL: 'default' })[status] || 'default' }
async function load() { loading.value = true; try { const res = await getRepairList({ page: page.value, size: 10 }); const rows = res?.data?.list || []; list.value.push(...rows); if (rows.length < 10) finished.value = true; else page.value++ } finally { loading.value = false; finished.value = true } }
function refresh() { list.value = []; page.value = 1; finished.value = false; refreshing.value = false; load() }
function goWorkbench() { router.push('/customer/workbench') }
</script>
<style scoped>
.all-page { min-height:100vh; padding-bottom:70px; background:#f4f6fa; color:#24324c; }.all-head { margin:10px 16px 8px; padding:22px 20px; text-align:center; border-radius:22px; color:#fff; background:linear-gradient(135deg,#7359ce,#9a7be2); box-shadow:0 12px 24px rgba(115,89,206,.2); }.all-head h1 { margin:0; font-size:22px; }.all-head p { margin:8px 0 0; color:rgba(255,255,255,.84); font-size:12px; }.list-wrap { min-height:calc(100vh - 190px); margin:0 16px; padding:8px 0 24px; background:#fff; border-radius:20px; }.empty-panel { min-height:calc(100vh - 260px); display:flex; align-items:flex-start; justify-content:center; padding:24px 8px; }.empty-panel :deep(.van-empty) { padding:32px 0; }.item-card { margin:10px 16px; border-radius:16px; box-shadow:0 6px 18px rgba(31,48,80,.05); }.item-title { display:flex; align-items:center; justify-content:space-between; }.item-meta,.item-desc { margin-top:7px; color:#7c8798; font-size:12px; }.item-desc { color:#4d5b71; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
</style>
