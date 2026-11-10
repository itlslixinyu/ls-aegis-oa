<template>
  <div class="workbench-page">
    <van-nav-bar title="工作台" />

    <section class="welcome-card">
      <div>
        <div class="eyebrow">客户服务中心</div>
        <h1>{{ customerName }}，您好</h1>
        <p>选择下面的功能开始使用</p>
      </div>
      <SystemLogo :src="systemStore.logoUrl" class="workbench-logo" />
    </section>

    <section class="section-block">
      <div class="section-title">常用功能</div>
      <div class="action-grid action-grid--primary">
        <button class="action-card action-card--blue" type="button" @click="goApply">
          <span class="action-icon">＋</span><span>在线报修</span><small>提交新的报修申请</small>
        </button>
        <button class="action-card action-card--purple" type="button" @click="goRepairs('')">
          <span class="action-icon">▤</span><span>我的报修</span><small>查看全部报修记录</small>
        </button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">报修状态</div>
      <div class="action-grid">
        <button v-for="item in statuses" :key="item.status" class="action-card status-card" :class="`status-card--${item.tone}`" type="button" @click="goRepairs(item.status)">
          <span class="action-icon"><span class="status-dot"></span></span><span class="status-card-title"><span>{{ item.label }}</span><b>{{ counts[item.status] || 0 }}</b></span><small>{{ item.help }}</small>
        </button>
      </div>
    </section>

    <van-tabbar route>
      <van-tabbar-item to="/customer/workbench" icon="apps-o">工作台</van-tabbar-item>
      <van-tabbar-item to="/customer/profile" icon="manager-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'
import SystemLogo from '@/components/SystemLogo.vue'
import { getRepairList } from '@/api/customer'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()
const counts = ref({ WAIT: 0, CONFIRM: 0, ORDER: 0, CANCEL: 0 })
const customerName = computed(() => userStore.customerInfo?.customerName || userStore.customerInfo?.name || '客户')
const statuses = [
  { label: '待处理', help: '等待平台处理', status: 'WAIT', tone: 'orange' },
  { label: '已确认', help: '查看已确认记录', status: 'CONFIRM', tone: 'blue' },
  { label: '已转工单', help: '查看维修进度', status: 'ORDER', tone: 'green' },
  { label: '已取消', help: '查看已取消记录', status: 'CANCEL', tone: 'gray' }
]
function goApply() { router.push('/customer/apply') }
function goRepairs(status) { router.push(status ? `/customer/repairs/status/${status}` : '/customer/repairs/all') }
onMounted(async () => {
  await Promise.all(statuses.map(async item => {
    try {
      const res = await getRepairList({ page: 1, size: 1, status: item.status })
      counts.value[item.status] = Number(res?.data?.total ?? 0)
    } catch { counts.value[item.status] = 0 }
  }))
})
</script>

<style scoped>
.workbench-page { min-height: 100vh; padding-bottom: 70px; background: #f4f6fa; color: #1e2b42; }
:deep(.van-nav-bar) { background: transparent; }
:deep(.van-nav-bar__title) { font-size: 18px; font-weight: 700; }
.welcome-card { margin: 12px 16px 24px; padding: 22px; display: flex; align-items: center; justify-content: space-between; border-radius: 24px; color: #fff; background: linear-gradient(135deg, #315ec4, #668ce8); box-shadow: 0 14px 28px rgba(49,94,196,.22); }
.eyebrow { font-size: 12px; opacity: .78; letter-spacing: .1em; }
h1 { margin: 8px 0 6px; font-size: 23px; }
.welcome-card p { margin: 0; font-size: 13px; opacity: .82; }
.workbench-logo { width: 70px; height: 70px; object-fit: contain; }
.section-block { margin: 0 16px 22px; }
.section-title { margin-bottom: 10px; font-size: 16px; font-weight: 700; }
.action-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 12px; }
.action-card { min-height: 122px; padding: 17px; border: 0; border-radius: 18px; color: #fff; text-align: left; box-shadow: 0 8px 18px rgba(35,56,100,.12); }
.action-card--blue { background: linear-gradient(145deg,#3f78df,#5e95ee); }
.action-card--purple { background: linear-gradient(145deg,#7359ce,#9a7be2); }
.action-card span:not(.action-icon) { display: block; font-size: 16px; font-weight: 700; }
.action-card small { display: block; margin-top: 7px; font-size: 12px; opacity: .8; }
.action-icon { display: block; margin-bottom: 8px; font-size: 27px; line-height: 1; }
.status-card { min-height: 104px; }.status-card--orange { background: linear-gradient(145deg,#e99b39,#f3bd67); }.status-card--blue { background: linear-gradient(145deg,#4b7ee1,#79a0ed); }.status-card--green { background: linear-gradient(145deg,#34a978,#61c79a); }.status-card--gray { background: linear-gradient(145deg,#7f8a9a,#aab3c0); }.status-dot { display: block; width: 12px; height: 12px; border-radius: 50%; background: rgba(255,255,255,.85); }
.status-card-title { display:flex !important; align-items:center; justify-content:space-between; gap:8px; }.status-card-title b { min-width:28px; padding:3px 7px; border-radius:10px; background:rgba(255,255,255,.2); font-size:15px; text-align:center; }
</style>
