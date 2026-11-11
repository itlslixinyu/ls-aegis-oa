<template>
  <div class="page">
    <van-nav-bar title="我的报修" />

    <section class="page-hero">
      <div>
        <div class="hero-kicker">服务中心</div>
        <h1>我的报修</h1>
        <p>提交问题后，随时查看处理进度</p>
      </div>
      <div class="hero-icon"><van-icon name="orders-o" /></div>
    </section>

    <!-- 当前用户信息条（原生蓝色风格，与engineer/tasks工程师条一致） -->
    <div class="customer-bar">
      <span class="customer-bar__icon">👤</span>
      <span class="customer-bar__label">用户：</span>
      <span class="customer-bar__name">{{ customerName }}</span>
    </div>

    <div class="quick-actions">
      <van-button round type="primary" icon="plus" @click="goApply">在线报修</van-button>
      <van-button round plain type="primary" icon="replay" @click="onRefresh">刷新记录</van-button>
    </div>

    <van-tabs v-model:active="activeTab" @change="onTabChange" sticky>
      <van-tab title="全部" name="" />
      <van-tab title="待处理" name="WAIT" />
      <van-tab title="已确认" name="CONFIRM" />
      <van-tab title="已转工单" name="ORDER" />
      <van-tab title="已取消" name="CANCEL" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="list-container">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-if="list.length === 0 && finished" class="empty-tip">
          <van-empty image="search" description="还没有报修记录">
            <van-button round type="primary" size="small" @click="goApply">立即提交报修</van-button>
          </van-empty>
        </div>

        <van-cell
          v-for="item in list"
          :key="item.id"
          is-link
          class="repair-card"
          @click="goDetail(item.id)"
        >
          <template #title>
            <div class="card-header">
              <span class="repair-no">{{ item.repairNo }}</span>
              <van-tag :type="getTagType(item.status)" size="medium">
                {{ getApplyStatusText(item.status) }}
              </van-tag>
            </div>
          </template>
          <template #label>
            <div class="card-body">
              <div class="info-row">
                <span class="label">联系人：</span>
                <span>{{ item.contactName }}</span>
              </div>
              <div class="info-row">
                <span class="label">故障类型：</span>
                <span>{{ item.faultType || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="label">报修时间：</span>
                <span>{{ formatDateTime(item.applyTime) }}</span>
              </div>
              <div class="info-row desc">
                <span class="label">故障描述：</span>
                <span class="desc-text">{{ item.faultDesc || '-' }}</span>
              </div>
            </div>
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>

    <div class="fab-add-btn" @click="goApply">
      <van-icon name="plus" size="24" color="#fff" />
    </div>

    <!-- 底部导航 -->
    <AppTabbar />
  </div>
</template>

<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRepairList } from '@/api/customer'
import { useUserStore } from '@/store/user'
import { getApplyStatusText, formatDateTime } from '@/utils/dict'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()


/**
 * 当前登录客户姓名/标识：多层字段 fallback
 *  - 主字段：customerInfo.customerName（客户注册名）
 *  - 兼容：name / contactName / nickName / userName / realName
 *  - 最后兜底：显示客户手机号（脱敏或完整），再不行返回「用户」
 */
const customerName = computed(() => {
  const info = userStore.customerInfo
  if (!info) return '用户'

  const name = info.customerName
    || info.name
    || info.contactName
    || info.nickName
    || info.nickname
    || info.userName
    || info.username
    || info.realName
  if (name) return name

  // 兜底：显示客户手机号（完整显示，前端不做脱敏以免影响识别）
  const phone = info.customerMobile || info.mobile || info.phone || info.customerPhone || info.tel
  if (phone) return phone

  return '用户'
})
const customerNo = computed(() => userStore.customerInfo?.customerNo || '')

const activeTab = ref(typeof route.query.status === 'string' ? route.query.status : '')
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 10

function getTagType(status) {
  const map = {
    WAIT: 'warning',
    CONFIRM: 'primary',
    ORDER: 'success',
    CANCEL: 'default'
  }
  return map[status] || 'default'
}

async function onLoad() {
  loading.value = true
  try {
    const res = await getRepairList({
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
  router.push(`/customer/repair/${id}`)
}

function goApply() {
  router.push('/customer/apply')
}
</script>

<style scoped>
:deep(.van-nav-bar) {
  height: 56px !important;
  line-height: 56px !important;
}
:deep(.van-nav-bar__title) {
  font-size: 17px !important;
  font-weight: 600 !important;
}
:deep(.van-nav-bar__content) {
  height: 56px !important;
}

/* ===== 当前用户信息条（与engineer/tasks工程师条同风格同配色） ===== */
.customer-bar {
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
.customer-bar__icon {
  font-size: 16px;
  flex-shrink: 0;
}
.customer-bar__label {
  color: #1677FF;
  font-weight: 500;
  flex-shrink: 0;
}
.customer-bar__name {
  color: #0F172A;
  font-weight: 600;
  word-break: break-all;
}

.list-container {
  min-height: calc(100vh - 160px);
  padding-bottom: 56px;
}

.repair-card {
  margin: 8px 12px;
  border-radius: 8px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.repair-no {
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

.info-row.desc {
  align-items: flex-start;
}

.desc-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fab-add-btn {
  position: fixed;
  right: 20px;
  bottom: calc(56px + env(safe-area-inset-bottom, 0px) + 16px);
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #1989fa 100%);
  box-shadow: 0 4px 12px rgba(25, 137, 250, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.fab-add-btn:active {
  transform: scale(0.9);
}
.page { min-height: 100vh; background: #f4f6fa; }
:deep(.van-nav-bar) { background: #f4f6fa !important; }
:deep(.van-nav-bar__title) { color: #1c2940 !important; font-size: 19px !important; font-weight: 700 !important; }
.customer-bar { margin: 4px 16px 12px; padding: 13px 16px; border: 0; border-radius: 16px; background: linear-gradient(110deg,#e8efff,#f1f4ff); box-shadow: 0 5px 16px rgba(45,73,130,.06); }
.customer-bar__icon { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 11px; background: #fff; }
.customer-bar__label { color: #637aa9; }
.customer-bar__name { color: #24324c; }
:deep(.van-tabs) { background: #f4f6fa; }
:deep(.van-tabs__wrap) { margin: 0 12px 8px; border-radius: 14px; background: #fff; box-shadow: 0 4px 14px rgba(27,44,76,.04); }
:deep(.van-tabs__line) { background: #426bd0 !important; border-radius: 8px; }
:deep(.van-tab--active) { color: #315ec4 !important; font-weight: 650; }
.repair-card { margin: 10px 14px; border: 1px solid #edf0f5; border-radius: 18px; box-shadow: 0 7px 22px rgba(31,48,80,.055); }
.repair-card :deep(.van-cell) { padding: 16px; }
.repair-no { color: #24324c; font-size: 15px; }
.info-row { color: #536078; margin-top: 7px; }
.info-row .label { color: #98a2b3; }
.fab-add-btn { width: 56px; height: 56px; right: 18px; background: linear-gradient(135deg,#557fe0,#315ec4); box-shadow: 0 9px 22px rgba(49,94,196,.32); }
:deep(.van-tabbar) { border-radius: 20px 20px 0 0; box-shadow: 0 -5px 24px rgba(28,45,78,.07); padding-bottom: env(safe-area-inset-bottom); }
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
