<template>
  <div class="workbench-page">
    <van-nav-bar title="工作台" />

    <WelcomeCard eyebrow="客户服务中心" :name="customerName" description="选择下面的功能开始使用" :logo="systemStore.logoUrl" />

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
          <span class="action-icon" aria-hidden="true"><van-icon :name="item.icon" :size="22" /></span><span class="status-card-title"><span>{{ item.label }}</span><b>{{ counts[item.status] || 0 }}</b></span><small>{{ item.help }}</small>
        </button>
      </div>
    </section>

    <AppTabbar />
  </div>
</template>

<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'
import WelcomeCard from '@/components/WelcomeCard.vue'
import { getRepairList } from '@/api/customer'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()
const counts = ref({ WAIT: 0, CONFIRM: 0, ORDER: 0, CANCEL: 0 })
const customerName = computed(() => userStore.customerInfo?.customerName || userStore.customerInfo?.name || '客户')
const statuses = [
  { label: '等待接单', help: '等待接单与服务安排', status: 'WAIT', icon: 'clock-o', tone: 'orange' },
  { label: '已确认', help: '查看已确认记录', status: 'CONFIRM', icon: 'passed', tone: 'blue' },
  { label: '已转工单', help: '查看维修进度', status: 'ORDER', icon: 'records-o', tone: 'green' },
  { label: '已取消', help: '查看已取消记录', status: 'CANCEL', icon: 'close', tone: 'gray' }
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

<style scoped src="@/styles/workbench.css"></style>
