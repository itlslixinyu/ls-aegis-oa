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
          <van-tag :color="getWorkOrderStatusBackground(order.status)" size="medium">
            {{ getEngineerStatusText(order.status) }}
          </van-tag>
        </template>
      </van-cell>
      <template v-if="order.reportSource === 'ENGINEER_PHONE'">
        <van-cell title="代报工程师" :value="order.fieldReporter" />
        <van-cell title="报修联系人" :value="order.reportContact" />
        <van-cell title="故障描述" :label="order.reportedFault" />
      </template>
      <van-cell v-if="order.reportAddress" title="报修地址" class="address-cell" :value="order.reportAddress" />
      <van-cell title="接单时间" :value="formatDateTime(order.acceptTime)" />
      <van-cell title="开始时间" :value="formatDateTime(order.startTime)" />
      <van-cell v-if="order.signInAddress" title="签到地址" class="address-cell">
        <template #value>
          <div class="desc-text">{{ order.signInAddress }}</div>
        </template>
      </van-cell>
      <van-cell title="完成时间" :value="formatDateTime(order.finishTime)" />
      <van-cell v-if="order.signOutAddress" title="签退地址" class="address-cell">
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
        <van-cell title="当前可操作" class="operation-status">
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

      <div class="detail-entry-buttons">
        <van-button round block plain type="primary" icon="records-o" @click="onRecordEntry" :aria-expanded="showRecords">
          {{ records.length || order.status !== 'PROCESSING' ? '查看维修记录' : '填写维修记录' }}
        </van-button>
        <van-button round block plain type="primary" icon="apps-o" @click="toggleMaterials" :aria-expanded="showMaterials">
          {{ order.status === 'PROCESSING' ? '填写使用配件' : '查看使用配件' }}
        </van-button>
      </div>
    </div>

    <!-- 仅在点击入口后展示对应内容 -->
    <div v-if="showRecords" class="section record-section">
      <div class="block-title">
        维修记录（{{ records.length }}条）
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

    <section v-if="showMaterials && order.id" class="materials-section">
      <RepairMaterialsSummary ref="materialsSummary" :order-id="order.id" />
      <van-form v-if="order.status === 'PROCESSING'" @submit="saveMaterials">
        <RepairMaterialFields :key="materialEditorKey" v-model="materialDraft" :order-id="String(order.id)" />
        <div class="materials-save">
          <van-button round block type="primary" native-type="submit" :loading="materialSaving">{{ materialDraft.length ? '保存使用配件' : '完成填写' }}</van-button>
        </div>
      </van-form>
    </section>
    <section v-if="order.status === 'PROCESSING'" class="action-section finish-section">
      <van-button round block type="success" :disabled="!records.length" :loading="actionLoading" @click="onFinish">完成维修</van-button>
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
import { getWorkOrderStatusBackground } from '@/utils/workOrderStatus'
import { getEngineerStatusText } from '@/utils/engineerStatus'
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { showSuccessToast, showFailToast, showConfirmDialog, closeDialog } from 'vant'
import EomsAuthImage from '@/components/EomsAuthImage.vue'
import RepairMaterialsSummary from '@/components/RepairMaterialsSummary.vue'
import RepairMaterialFields from '@/components/RepairMaterialFields.vue'
import { openEomsImagePreview } from '@/utils/eomsFile'
import {
  getTaskDetail,
  acceptTask,
  startTask,
  finishTask,
  addTaskMaterials
} from '@/api/engineer'
import { formatDateTime, orderStatusMap } from '@/utils/dict'
import { toBizId } from '@/utils/biz-id'
import { getCurrentPosition } from '@/utils/geolocation'
import { getNativeAddress } from '@/utils/nativeAddress'

const route = useRoute()
const router = useRouter()

const order = ref({})
const records = ref([])
const showRecords = ref(false)
const showMaterials = ref(false)
const materialDraft = ref([])
const materialSaving = ref(false)
const materialsSummary = ref(null)
const materialEditorKey = ref(0)
const newMaterialRequestId = () => globalThis.crypto?.randomUUID?.() || `materials-${Date.now()}-${Math.random().toString(36).slice(2)}`
let materialRequestId = newMaterialRequestId()
let materialLeaveConfirmation
async function confirmDiscardMaterials() {
  if (materialSaving.value) { showFailToast('配件正在保存'); return false }
  if (!materialDraft.value.length) return true
  if (!materialLeaveConfirmation) {
    materialLeaveConfirmation = showConfirmDialog({ title:'放弃本次配件填写？', message:'已保存的配件不受影响。', closeOnPopstate:false })
      .then(()=>true,()=>false).finally(()=>{materialLeaveConfirmation=undefined})
  }
  return materialLeaveConfirmation
}
onBeforeRouteLeave(confirmDiscardMaterials)
onBeforeUnmount(()=>{if(materialLeaveConfirmation)closeDialog()})
function onRecordEntry() {
  if (!records.value.length && order.value.status === 'PROCESSING') { goRecord(); return }
  showRecords.value = !showRecords.value
}
async function toggleMaterials() {
  if (showMaterials.value && !await confirmDiscardMaterials()) return
  if (showMaterials.value) { materialDraft.value=[]; materialRequestId=newMaterialRequestId() }
  showMaterials.value = !showMaterials.value
}
async function saveMaterials() {
  if (materialSaving.value || !pageActive) return
  if (!materialDraft.value.length) { showMaterials.value=false; return }
  materialSaving.value=true
  try {
    await addTaskMaterials(String(order.value.id),materialDraft.value.map(line=>({
      materialId:line.materialId,warehouseId:line.warehouseId,quantity:line.quantity,remark:line.remark
    })),materialRequestId)
    if (!pageActive) return
    materialDraft.value=[];materialRequestId=newMaterialRequestId();materialEditorKey.value++
    showSuccessToast('使用配件已保存')
    await materialsSummary.value?.reload()
  } catch { /* 请求层展示错误，保留填写内容和提交标识供重试。 */ }
  finally { materialSaving.value=false }
}
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
  if (materialSaving.value || materialDraft.value.length) { showFailToast('使用配件尚未保存'); return }
  if (!records.value.length) {
    showFailToast('维修记录尚未提交')
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

.address-cell {
  gap: 12px;
  align-items: flex-start;
}
.address-cell :deep(.van-cell__title) {
  flex: 0 0 auto;
  white-space: nowrap;
}
.address-cell :deep(.van-cell__value) {
  flex: 1;
  min-width: 0;
  text-align: left;
  overflow-wrap: anywhere;
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

.operation-status {
  gap: 8px;
}
.operation-status :deep(.van-cell__title) {
  flex: 0 0 auto;
  font-size: 13px;
  white-space: nowrap;
}
.operation-status :deep(.van-cell__value) {
  flex: 1;
  min-width: 0;
}

.action-tip {
  color: #1989fa;
  font-size: clamp(11px, 3.25vw, 13px);
  white-space: nowrap;
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
.detail-entry-buttons{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin:16px}.detail-entry-buttons :deep(.van-button){padding:0 8px;font-size:13px;white-space:nowrap}.materials-save{margin:12px 16px 20px}.materials-section{margin:0}
</style>
