<template>
  <div class="all-page pool-page">
    <van-nav-bar title="工单池" />
    <header class="all-head"><h1>可领取的报修</h1><p>报修满10分钟仍未接单后开放，领取成功即进入我的任务</p></header>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" :finished="finished" :error="failed" error-text="暂未加载成功，点击重试" :finished-text="list.length ? '没有更多了' : ''" @update:error="failed = $event" @load="load">
        <van-empty v-if="!list.length && finished && !failed" image="search" description="暂无可领取的报修" />
        <article v-for="item in list" :key="item.applyId" class="pool-card">
          <div class="pool-card__head"><strong>{{ item.repairNo }}</strong><van-tag plain type="primary">等待接单</van-tag></div>
          <div class="pool-meta">{{ item.faultType || '设备报修' }} · {{ formatDateTime(item.applyTime) }}</div>
          <p class="pool-description">{{ item.faultDesc || '暂无故障描述' }}</p>
          <van-button block round type="primary" size="small" :loading="claiming === item.applyId" :disabled="!!claiming" @click="claim(item)">领取工单</van-button>
        </article>
      </van-list>
    </van-pull-refresh>
    <AppTabbar />
  </div>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, closeDialog } from 'vant'
import AppTabbar from '@/components/AppTabbar.vue'
import { getPoolList, claimPoolTask } from '@/api/engineer'
import { formatDateTime } from '@/utils/dict'
const router = useRouter()
const list = ref([]), loading = ref(false), refreshing = ref(false), finished = ref(false), failed = ref(false), claiming = ref('')
let page = 1, pending = false, active = true, generation = 0, timer, dialogOpen = false
async function load() {
  if (pending || !active) return
  const current = generation
  pending = true; loading.value = true; failed.value = false
  try {
    const response = await getPoolList({ page, size: 10 })
    if (!active || current !== generation) return
    const rows = response?.data?.list || []
    const seen = new Set(list.value.map(item => item.applyId))
    list.value.push(...rows.filter(item => !seen.has(item.applyId)))
    finished.value = rows.length < 10
    if (!finished.value) page++
  } catch {
    if (active && current === generation) failed.value = true
  } finally {
    pending = false
    if (active && current === generation) { loading.value = false; refreshing.value = false }
  }
}
function refresh() {
  if (pending || claiming.value) { refreshing.value = false; return }
  generation++; list.value = []; page = 1; finished.value = false
  return load()
}
async function claim(item) {
  if (claiming.value || !active) return
  claiming.value = item.applyId
  try {
    dialogOpen = true
    await showConfirmDialog({ title: '领取工单', message: `确认领取 ${item.repairNo}？领取后请按任务要求安排服务。` })
    dialogOpen = false
    if (!active) return
    const response = await claimPoolTask(item.applyId)
    if (!active) return
    const orderId = response?.data?.orderId
    if (orderId) { showSuccessToast('领取成功'); router.push(`/engineer/task/${orderId}`) }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && active) {
      claiming.value = ''; await refresh()
    }
  } finally { dialogOpen = false; if (active) claiming.value = '' }
}
onMounted(() => { timer = setInterval(() => { if (document.visibilityState === 'visible' && window.scrollY < 50) refresh() }, 30000) })
onBeforeUnmount(() => { active = false; generation++; clearInterval(timer); if (dialogOpen) closeDialog() })
</script>
<style scoped src="@/styles/service-list.css"></style>
<style scoped>
.pool-page .all-head { margin-bottom: 16px; }
.pool-card { margin: 0 0 12px; padding: 16px; background: #fff; border: 1px solid #e8edf5; border-radius: 16px; }
.pool-card__head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.pool-meta { color: #8893a5; font-size: 12px; margin-top: 10px; }
.pool-description { font-size: 14px; line-height: 1.7; color: #475569; overflow-wrap: anywhere; white-space: pre-wrap; }
</style>
