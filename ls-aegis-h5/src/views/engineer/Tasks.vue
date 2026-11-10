<template>
  <div class="page">
    <van-nav-bar title="我的任务" />

    <section class="page-hero">
      <div>
        <div class="hero-kicker">工作台</div>
        <h1>我的任务</h1>
        <p>查看待接单、维修中和已完成的任务</p>
      </div>
      <div class="hero-icon"><van-icon name="records" /></div>
    </section>

    <!-- 工程师信息条（原生实现，避开Vant NoticeBar滚动逻辑造成文字消失问题，用户确认蓝色信息条风格） -->
    <div class="engineer-bar">
      <span class="engineer-bar__icon">👤</span>
      <span class="engineer-bar__label">工程师：</span>
      <span class="engineer-bar__name">{{ engineerName }}</span>
    </div>

    <div class="quick-actions">
      <van-button round type="primary" icon="replay" @click="onRefresh">刷新任务</van-button>
      <van-button round plain type="primary" icon="clock-o" @click="activeTab = 'WAIT_ACCEPT'; onTabChange()">待接单</van-button>
    </div>

    <van-tabs v-model:active="activeTab" @change="onTabChange" sticky>
      <van-tab title="全部" name="" />
      <van-tab title="待接单" name="WAIT_ACCEPT" />
      <van-tab title="已接单" name="ACCEPTED" />
      <van-tab title="维修中" name="PROCESSING" />
      <van-tab title="待结算" name="WAIT_SETTLEMENT" />
      <van-tab title="已完成" name="FINISHED" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="list-container">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-if="list.length === 0 && finished" class="empty-tip">
          <van-empty image="search" description="当前没有待处理任务">
            <van-button round type="primary" size="small" @click="onRefresh">重新加载</van-button>
          </van-empty>
        </div>

        <van-cell
          v-for="item in list"
          :key="item.id"
          is-link
          class="task-card"
          @click="goDetail(item.id)"
        >
          <template #title>
            <div class="card-header">
              <span class="order-no">{{ item.orderNo }}</span>
              <van-tag :type="getTagType(item.status)" size="medium">
                {{ getOrderStatusText(item.status) }}
              </van-tag>
            </div>
          </template>
          <template #label>
            <div class="card-body">
              <div class="info-row">
                <span class="label">维修地址：</span>
                <span>{{ item.address || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="label">接单时间：</span>
                <span>{{ formatDateTime(item.acceptTime) }}</span>
              </div>
              <div class="info-row" v-if="item.startTime">
                <span class="label">开始时间：</span>
                <span>{{ formatDateTime(item.startTime) }}</span>
              </div>
            </div>
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>

    <!-- 底部导航 -->
    <van-tabbar v-model="activeTabBar" route>
      <van-tabbar-item to="/engineer/workbench" icon="apps-o">工作台</van-tabbar-item>
      <van-tabbar-item to="/engineer/tasks" icon="list-switching">任务列表</van-tabbar-item>
      <van-tabbar-item to="/engineer/profile" icon="manager-o">个人中心</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTaskList } from '@/api/engineer'
import { useUserStore } from '@/store/user'
import { getOrderStatusText, formatDateTime, orderStatusMap } from '@/utils/dict'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/**
 * 工程师姓名：多层字段 fallback
 *  - 登录后按 { user, roles, permissions } 结构存入，优先从 info.user.* 读取
 *  - 兼容 info 顶层直接是 user 对象的异常场景
 *  - 覆盖大小写 + 常见字段名：nickName/nickname/userName/username/name/realName
 */
const engineerName = computed(() => {
  const info = userStore.engineerInfo
  if (!info) return '工程师'

  const u = info.user
  if (u) {
    const name = u.nickName || u.nickname || u.userName || u.username || u.name || u.realName
    if (name) return name
  }

  const topName = info.nickName || info.nickname || info.userName || info.username || info.name || info.realName
  if (topName) return topName

  return '工程师'
})

const activeTabBar = ref(1)
const activeTab = ref('')
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 10

function getTagType(status) {
  return orderStatusMap[status]?.tagType || 'default'
}

async function onLoad() {
  loading.value = true
  try {
    const res = await getTaskList({
      page: pageNum.value,
      size: pageSize,
      status: activeTab.value || undefined
    })
    const rows = res?.data?.list || []
    if (rows.length) {
      list.value.push(...rows)
    }
    if (rows.length < pageSize) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
  refreshing.value = false
}

function onTabChange() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function goDetail(id) {
  router.push(`/engineer/task/${id}`)
}

// 从query初始化状态筛选
onMounted(() => {
  const status = route.query.status
  if (status && typeof status === 'string') {
    activeTab.value = status
  }
})
</script>

<style scoped>
/* ===== 顶部：van-nav-bar 高度与 customer/repairs 统一为 56px，避免两个入口页面导航高度视觉不一致 ===== */
.page :deep(.van-nav-bar) {
  border: none !important;
  background: #FFFFFF;
  height: 56px !important;
  line-height: 56px !important;
}
.page :deep(.van-nav-bar::after) {
  display: none !important;   /* 去掉Vant默认下边框白线 */
}
.page :deep(.van-nav-bar__content) {
  height: 56px !important;
}
.page :deep(.van-nav-bar__title) {
  font-weight: 600 !important;
  font-size: 17px !important;
  color: #0F172A;
}

/* ===== 工程师信息条（原生实现，用户确认蓝色风格） ===== */
.engineer-bar {
  margin: 0 12px 10px;
  padding: 13px 16px;
  background: #E6F4FF;
  border: 1px solid #91CAFF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  line-height: 1.5;
}
.engineer-bar__icon {
  font-size: 16px;
  flex-shrink: 0;
}
.engineer-bar__label {
  color: #1677FF;
  font-weight: 500;
  flex-shrink: 0;
}
.engineer-bar__name {
  color: #0F172A;
  font-weight: 600;
  word-break: break-all;
}

/* ===== Tabs：去掉顶部白线与nav-bar的白线呼应，tab下划线用科技蓝主题 ===== */
.page :deep(.van-tabs) {
  background: #FFFFFF;
}
.page :deep(.van-tabs::before),
.page :deep(.van-tabs::after) {
  display: none !important;  /* 去除Vant默认上下边框白线 */
}
.page :deep(.van-tabs__line) {
  background-color: #1677FF !important;
  height: 3px !important;
  border-radius: 2px !important;
}
.page :deep(.van-tab--active) {
  color: #1677FF !important;
  font-weight: 600 !important;
}
.page :deep(.van-tab) {
  color: #64748B !important;
  font-weight: 500;
  font-size: 14px;
}

.list-container {
  min-height: calc(100vh - 200px);
  padding-bottom: 50px;
}

.task-card {
  margin: 8px 12px;
  border-radius: 8px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-weight: 600;
  color: #323233;
}

.card-body {
  padding-top: 4px;
}

.info-row {
  display: flex;
  font-size: 13px;
  color: #646566;
  margin-top: 4px;
}

.info-row .label {
  color: #969799;
  flex-shrink: 0;
  width: 75px;
}
.page { min-height: 100vh; background: #f4f6fa; }
.page :deep(.van-nav-bar) { background: #f4f6fa !important; }
.page :deep(.van-nav-bar__title) { color: #1c2940 !important; font-size: 19px !important; font-weight: 700 !important; }
.engineer-bar { margin: 4px 16px 12px; padding: 13px 16px; border: 0; border-radius: 16px; background: linear-gradient(110deg,#e8efff,#f1f4ff); box-shadow: 0 5px 16px rgba(45,73,130,.06); }
.engineer-bar__icon { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 11px; background: #fff; }
.engineer-bar__label { color: #637aa9; }
.engineer-bar__name { color: #24324c; }
.page :deep(.van-tabs) { background: #f4f6fa; }
.page :deep(.van-tabs__wrap) { margin: 0 12px 8px; border-radius: 14px; background: #fff; box-shadow: 0 4px 14px rgba(27,44,76,.04); }
.page :deep(.van-tabs__line) { background: #426bd0 !important; border-radius: 8px; }
.page :deep(.van-tab--active) { color: #315ec4 !important; font-weight: 650; }
.task-card { margin: 10px 14px; border: 1px solid #edf0f5; border-radius: 18px; box-shadow: 0 7px 22px rgba(31,48,80,.055); }
.task-card :deep(.van-cell) { padding: 16px; }
.order-no { color: #24324c; font-size: 15px; }
.info-row { color: #536078; margin-top: 7px; }
.info-row .label { color: #98a2b3; }
.page :deep(.van-tabbar) { border-radius: 20px 20px 0 0; box-shadow: 0 -5px 24px rgba(28,45,78,.07); padding-bottom: env(safe-area-inset-bottom); }
.page-hero { margin: 4px 16px 12px; padding: 20px; display: flex; align-items: center; justify-content: space-between; border-radius: 22px; color: #fff; background: linear-gradient(135deg, #315ec4 0%, #628be8 100%); box-shadow: 0 12px 26px rgba(49,94,196,.22); }
.hero-kicker { margin-bottom: 6px; font-size: 12px; opacity: .78; letter-spacing: .08em; }
.page-hero h1 { margin: 0; font-size: 24px; line-height: 1.2; letter-spacing: -.02em; }
.page-hero p { margin: 8px 0 0; font-size: 13px; opacity: .84; }
.hero-icon { width: 54px; height: 54px; display: grid; place-items: center; border-radius: 18px; background: rgba(255,255,255,.18); font-size: 28px; }
.quick-actions { display: flex; gap: 10px; margin: 0 16px 12px; }
.quick-actions .van-button { flex: 1; height: 42px; font-weight: 650; }
.empty-tip { margin: 18px 16px; padding: 24px 12px 28px; border-radius: 20px; background: rgba(255,255,255,.72); }
.empty-tip :deep(.van-empty) { padding: 14px 0 0; }
</style>
