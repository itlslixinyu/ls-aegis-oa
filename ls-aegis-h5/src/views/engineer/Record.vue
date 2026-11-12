<template>
  <div class="page">
    <van-nav-bar
      title="提交维修记录"
      left-text="返回"
      left-arrow
      @click-left="onBack"
    />

    <header class="record-intro">
      <div class="intro-icon"><van-icon name="records-o" /></div>
      <div><h1>记录本次维修</h1><p>填写处理情况，登记配件并补充现场照片</p></div>
    </header>
    <div v-if="orderInfo.orderNo" class="order-info"><span>{{ orderInfo.orderNo }}</span><van-tag plain type="primary">{{ getEngineerStatusText(orderInfo.status) }}</van-tag></div>

    <van-form @submit="onSubmit" class="form" label-align="top">
      <van-cell-group inset title="维修情况">
        <van-field
          v-model="form.faultReason"
          type="textarea"
          label="故障原因"
          placeholder="请描述故障原因"
          rows="2"
          autosize
          maxlength="500"
          show-word-limit
          :rules="[{ required: true, message: '请输入故障原因' }]"
          required
        />
        <van-field
          v-model="form.checkProcess"
          type="textarea"
          label="检测过程"
          placeholder="请描述检测过程"
          rows="2"
          autosize
          maxlength="1000"
          show-word-limit
          :rules="[{ required: true, message: '请输入检测过程' }]"
          required
        />
        <van-field
          v-model="form.solution"
          type="textarea"
          label="解决方案"
          placeholder="请描述解决方案"
          rows="2"
          autosize
          maxlength="1000"
          show-word-limit
          :rules="[{ required: true, message: '请输入解决方案' }]"
          required
        />
        <van-field
          v-model="form.result"
          type="textarea"
          label="维修结果"
          placeholder="请描述维修结果"
          rows="2"
          autosize
          maxlength="500"
          show-word-limit
          :rules="[{ required: true, message: '请输入维修结果' }]"
          required
        />
        <van-field v-model="form.remark" label="备注" type="textarea" placeholder="选填，请在添加照片前填写" rows="1" autosize maxlength="500" show-word-limit />
      </van-cell-group>

      <RepairMaterialFields v-model="form.materials" :order-id="form.repairOrderId || ''" />

      <div class="photo-sections">
      <PhotoUploadSection title="维修中照片" :files="processingFileList" :max-count="6" description="记录故障现场和维修过程，最多添加6张照片。">
        <van-uploader
          multiple v-model="processingFileList"
          :data-photo-remaining="6 - processingFileList.length"
          :max-count="6" :max-size="10 * 1024 * 1024"
          accept="image/jpeg,image/jpg,image/png"
          :after-read="(file) => afterRead(file, 'processing')"
          :before-read="beforeRead" :before-delete="beforeDelete" :preview-full-image="false"
          @click-preview="(f) => previewUploadImage(processingFileList, f)" @oversize="onOversize"
          upload-text="添加照片"
        />
        <PhotoUploadFeedback :files="processingFileList" @retry="photoSession.retry" />
      </PhotoUploadSection>
      <PhotoUploadSection title="维修完成照片" :files="completedFileList" :max-count="6" description="记录修复后的设备状态，最多添加6张照片。">
        <van-uploader
          multiple v-model="completedFileList"
          :data-photo-remaining="6 - completedFileList.length"
          :max-count="6" :max-size="10 * 1024 * 1024"
          accept="image/jpeg,image/jpg,image/png"
          :after-read="(file) => afterRead(file, 'completed')"
          :before-read="beforeRead" :before-delete="beforeDelete" :preview-full-image="false"
          @click-preview="(f) => previewUploadImage(completedFileList, f)" @oversize="onOversize"
          upload-text="添加照片"
        />
        <PhotoUploadFeedback :files="completedFileList" @retry="photoSession.retry" />
      </PhotoUploadSection>
      </div>

      <!-- 定位状态 -->
      <div v-if="locationStatus" class="location-status">
        <van-icon :name="locationStatus === 'success' ? 'location-o' : locationStatus === 'loading' ? 'clock-o' : 'info-o'" />
        <span>{{ locationStatus === 'success' ? '定位已获取，照片将带定位水印' : locationStatus === 'loading' ? '正在获取位置…' : '暂未获取位置，将使用时间水印' }}</span>
      </div>

      <div class="submit-area">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          提交记录
        </van-button>
      </div>
    </van-form>

    <div class="tip">
      <van-icon name="info-o" />
      <span>每个工单仅可提交一条维修记录，提交后可返回工单核对并完成维修</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { showSuccessToast, showFailToast, showConfirmDialog } from 'vant'
import { bindUploaderWatermarkPreview, openEomsImagePreview } from '@/utils/eomsFile'
import { getTaskDetail, submitRecord, uploadRepairPhoto } from '@/api/engineer'
import { getEngineerStatusText } from '@/utils/engineerStatus'
import { toBizId } from '@/utils/biz-id'
import { getCurrentPosition } from '@/utils/geolocation'
import { getNativeAddress } from '@/utils/nativeAddress'
import { createPhotoUploadSession, photoSubmissionError } from '@/utils/photoUploadSession'
import { validateUploadImage } from '@/utils/uploadImage'
import { releaseEomsFileObjectUrl, closeEomsImagePreview } from '@/utils/eomsFile'
import PhotoUploadFeedback from '@/components/PhotoUploadFeedback.vue'
import PhotoUploadSection from '@/components/PhotoUploadSection.vue'
import RepairMaterialFields from '@/components/RepairMaterialFields.vue'

const route = useRoute()
const router = useRouter()

const orderInfo = ref({})
const loading = ref(false)

const form = reactive({
  repairOrderId: null,
  faultReason: '',
  checkProcess: '',
  solution: '',
  result: '',
  processingPhotos: '',
  completedPhotos: '',
  remark: '',
  materials: []
})

const processingFileList = ref([])
const completedFileList = ref([])
const locationStatus = ref('')
const locationData = reactive({
  longitude: '',
  latitude: '',
  address: ''
})

/** 获取浏览器定位（用于照片水印） */
let locationPromise
function getLocation() {
  if (['success', 'denied'].includes(locationStatus.value)) return Promise.resolve()
  if (locationPromise) return locationPromise
  if (window.isSecureContext === false) {
    locationStatus.value = 'denied'
    return Promise.resolve()
  }
  if (!navigator.geolocation) {
    locationStatus.value = 'denied'
    return Promise.resolve()
  }
  locationStatus.value = 'loading'
  locationPromise = new Promise((resolve) => {
    getCurrentPosition(
      async (position) => {
        locationData.longitude = String(position.coords.longitude.toFixed(6))
        locationData.latitude = String(position.coords.latitude.toFixed(6))
        locationData.address = await getNativeAddress(position)
        locationStatus.value = 'success'
        resolve()
      },
      () => {
        locationStatus.value = 'denied'
        resolve()
      },
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
    )
  }).finally(() => { locationPromise = null })
  return locationPromise
}

/** 图片上传后回调 */
const selectedPhotos = () => [...processingFileList.value, ...completedFileList.value]
const photoSession = createPhotoUploadSession({
  getItems: selectedPhotos,
  prepare: getLocation,
  upload: (file, context, options) => uploadRepairPhoto(file, form.repairOrderId, context.photoType, locationData.longitude, locationData.latitude, { ...options, location: locationData.address, remark: context.remark }),
  preview: bindUploaderWatermarkPreview,
  release: releaseEomsFileObjectUrl,
})
function afterRead(files, photoType) { return Promise.all((Array.isArray(files) ? files : [files]).map(item => photoSession.enqueue(item, { photoType, remark: form.remark }))) }
async function beforeRead(files) {
  try {
    const validated = await Promise.all((Array.isArray(files) ? files : [files]).map(validateUploadImage))
    return Array.isArray(files) ? validated : validated[0]
  } catch (error) { showFailToast(error.message); throw error }
}
let submitted = false
let navigationTimer
onBeforeUnmount(() => { clearTimeout(navigationTimer); photoSession.dispose(); closeEomsImagePreview(photoSession) })
onBeforeRouteLeave(async () => {
  if (loading.value) { showFailToast('正在提交维修记录，请稍候'); return false }
  if (submitted || (!selectedPhotos().length && !form.materials.length && ![form.faultReason,form.checkProcess,form.solution,form.result,form.remark].some(Boolean))) return true
  try { await showConfirmDialog({ title: '离开当前页面？', message: '已选照片和填写内容不会保留，未完成上传将被取消。' }); return true }
  catch { return false }
})

/** 文件超限提示 */
function beforeDelete(fileItem) {
  photoSession.remove(fileItem)
  return true
}

function onOversize() {
  showFailToast('图片大小不能超过 10MB')
}

/** 上传缩略图预览（自建全屏，避免 Vant 细白条） */
async function previewUploadImage(listRef, fileItem) {
  const items = (listRef?.value || listRef || []).filter((f) => f.url || f.content)
  const images = items.map((f) => f.url || f.content)
  if (!images.length) return
  const startIndex = items.findIndex((f) => f === fileItem)
  await openEomsImagePreview(images, startIndex >= 0 ? startIndex : 0, { owner: photoSession, isActive: photoSession.isActive })
}

/** 加载工单信息 */
async function loadOrder() {
  const orderId = toBizId(route.params.orderId)
  if (!orderId) {
    showFailToast('缺少工单ID')
    return
  }
  form.repairOrderId = orderId
  try {
    const res = await getTaskDetail(orderId)
    if (res.data) {
      orderInfo.value = res.data.order || {}
    }
  } catch (e) {
    // 错误已在拦截器处理
  }
}

/** 提交维修记录 */
async function onSubmit() {
  if (loading.value || submitted) return
  if (!form.repairOrderId) {
    showFailToast('工单ID缺失')
    return
  }
  const photoError = photoSubmissionError(selectedPhotos())
  if (photoError) { showFailToast(photoError); return }
  const photoIds = list => list.filter(item => item.status === 'done' && item.fileId).map(item => item.fileId).join(',')
  form.processingPhotos = photoIds(processingFileList.value)
  form.completedPhotos = photoIds(completedFileList.value)
  loading.value = true
  try {
    const res = await submitRecord({ ...form, materials: form.materials.map(line => ({
      materialId: line.materialId, warehouseId: line.warehouseId, quantity: line.quantity, remark: line.remark
    })) })
    if (res && photoSession.isActive()) {
      submitted = true
      showSuccessToast('维修记录提交成功')
      navigationTimer = setTimeout(() => {
        if (photoSession.isActive()) router.back()
      }, 1500)
    }
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

function onBack() {
  router.back()
}

// 初始化
loadOrder()
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

.order-info {
  display:flex;align-items:center;justify-content:space-between;gap:12px;margin:0 16px;padding:12px 16px;border:1px solid #e7ecf3;border-radius:12px;background:#fff;font-size:13px;color:#475569;
}

.form {
  padding: 12px 0;
}

.photo-sections { display: flex; flex-direction: column; gap: 14px; margin: 16px; }

.location-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 12px;
  color: #969799;
}

.submit-area {
  margin: 24px 16px;
}

.tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 16px;
  color: #969799;
  font-size: 12px;
}
.record-intro{display:flex;align-items:center;gap:12px;padding:22px 20px 16px}.intro-icon{display:grid;place-items:center;flex-shrink:0;width:42px;height:42px;border-radius:14px;background:#e4edff;color:#4775d5;font-size:24px}.record-intro h1{margin:0;font-size:19px;color:#17243a}.record-intro p{margin:6px 0 0;font-size:12px;color:#8290a5;line-height:1.6}
.form :deep(.van-cell-group--inset){border:1px solid #e7ecf3;border-radius:16px;overflow:hidden}.form :deep(.van-cell-group__title){font-size:15px;font-weight:600;color:#17243a}.form :deep(.van-field__label){font-weight:500;color:#334155}.form :deep(.van-field__control){font-size:14px;line-height:1.7}.form :deep(.van-field__word-limit){color:#a1adbe}.submit-area{margin-top:18px;margin-bottom:4px}.tip{padding-top:8px;padding-bottom:calc(20px + env(safe-area-inset-bottom));line-height:1.6;text-align:center}.page{padding-bottom:8px}
</style>
