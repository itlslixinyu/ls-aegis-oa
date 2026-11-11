<template>
  <div class="engineer-profile-page">
    <van-nav-bar title="我的" />

    <section class="profile-card"><div class="profile-avatar">{{ engineerName.slice(0, 1) }}</div><div class="profile-info"><SingleLineHeading class="profile-name" :text="engineerName" :max-size="21" /><div class="profile-no">{{ engineerDept }}</div></div></section>

    <section class="profile-stats"><div class="profile-stat"><b>{{ stats.total || 0 }}</b><span>总任务</span></div><div class="profile-stat profile-stat--orange"><b>{{ stats.waitAccept || 0 }}</b><span>待接单</span></div><div class="profile-stat profile-stat--blue"><b>{{ stats.accepted || 0 }}</b><span>已接单</span></div><div class="profile-stat profile-stat--purple"><b>{{ stats.processing || 0 }}</b><span>维修中</span></div><div class="profile-stat profile-stat--green"><b>{{ stats.waitSettlement || 0 }}</b><span>待结算</span></div><div class="profile-stat profile-stat--green"><b>{{ stats.finished || 0 }}</b><span>已完成</span></div><div class="profile-stat profile-stat--gray"><b>{{ stats.closed || 0 }}</b><span>已关闭</span></div></section>

    <div class="profile-spacer"></div>

    <!-- 账户操作 -->
    <div class="account-section">
      <div class="block-title">账户</div>
      <van-cell-group inset>
        <van-cell title="修改密码" icon="lock" is-link @click="showComingSoon" />
        <van-cell title="意见反馈" icon="comment-o" is-link @click="showComingSoon" />
        <van-cell title="关于系统" icon="info-o" is-link @click="showAbout" />
        <van-cell
          title="退出登录"
          icon="cross"
          is-link
          title-class="logout-text"
          @click="onLogout"
        />
      </van-cell-group>
    </div>

    <div class="version">
      <p>{{ systemStore.h5Title }} v{{ APP_VERSION }}</p>
    </div>

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
import { showSuccessToast, showFailToast, showConfirmDialog, showDialog } from 'vant'
import { getProfile, logout } from '@/api/engineer'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'
import { getRoleLabel } from '@/utils/dict'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

const stats = ref({})

const engineerName = computed(() =>
  userStore.engineerInfo?.user?.nickName ||
  userStore.engineerInfo?.user?.userName ||
  '工程师'
)

const engineerDept = computed(() => {
  const dept = userStore.engineerInfo?.user?.dept
  return dept ? dept.deptName : '运维部门'
})

const roles = computed(() => {
  const raw = userStore.engineerInfo?.roles || []
  return raw.map(getRoleLabel)
})

const avatarUrl = computed(() => {
  const avatar = userStore.engineerInfo?.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return '/' + avatar.replace(/^\//, '')
})

/** 加载统计 */
async function loadProfile() {
  try {
    const res = await getProfile()
    if (res.data) {
      stats.value = res.data
    }
  } catch (e) {
    // 错误已在拦截器处理
  }
}

function goTasks() {
  router.push('/engineer/tasks')
}

function goWaitAccept() {
  router.push({ path: '/engineer/tasks', query: { status: 'WAIT_ACCEPT' } })
}

function goProcessing() {
  router.push({ path: '/engineer/tasks', query: { status: 'PROCESSING' } })
}

function goWaitSettlement() {
  router.push({ path: '/engineer/tasks', query: { status: 'WAIT_SETTLEMENT' } })
}

function showComingSoon() {
  showDialog({ title: '提示', message: '功能开发中，敬请期待' })
}

function showAbout() {
  showDialog({
    title: '关于系统',
    message: `${systemStore.systemName}\n工程师移动端 v${APP_VERSION}\n${systemStore.footerContent}`
  })
}

async function onLogout() {
  try {
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出登录吗？'
    })
    // 调用退出接口（忽略失败，本地清除即可）
    try {
      await logout()
    } catch (e) {
      // 忽略退出接口错误
    }
    userStore.clearEngineerToken()
    showSuccessToast('已退出登录')
    setTimeout(() => {
      router.replace('/engineer/login')
    }, 1000)
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
/* 个人页标题栏与客户入口保持一致。 */
.engineer-profile-page :deep(.van-nav-bar) {
  height: 56px !important;
  line-height: 56px !important;
  background: #FFFFFF;
  border: none !important;
}
.engineer-profile-page :deep(.van-nav-bar::after) {
  display: none !important;
}
.engineer-profile-page :deep(.van-nav-bar__content) {
  height: 56px !important;
}
.engineer-profile-page :deep(.van-nav-bar__title) {
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

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.user-dept {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.user-roles {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.role-tag {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
  border-color: transparent !important;
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

.stat-item.total {
  grid-column: span 1;
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
  padding: 24px 16px 60px;
  color: #969799;
  font-size: 12px;
}
.engineer-profile-page { min-height:100vh; padding-bottom:70px; display:flex; flex-direction:column; background:#f4f6fa; color:#1e2b42; }.engineer-profile-page :deep(.van-nav-bar) { background:transparent; }.engineer-profile-page :deep(.van-nav-bar__title) { font-size:18px !important; font-weight:700 !important; }.profile-card > div:last-child { min-width:0; overflow-wrap:anywhere; }.profile-avatar { flex-shrink:0; }.profile-card { margin:12px 16px 0; padding:22px; display:flex; align-items:center; gap:16px; border-radius:24px; color:#fff; background:linear-gradient(135deg,#315ec4,#668ce8); box-shadow:0 14px 28px rgba(49,94,196,.22); }.profile-avatar { width:58px; height:58px; display:grid; place-items:center; border-radius:18px; background:rgba(255,255,255,.2); font-size:24px; font-weight:700; }.profile-name { font-size:21px; font-weight:700; }.profile-no { margin-top:7px; font-size:12px; opacity:.82; }.profile-stats { margin:14px 16px 0; display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:8px; }.profile-stat { min-height:76px; padding:10px 4px; display:flex; flex-direction:column; align-items:center; justify-content:center; border-radius:14px; background:#fff; box-shadow:0 6px 16px rgba(31,48,80,.06); }.profile-stat b { color:#315ec4; font-size:20px; line-height:1; }.profile-stat span { margin-top:8px; color:#7b8798; font-size:10px; white-space:nowrap; }.profile-stat--orange b { color:#e99b39; }.profile-stat--blue b { color:#4b7ee1; }.profile-stat--purple b { color:#7359ce; }.profile-stat--green b { color:#34a978; }.profile-stat--gray b { color:#8b96a8; }.profile-spacer { flex:1; min-height:30px; }.account-section { margin:0 16px; }.account-section .block-title { margin:0 0 10px; font-size:16px; font-weight:700; }.account-section :deep(.van-cell-group) { overflow:hidden; border-radius:18px; }.account-section + .version { padding:18px 0 0; }
.profile-info { flex: 1; min-width: 0; }
.profile-avatar { flex-shrink: 0; }
</style>
