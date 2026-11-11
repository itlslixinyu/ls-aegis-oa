<template>
  <div class="profile-page">
    <van-nav-bar title="我的" />
    <section class="profile-card">
      <div class="profile-avatar">{{ avatarText }}</div>
      <div class="profile-info"><SingleLineHeading class="profile-name" :text="customerName" :max-size="21" /><div class="profile-no">客户编号：{{ customerNo || '-' }}</div></div>
    </section>
    <section class="profile-stats">
      <div class="profile-stat profile-stat--total"><b>{{ stats.total || 0 }}</b><span>全部报修</span></div>
      <div class="profile-stat profile-stat--wait"><b>{{ stats.wait || 0 }}</b><span>待处理</span></div>
      <div class="profile-stat profile-stat--confirm"><b>{{ stats.confirm || 0 }}</b><span>已确认</span></div>
      <div class="profile-stat profile-stat--order"><b>{{ stats.order || 0 }}</b><span>已转工单</span></div>
      <div class="profile-stat profile-stat--cancel"><b>{{ stats.cancel || 0 }}</b><span>已取消</span></div>
    </section>
    <div class="profile-spacer"></div>
    <section class="account-section">
      <div class="block-title">账户</div>
      <van-cell-group inset>
        <van-cell title="关于系统" icon="info-o" is-link @click="showAbout" />
        <van-cell
          title="退出登录"
          icon="cross"
          is-link
          title-class="logout-text"
          @click="onLogout"
        />
      </van-cell-group>
      <div class="version"><p>{{ systemStore.h5Title }} v{{ APP_VERSION }}</p></div>
    </section>

    <!-- 底部导航 -->
    <AppTabbar />
  </div>
</template>

<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import SingleLineHeading from '@/components/SingleLineHeading.vue'
import { APP_VERSION } from '@/utils/version'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showConfirmDialog, showDialog } from 'vant'
import { getRepairList, logout } from '@/api/customer'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

const stats = ref({
  total: 0,
  wait: 0,
  confirm: 0,
  order: 0,
  cancel: 0
})

const customerName = computed(() => {
  const info = userStore.customerInfo
  if (!info) return '客户'
  return info.customerName || info.name || info.contactName || '客户'
})

const customerNo = computed(() => userStore.customerInfo?.customerNo || '')

/** 无头像时用名称首字 */
const avatarText = computed(() => {
  const name = customerName.value
  if (!name || name === '客户') return '客'
  return name.trim().charAt(0)
})

/** 按状态拉取 total 汇总报修统计 */
async function loadStats() {
  const pairs = [
    ['total', undefined],
    ['wait', 'WAIT'],
    ['confirm', 'CONFIRM'],
    ['order', 'ORDER'],
    ['cancel', 'CANCEL']
  ]
  try {
    const results = await Promise.all(
      pairs.map(([, status]) =>
        getRepairList({ page: 1, size: 1, status: status || undefined })
      )
    )
    pairs.forEach(([key], index) => {
      stats.value[key] = Number(results[index]?.data?.total ?? 0)
    })
  } catch (e) {
    // 错误已在拦截器处理
  }
}

function goRepairs() {
  router.push('/customer/repairs')
}

function goApply() {
  router.push('/customer/apply')
}

function goWaitRepairs() {
  router.push({ path: '/customer/repairs', query: { status: 'WAIT' } })
}

function goOrderRepairs() {
  router.push({ path: '/customer/repairs', query: { status: 'ORDER' } })
}

function showAbout() {
  showDialog({
    title: '关于系统',
    message: `${systemStore.systemName}\n客户报修端 v${APP_VERSION}\n${systemStore.footerContent}`
  })
}

async function onLogout() {
  try {
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出登录吗？'
    })
    try {
      if (userStore.h5Token) {
        await logout(userStore.h5Token)
      }
    } catch (e) {
      // 忽略退出接口错误，本地仍清除
    }
    userStore.clearH5Token()
    showSuccessToast('已退出登录')
    setTimeout(() => {
      router.replace('/customer/auth')
    }, 800)
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.profile-page :deep(.van-nav-bar) {
  height: 56px !important;
  line-height: 56px !important;
  background: #FFFFFF;
  border: none !important;
}
.profile-page :deep(.van-nav-bar::after) {
  display: none !important;
}
.profile-page :deep(.van-nav-bar__content) {
  height: 56px !important;
}
.profile-page :deep(.van-nav-bar__title) {
  font-weight: 600 !important;
  font-size: 17px !important;
  color: #0F172A;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 16px;
  background: linear-gradient(135deg, #1989fa 0%, #2d8cf0 100%);
  color: #fff;
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
  word-break: break-all;
}

.user-no {
  font-size: 13px;
  opacity: 0.9;
}

.block-title {
  font-size: 14px;
  color: #969799;
  padding: 16px 16px 8px;
  font-weight: 500;
}

.stats-section {
  background: #fff;
  margin: 8px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #ebedf0;
  margin: 0 8px;
  border-radius: 8px;
  overflow: hidden;
}

.stat-item {
  background: #fff;
  padding: 16px 8px;
  text-align: center;
}

.stat-num {
  font-size: 20px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 4px;
}

.stat-num.warning {
  color: #ff976a;
}

.stat-num.primary {
  color: #1989fa;
}

.stat-num.success {
  color: #07c160;
}

.stat-num.default {
  color: #969799;
}

.stat-label {
  font-size: 12px;
  color: #646566;
}

.menu-section {
  margin-top: 8px;
}

.logout-text {
  color: #ee0a24;
}

.version {
  text-align: center;
  padding: 24px 16px 72px;
  color: #969799;
  font-size: 12px;
}

.profile-page { min-height:100vh; padding-bottom:70px; display:flex; flex-direction:column; background:#f4f6fa; color:#1e2b42; }
.profile-page :deep(.van-nav-bar) { background:transparent; }
.profile-page :deep(.van-nav-bar__title) { font-size:18px !important; font-weight:700 !important; }
.profile-card { margin:12px 16px 0; padding:22px; display:flex; align-items:center; gap:16px; border-radius:24px; color:#fff; background:linear-gradient(135deg,#315ec4,#668ce8); box-shadow:0 14px 28px rgba(49,94,196,.22); }
.profile-avatar { width:58px; height:58px; display:grid; place-items:center; border-radius:18px; background:rgba(255,255,255,.2); font-size:24px; font-weight:700; }
.profile-name { font-size:21px; font-weight:700; }.profile-no { margin-top:7px; font-size:12px; opacity:.82; }.profile-spacer { flex:1; min-height:30px; }.account-section { margin:0 16px; }.account-section .block-title { margin:0 0 10px; font-size:16px; font-weight:700; }.account-section :deep(.van-cell-group) { overflow:hidden; border-radius:18px; }.account-section .version { padding:18px 0 0; }
.profile-stats { margin:14px 16px 0; display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:8px; }.profile-stat { min-height:76px; padding:10px 4px; display:flex; flex-direction:column; align-items:center; justify-content:center; border-radius:14px; background:#fff; box-shadow:0 6px 16px rgba(31,48,80,.06); }.profile-stat b { font-size:20px; line-height:1; }.profile-stat span { margin-top:8px; color:#7b8798; font-size:10px; white-space:nowrap; }.profile-stat--total b { color:#315ec4; }.profile-stat--wait b { color:#e99b39; }.profile-stat--confirm b { color:#4b7ee1; }.profile-stat--order b { color:#34a978; }.profile-stat--cancel b { color:#8b96a8; }
.profile-info { flex: 1; min-width: 0; }
.profile-avatar { flex-shrink: 0; }
</style>
