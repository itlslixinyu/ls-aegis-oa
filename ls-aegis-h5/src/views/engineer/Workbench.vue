<template>
  <div class="workbench-page">
    <van-nav-bar title="工作台" />

    <section class="welcome-card">
      <div><div class="eyebrow">维修工程师工作台</div><h1>{{ engineerName }}，您好</h1><p>选择任务状态查看工作内容</p></div>
      <div class="workbench-mark"><van-icon name="setting-o" /></div>
    </section>

    <section class="section-block">
      <div class="section-title">任务状态</div>
      <div class="status-grid">
        <button v-for="item in statuses" :key="item.status" class="action-card status-card" :class="`status-card--${item.tone}`" type="button" @click="goTasks(item.status)">
          <span class="action-icon"><span class="status-dot"></span></span><span class="status-card-title"><span>{{ item.label }}</span><b>{{ counts[item.status] || 0 }}</b></span><small>{{ item.help }}</small>
        </button>
      </div>
    </section>

    <section class="section-block quick-block">
      <div class="section-title">快捷入口</div>
      <button class="all-tasks" type="button" @click="goTasks('')"><van-icon name="orders-o" /><span><strong>全部任务</strong><small>查看所有已分配任务</small></span><van-icon name="arrow" /></button>
    </section>

    <van-tabbar route>
      <van-tabbar-item to="/engineer/workbench" icon="apps-o">工作台</van-tabbar-item>
      <van-tabbar-item to="/engineer/profile" icon="manager-o">个人中心</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getTaskList } from '@/api/engineer'

const router = useRouter()
const userStore = useUserStore()
const counts = ref({ WAIT_ACCEPT: 0, ACCEPTED: 0, PROCESSING: 0, WAIT_SETTLEMENT: 0, FINISHED: 0 })
const engineerName = computed(() => userStore.engineerInfo?.user?.nickName || userStore.engineerInfo?.user?.userName || '工程师')
const statuses = [
  { label: '待接单', help: '查看等待接单的任务', status: 'WAIT_ACCEPT', tone: 'orange' },
  { label: '已接单', help: '查看已接受的任务', status: 'ACCEPTED', tone: 'blue' },
  { label: '维修中', help: '继续处理中的维修任务', status: 'PROCESSING', tone: 'purple' },
  { label: '待结算', help: '查看等待结算的任务', status: 'WAIT_SETTLEMENT', tone: 'green' },
  { label: '已完成', help: '查看已完成的维修记录', status: 'FINISHED', tone: 'gray' }
]
function goTasks(status) { router.push(status ? `/engineer/tasks/status/${status}` : '/engineer/tasks') }
onMounted(async () => {
  await Promise.all(statuses.map(async item => {
    try { const res = await getTaskList({ page: 1, size: 1, status: item.status }); counts.value[item.status] = Number(res?.data?.total ?? 0) } catch { counts.value[item.status] = 0 }
  }))
})
</script>

<style scoped>
.workbench-page { min-height: 100vh; padding-bottom: 70px; background: #f4f6fa; color: #1e2b42; }
:deep(.van-nav-bar) { background: transparent; }.welcome-card { margin: 12px 16px 24px; padding: 22px; display: flex; align-items: center; justify-content: space-between; border-radius: 24px; color: #fff; background: linear-gradient(135deg,#315ec4,#668ce8); box-shadow: 0 14px 28px rgba(49,94,196,.22); }
.eyebrow { font-size: 12px; opacity: .78; letter-spacing: .1em; } h1 { margin: 8px 0 6px; font-size: 23px; }.welcome-card p { margin: 0; font-size: 13px; opacity: .82; }.workbench-mark { width: 64px; height: 64px; display: grid; place-items: center; border-radius: 20px; background: rgba(255,255,255,.18); font-size: 32px; }
.section-block { margin: 0 16px 22px; }.section-title { margin-bottom: 10px; font-size: 16px; font-weight: 700; }.status-grid,.action-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 12px; }.action-card { min-height: 112px; padding: 17px; border: 0; border-radius: 18px; color: #fff; text-align: left; box-shadow: 0 8px 18px rgba(35,56,100,.12); }.action-card span:not(.action-icon) { display: block; font-size: 16px; font-weight: 700; }.action-card small { display: block; margin-top: 7px; font-size: 12px; opacity: .8; }.action-icon { display: block; margin-bottom: 8px; font-size: 27px; line-height: 1; }.status-card--orange { background: linear-gradient(145deg,#e99b39,#f3bd67); }.status-card--blue { background: linear-gradient(145deg,#4b7ee1,#79a0ed); }.status-card--purple { background: linear-gradient(145deg,#7359ce,#9a7be2); }.status-card--green { background: linear-gradient(145deg,#34a978,#61c79a); }.status-card--gray { background: linear-gradient(145deg,#7f8a9a,#aab3c0); }.status-dot { display: block; width: 12px; height: 12px; border-radius: 50%; background: rgba(255,255,255,.85); }.all-tasks > .van-icon:first-child { margin-right: 12px; color: #4b7ee1; font-size: 22px; }
.status-card-title { display:flex !important; align-items:center; justify-content:space-between; gap:8px; }.status-card-title b { min-width:28px; padding:3px 7px; border-radius:10px; background:rgba(255,255,255,.2); font-size:15px; text-align:center; }
</style>
