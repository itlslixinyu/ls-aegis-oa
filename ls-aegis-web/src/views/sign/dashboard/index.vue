<template>
  <GiPageLayout>
    <a-spin :loading="loading" style="width: 100%">
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col v-for="item in statCards" :key="item.label" :span="6">
          <a-card :bordered="false" class="stat-card" :style="{ background: item.bg }">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="12">
          <a-card title="项目状态分布" :bordered="false">
            <template #extra>
              <span class="sub">共 {{ stats.projectCount || 0 }} 个项目</span>
            </template>
            <div v-if="projectStatusList.length">
              <div v-for="item in projectStatusList" :key="item.label" class="progress-item">
                <span class="progress-label">{{ item.label }}</span>
                <a-progress :percent="item.percent" :color="item.color" />
                <span class="progress-count">{{ item.count }}</span>
              </div>
            </div>
            <a-empty v-else description="暂无数据" />
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="印章类型分布" :bordered="false">
            <template #extra>
              <span class="sub">共 {{ stats.sealCount || 0 }} 个印章</span>
            </template>
            <div v-if="sealCategoryList.length">
              <div v-for="item in sealCategoryList" :key="item.label" class="progress-item">
                <span class="progress-label">{{ item.label }}</span>
                <a-progress :percent="item.percent" />
                <span class="progress-count">{{ item.count }}</span>
              </div>
            </div>
            <a-empty v-else description="暂无数据" />
          </a-card>
        </a-col>
      </a-row>

      <a-card title="最近签章记录" :bordered="false">
        <template #extra>
          <a-button type="outline" @click="$router.push('/sign/log')">查看全部</a-button>
        </template>
        <a-table
          :columns="recentColumns"
          :data="stats.recentSigns || []"
          :pagination="false"
          row-key="recordId"
        >
          <template #signTime="{ record }">
            {{ formatSignTime(record.signTime) }}
          </template>
          <template #empty>
            <a-empty description="暂无签章记录" />
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { type DashboardStats, getDashboardStats } from '@/apis/sign'
import { useDict } from '@/hooks/app'
import { findDictLabel, findDictTagColor } from '@/utils/dict'

defineOptions({ name: 'SignDashboard' })

const stats = ref<DashboardStats>({})
const loading = ref(false)
const { sign_project_status } = useDict('sign_project_status')

const statCards = computed(() => [
  { label: '签章项目', value: stats.value.projectCount || 0, bg: 'linear-gradient(135deg, #409eff, #1890ff)' },
  { label: '印章总数', value: stats.value.sealCount || 0, bg: 'linear-gradient(135deg, #67c23a, #2ba35a)' },
  { label: '本月签章', value: stats.value.monthSignCount || 0, bg: 'linear-gradient(135deg, #e6a23c, #d68910)' },
  { label: '授权用户', value: stats.value.userCount || 0, bg: 'linear-gradient(135deg, #f56c6c, #d9363e)' },
])

const projectStatusList = computed(() => {
  const map = stats.value.projectStatusStats || {}
  const total = stats.value.projectCount || 1
  return Object.entries(map).map(([status, count]) => ({
    label: findDictLabel(sign_project_status.value, status, status),
    count,
    percent: Math.round((count / total) * 100),
    color: findDictTagColor(sign_project_status.value, status),
  }))
})

const sealCategoryList = computed(() => {
  const map = stats.value.sealCategoryStats || {}
  const total = stats.value.sealCount || 1
  return Object.entries(map).map(([category, count]) => ({
    label: category,
    count,
    percent: Math.round((count / total) * 100),
  }))
})

const recentColumns: TableInstance['columns'] = [
  { title: '记录ID', dataIndex: 'recordId', ellipsis: true, tooltip: true },
  { title: '项目名称', dataIndex: 'projectName', ellipsis: true, tooltip: true },
  { title: '印章名称', dataIndex: 'sealName' },
  { title: '操作人', dataIndex: 'operatorName' },
  { title: '签章时间', dataIndex: 'signTime', slotName: 'signTime' },
]

const formatSignTime = (val?: string) => {
  if (!val) return '-'
  const d = new Date(val)
  if (Number.isNaN(d.getTime())) return val
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
  } finally {
    loading.value = false
  }
}

onMounted(() => loadStats())
</script>

<style scoped>
.stat-card {
  color: #fff;
  border-radius: 10px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
}
.stat-label {
  margin-top: 6px;
  opacity: 0.9;
  font-size: 14px;
}
.sub {
  color: var(--color-text-3);
  font-size: 12px;
}
.progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.progress-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
}
.progress-item :deep(.arco-progress) {
  flex: 1;
}
.progress-count {
  width: 40px;
  text-align: right;
  font-size: 13px;
}
</style>
