<template>
  <div v-if="visible" class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>签章汇总</span>
      </div>
      <a-link @click="goDashboard">看板</a-link>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goProjects">
          <div class="summary-label">项目</div>
          <div class="stat-value">{{ stats.projectCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goSeals">
          <div class="summary-label">印章</div>
          <div class="stat-value">{{ stats.sealCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goLogs">
          <div class="summary-label">本月签章</div>
          <div class="stat-value">{{ stats.monthSignCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goDashboard">
          <div class="summary-label">用户</div>
          <div class="stat-value">{{ stats.userCount ?? 0 }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type DashboardStats, getDashboardStats } from '@/apis/sign/dashboard'
import has from '@/utils/has'

defineOptions({ name: 'BizSignSummary' })

const emit = defineEmits<{
  loaded: [stats: DashboardStats | null]
}>()

const router = useRouter()
const visible = ref(false)
const stats = ref<DashboardStats>({})

const goDashboard = () => router.push('/sign/dashboard')
const goProjects = () => router.push('/sign/project')
const goSeals = () => router.push('/sign/seal')
const goLogs = () => router.push('/sign/log')

const load = async () => {
  if (!has.hasPermOr(['sign:dashboard:stats', 'sign:dashboard:view'])) {
    visible.value = false
    emit('loaded', null)
    return
  }
  try {
    const { data } = await getDashboardStats()
    stats.value = data || {}
    visible.value = true
    emit('loaded', stats.value)
  } catch {
    visible.value = false
    emit('loaded', null)
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
