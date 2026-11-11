<template>
  <div class="workbench-page">
    <van-nav-bar title="工作台" />

    <WelcomeCard eyebrow="维修工程师工作台" :name="engineerName" description="选择任务状态查看工作内容" :logo="systemStore.logoUrl" />

    <section class="section-block">
      <div class="section-title">常用功能</div>
      <button class="action-card action-card--blue all-tasks" type="button" @click="goTasks('')"><van-icon name="orders-o" /><span><strong>全部任务</strong><small>查看所有已分配任务</small></span><van-icon name="arrow" /></button>
    </section>

    <section class="section-block">
      <div class="section-title">任务状态</div>
      <div class="action-grid">
        <button v-for="item in statuses" :key="item.status" class="action-card status-card" :class="`status-card--${item.tone}`" type="button" @click="goTasks(item.status)">
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
import { getTaskList } from '@/api/engineer'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()
const counts = ref({ WAIT_ACCEPT: 0, ACCEPTED: 0, PROCESSING: 0, WAIT_SETTLEMENT: 0, FINISHED: 0 })
const engineerName = computed(() => userStore.engineerInfo?.user?.nickName || userStore.engineerInfo?.user?.userName || '工程师')
const statuses = [
  { label: '待接单', help: '查看等待接单的任务', status: 'WAIT_ACCEPT', icon: 'clock-o', tone: 'orange' },
  { label: '已接单', help: '查看已接受的任务', status: 'ACCEPTED', icon: 'todo-list-o', tone: 'blue' },
  { label: '维修中', help: '继续处理中的维修任务', status: 'PROCESSING', icon: 'setting-o', tone: 'purple' },
  { label: '待结算', help: '查看等待结算的任务', status: 'WAIT_SETTLEMENT', icon: 'balance-list-o', tone: 'green' },
  { label: '已完成', help: '查看已完成的维修记录', status: 'FINISHED', icon: 'completed', tone: 'gray' }
]
function goTasks(status) { router.push(status ? `/engineer/tasks/status/${status}` : '/engineer/tasks') }
onMounted(async () => {
  await Promise.all(statuses.map(async item => {
    try { const res = await getTaskList({ page: 1, size: 1, status: item.status }); counts.value[item.status] = Number(res?.data?.total ?? 0) } catch { counts.value[item.status] = 0 }
  }))
})
</script>

<style scoped src="@/styles/workbench.css"></style>
