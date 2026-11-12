<template>
  <div class="page">
    <van-nav-bar
      title="任务详情"
      left-text="返回"
      left-arrow
      @click-left="onBack"
    />

    <!-- 工单信息 -->
    <van-cell-group inset title="工单信息" class="section">
      <van-cell title="工单编号">
        <template #value>
          <span class="primary-text">{{ order.orderNo || '-' }}</span>
        </template>
      </van-cell>
      <van-cell title="工单状态">
        <template #value>
          <van-tag :type="getTagType(order.status)" size="medium">
            {{ getEngineerStatusText(order.status) }}
          </van-tag>
        </template>
      </van-cell>
      <van-cell v-if="order.reportAddress" title="报修地址" :value="order.reportAddress" />
      <van-cell title="接单时间" :value="formatDateTime(order.acceptTime)" />
      <van-cell title="开始时间" :value="formatDateTime(order.startTime)" />
      <van-cell v-if="order.signInAddress" title="签到地址">
        <template #value>
          <div class="desc-text">{{ order.signInAddress }}</div>
        </template>
      </van-cell>
      <van-cell title="完成时间" :value="formatDateTime(order.finishTime)" />
      <van-cell v-if="order.signOutAddress" title="签退地址">
        <template #value>
          <div class="desc-text">{{ order.signOutAddress }}</div>
        </template>
      </van-cell>
      <van-cell v-if="order.remark" title="备注">
        <template #value>
          <div class="desc-text">{{ order.remark }}</div>
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 现场照片（来源报修申请） -->
    <div v-if="photoList.length" class="section">
      <div class="block-title">现场照片（来源报修）</div>
      <div class="photo-grid">
        <EomsAuthImage
          v-for="(photo, index) in photoList"
          :key="index"
          :src="photo"
          width="100"
          height="100"
          fit="cover"
          radius="6"
          @click="previewPhotos(index)"
        />
      </div>
    </div>

    <!-- 维修操作按钮（根据状态展示） -->
    <div v-if="order.status" class="action-section">
      <div class="block-title">维修操作</div>
      <van-cell-group inset>
        <van-cell title="当前可操作">
          <template #value>
            <span v-if="order.status === 'WAIT_ACCEPT'" class="action-tip">待接单</span>
            <span v-else-if="order.status === 'ACCEPTED'" class="action-tip">已接单，请开始维修</span>
            <span v-else-if="order.status === 'PROCESSING'" class="action-tip">维修中，可提交记录或完成维修</span>
            <span v-else-if="order.status === 'WAIT_SETTLEMENT'" class="action-tip">维修已完成</span>
            <span v-else-if="order.status === 'FINISHED'" class="action-tip">工单已完成</span>
            <span v-else-if="order.status === 'CLOSED'" class="action-tip">工单已关闭</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 接单按钮 -->
      <div v-if="order.status === 'WAIT_ACCEPT'" class="btn-row">
        <van-button round block type="primary" @click="onAccept" :loading="actionLoading">接单</van-button>
      </div>

      <!-- 开始维修按钮 -->
      <div v-if="order.status === 'ACCEPTED'" class="btn-row">
        <van-button round block type="primary" @click="onStart" :loading="actionLoading">开始维修</van-button>
      </div>

      <!-- 尚无记录时可补充入口（已接单/维修中） -->
      <div
        v-if="!records.length && order.status === 'PROCESSING'"
        class="btn-row"
      >
        <van-button round block plain type="primary" @click="goRecord">
          填写维修记录
        </van-button>
      </div>
    </div>

    <!-- 维修记录列表 -->
    <div class="section">
      <div class="block-title">
        维修记录（{{ records.length }}条）
        <van-button
          v-if="!records.length && order.status === 'PROCESSING'"
          size="mini"
          type="primary"
          plain
          icon="plus"
          class="add-record-btn"
          @click="goRecord"
        >新增</van-button>
      </div>
      <van-cell-group inset>
        <van-empty v-if="!records.length" description="暂无维修记录" image="search" />
        <van-cell
          v-for="(record, idx) in records"
          :key="record.id"
          :title="`记录${idx + 1}`"
        >
          <template #title>
            <div class="record-title">
              <van-tag type="primary" plain size="mini">{{ record.createBy || '工程师' }}</van-tag>
              <span class="record-time">{{ formatDateTime(record.createTime) }}</span>
            </div>
          </template>
          <template #label>
            <div class="record-detail">
              <div v-if="record.faultReason"><span class="rec-label">故障原因：</span>{{ record.faultReason }}</div>
              <div v-if="record.checkProcess"><span class="rec-label">检测过程：</span>{{ record.checkProcess }}</div>
              <div v-if="record.solution"><span class="rec-label">解决方案：</span>{{ record.solution }}</div>
              <div v-if="record.result"><span class="rec-label">维修结果：</span>{{ record.result }}</div>
              <div v-if="record.remark"><span class="rec-label">备注：</span>{{ record.remark }}</div>
              <!-- 维修中照片 -->
                <div v-if="getPhotoList(record.processingPhotoUrls || record.processingPhotos).length" class="record-photos">
                <div class="photo-label">维修中照片：</div>
                <div class="photo-grid">
                  <EomsAuthImage
                    v-for="(photo, pi) in getPhotoList(record.processingPhotoUrls || record.processingPhotos)"
                    :key="'p' + pi"
                    :src="photo"
                    width="80"
                    height="80"
                    fit="cover"
                    radius="4"
                    @click="previewRecordPhotos(record.processingPhotoUrls || record.processingPhotos, pi)"
                  />
                </div>
              </div>
              <!-- 维修完成照片 -->
              <div v-if="getPhotoList(record.completedPhotoUrls || record.completedPhotos).length" class="record-photos">
                <div class="photo-label">维修完成照片：</div>
                <div class="photo-grid">
                  <EomsAuthImage
                    v-for="(photo, ci) in getPhotoList(record.completedPhotoUrls || record.completedPhotos)"
                    :key="'c' + ci"
                    :src="photo"
                    width="80"
                    height="80"
                    fit="cover"
                    radius="4"
                    @click="previewRecordPhotos(record.completedPhotoUrls || record.completedPhotos, ci)"
                  />
                </div>
              </div>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <RepairMaterialsSummary v-if="order.id" :order-id="order.id" />
    <section v-if="order.status === 'PROCESSING'" class="action-section finish-section">
      <p class="action-tip">请核对维修记录、使用配件和照片后再完成维修</p>
      <van-button round block type="success" :disabled="!records.length" :loading="actionLoading" @click="onFinish">完成维修</van-button>
      <p v-if="!records.length" class="action-tip">请先填写并提交维修记录</p>
    </section>
    <div class="bottom-space"></div>

    <!-- 操作弹窗（接单/签到/签退） -->
    <van-dialog
      v-model:show="showRemarkDialog"
      :title="remarkDialogTitle"
      show-cancel-button
      :confirm-button-text="confirmButtonText"
      :confirm-button-loading="actionLoading"
      :before-close="beforeRemarkClose"
    >
      <div class="dialog-content">
        <!-- 签到/签退定位说明 -->
        <div v-if="currentAction === 'start' || currentAction === 'finish'" class="sign-tip">
          <van-icon name="location-o" class="sign-icon" />
          <div class="sign-tip-text">
            <p class="sign-main">{{ currentAction === 'start' ? '签到' : '签退' }}将记录当前位置与时间</p>
            <p class="sign-sub">请确保已开启定位权限</p>
          </div>
        </div>
        <!-- 操作备注随本次接单、签到或签退提交 -->
        <van-field
          v-model="actionRemark"
          type="textarea"
          label="操作备注"
          placeholder="请输入操作备注（选填）"
          rows="2"
          autosize
          maxlength="200"
          show-word-limit
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { getEngineerStatusText } from '@/utils/engineerStatus'
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import EomsAuthImage from '@/components/EomsAuthImage.vue'
import RepairMaterialsSummary from '@/components/RepairMaterialsSummary.vue'
import { openEomsImagePreview } from '@/utils/eomsFile'
import {
  getTaskDetail,
  acceptTask,
  startTask,
  finishTask
} from '@/api/engineer'
import { formatDateTime, orderStatusMap } from '@/utils/dict'
import { toBizId } from '@/utils/biz-id'
import { getCurrentPosition } from '@/utils/geolocation'
import { getNativeAddress } from '@/utils/nativeAddress'

const route = useRoute()
const router = useRouter()

const order = ref({})
const records = ref([])
const actionLoading = ref(false)
const showRemarkDialog = ref(false)
const actionRemark = ref('')
const currentAction = ref('') // accept / start / finish
let pageActive = true
onBeforeUnmount(() => { pageActive = false })

const remarkDialogTitle = ref('')

/** 弹窗确认按钮文案：接单=确认、开始维修=签到、完成维修=签退 */
const confirmButtonText = computed(() => {
  return { accept: '确认', start: '签到', finish: '签退' }[currentAction.value] || '确认'
})

/** 获取浏览器地理位置（签到/签退用） */
function getLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    getCurrentPosition(
      async (position) => {
        const location = await getNativeAddress(position)
        resolve({
          longitude: String(position.coords.longitude.toFixed(6)),
          latitude: String(position.coords.latitude.toFixed(6)),
          location
        })
      },
      (err) => {
        reject(err)
      },
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
    )
  })
}

/** 现场照片列表（逗号分隔字符串转数组） */
const photoList = computed(() => {
  const src = order.value.photoUrls || order.value.photos
  if (!src) return []
  return src.split(',').filter(Boolean)
})

/** 点击预览大图 */
async function previewPhotos(startIndex) {
  await openEomsImagePreview(photoList.value, startIndex)
}

/** 将逗号分隔的照片字符串转为数组 */
function getPhotoList(photosStr) {
  if (!photosStr) return []
  return photosStr.split(',').filter(Boolean)
}

/** 预览维修记录中的照片 */
async function previewRecordPhotos(photosStr, startIndex) {
  const photos = getPhotoList(photosStr)
  await openEomsImagePreview(photos, startIndex)
}

function getTagType(status) {
  return orderStatusMap[status]?.tagType || 'default'
}

/** 加载任务详情 */
async function loadDetail() {
  const id = toBizId(route.params.id)
  if (!id) {
    showFailToast('缺少任务ID')
    return
  }
  try {
    const res = await getTaskDetail(id)
    if (res?.data) {
      order.value = res.data.order || {}
      records.value = res.data.records || []
    }
  } catch (e) {
    // 错误已在拦截器处理
  }
}

/** 接单 */
function onAccept() {
  currentAction.value = 'accept'
  remarkDialogTitle.value = '接单确认'
  actionRemark.value = ''
  showRemarkDialog.value = true
}

/** 开始维修 */
function onStart() {
  currentAction.value = 'start'
  remarkDialogTitle.value = '开始维修'
  actionRemark.value = ''
  showRemarkDialog.value = true
}

/** 完成维修：须先有维修记录 */
function onFinish() {
  if (!records.value.length) {
    showFailToast('请先提交维修记录后再完成维修')
    return
  }
  currentAction.value = 'finish'
  remarkDialogTitle.value = '完成维修'
  actionRemark.value = ''
  showRemarkDialog.value = true
}

/** 操作弹窗关闭前回调 */
async function beforeRemarkClose(action) {
  if (action !== 'confirm') return true
  if (!pageActive || actionLoading.value) return false
  actionLoading.value = true
  try {
    const id = order.value.id
    let res
    if (currentAction.value === 'accept') {
      res = await acceptTask(id, actionRemark.value)
    } else if (currentAction.value === 'start') {
      // 签到：先获取定位，失败则阻止提交
      let loc
      try {
        loc = await getLocation()
      } catch (e) {
        showFailToast('签到失败：无法获取定位，请检查定位权限后重试')
        return false
      }
      if (!pageActive) return false
      res = await startTask(id, actionRemark.value, loc.longitude, loc.latitude, loc.location)
    } else if (currentAction.value === 'finish') {
      // 签退：先获取定位，失败则阻止提交
      let loc
      try {
        loc = await getLocation()
      } catch (e) {
        showFailToast('签退失败：无法获取定位，请检查定位权限后重试')
        return false
      }
      if (!pageActive) return false
      res = await finishTask(id, actionRemark.value, loc.longitude, loc.latitude, loc.location)
    }
    // 拦截器已保证成功才 resolve（平台 R code===0）
    if (res) {
      const actionText = {
        accept: '接单成功',
        start: '签到成功，已开始维修',
        finish: '签退成功，维修已完成'
      }[currentAction.value]
      showSuccessToast(actionText)
      await loadDetail()
      return true
    }
    return false
  } catch (e) {
    return false
  } finally {
    actionLoading.value = false
  }
}

/** 跳转维修记录提交 */
function goRecord() {
  router.push(`/engineer/record/${order.value.id}`)
}

function onBack() {
  router.back()
}

// 初始化
loadDetail()
</script>

<style scoped>
/* ===== 顶部 van-nav-bar：与 engineer/tasks + customer/repairs 全局统一 56px 高度 ===== */
.page :deep(.van-nav-bar) {
  height: 56px !important;
  line-height: 56px !important;
  background: #FFFFFF;
  border: none !important;
}
.page :deep(.van-nav-bar::after) {
  display: none !important;
}
.page :deep(.van-nav-bar__content) {
  height: 56px !important;
}
.page :deep(.van-nav-bar__title) {
  font-weight: 600 !important;
  font-size: 17px !important;
  color: #0F172A;
}
.page :deep(.van-nav-bar__text) {
  color: #0F172A;
  font-size: 14px;
}

.section {
  margin-top: 12px;
}

.block-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #969799;
  padding: 16px 16px 8px;
  font-weight: 500;
}

.add-record-btn {
  margin-right: 4px;
}

.primary-text {
  color: #1989fa;
  font-weight: 600;
}

.desc-text {
  color: #323233;
  word-break: break-all;
  white-space: pre-wrap;
  line-height: 1.5;
}

.action-section {
  margin-top: 12px;
}

.action-tip {
  color: #1989fa;
  font-size: 13px;
}

.btn-row {
  margin: 16px;
}

.btn-row.double {
  display: flex;
  gap: 12px;
}

.btn-row.double .van-button {
  flex: 1;
}

.record-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-time {
  font-size: 12px;
  color: #969799;
}

.record-detail {
  margin-top: 4px;
  font-size: 13px;
  color: #646566;
  line-height: 1.6;
}

.rec-label {
  color: #969799;
}

.record-photos {
  margin-top: 8px;
}

.photo-label {
  font-size: 12px;
  color: #969799;
  margin-bottom: 4px;
}

.bottom-space {
  height: 24px;
}

.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 16px;
}

.dialog-content {
  padding: 12px 0;
}

.sign-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin: 0 16px;
  background: #f7f8fa;
  border-radius: 8px;
}

.sign-icon {
  font-size: 24px;
  color: #1989fa;
  flex-shrink: 0;
}

.sign-tip-text {
  flex: 1;
}

.sign-main {
  margin: 0;
  font-size: 14px;
  color: #323233;
  font-weight: 500;
}

.sign-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #969799;
}
</style>
