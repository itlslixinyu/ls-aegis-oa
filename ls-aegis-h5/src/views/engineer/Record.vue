<template>
  <div class="page">
    <van-nav-bar
      title="提交维修记录"
      left-text="返回"
      left-arrow
      @click-left="onBack"
    />

    <div v-if="orderInfo.orderNo" class="order-info">
      <van-notice-bar
        left-icon="orders-o"
        :text="`当前工单：${orderInfo.orderNo}（${getOrderStatusText(orderInfo.status)}）`"
      />
    </div>

    <van-form @submit="onSubmit" class="form">
      <van-cell-group inset title="维修记录信息">
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
          rows="3"
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
          rows="3"
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
      </van-cell-group>

      <!-- 维修中照片上传 -->
      <div class="upload-section">
        <div class="upload-title">
          <span class="upload-label">维修中照片</span>
          <span class="upload-hint">拍照记录维修过程（含水印）</span>
        </div>
        <van-uploader
          v-model="processingFileList"
          :max-count="6"
          :max-size="10 * 1024 * 1024"
          accept="image/jpeg,image/jpg,image/png"
          :after-read="(file) => afterRead(file, 'processing')"
          :before-delete="beforeDelete"
          :preview-full-image="false"
          @click-preview="(f) => previewUploadImage(processingFileList, f)"
          @oversize="onOversize"
          upload-text="维修中"
        />
      </div>

      <!-- 维修完成照片上传 -->
      <div class="upload-section">
        <div class="upload-title">
          <span class="upload-label">维修完成照片</span>
          <span class="upload-hint">拍照记录维修结果（含水印）</span>
        </div>
        <van-uploader
          v-model="completedFileList"
          :max-count="6"
          :max-size="10 * 1024 * 1024"
          accept="image/jpeg,image/jpg,image/png"
          :after-read="(file) => afterRead(file, 'completed')"
          :before-delete="beforeDelete"
          :preview-full-image="false"
          @click-preview="(f) => previewUploadImage(completedFileList, f)"
          @oversize="onOversize"
          upload-text="维修完成"
        />
      </div>

      <!-- 定位状态 -->
      <div v-if="locationStatus" class="location-status">
        <van-icon :name="locationStatus === 'success' ? 'location-o' : 'warning-o'" />
        <span>{{ locationStatus === 'success' ? '定位已获取，照片将带定位水印' : '定位获取失败，照片将不含定位信息' }}</span>
      </div>

      <div class="submit-area">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          提交记录
        </van-button>
      </div>
    </van-form>

    <div class="tip">
      <van-icon name="info-o" />
      <span>每个工单仅可提交一条维修记录，提交后方可完成维修进入结算</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { bindUploaderWatermarkPreview, openEomsImagePreview } from '@/utils/eomsFile'
import { getTaskDetail, submitRecord, uploadRepairPhoto } from '@/api/engineer'
import { getOrderStatusText } from '@/utils/dict'
import { toBizId } from '@/utils/biz-id'
import { getCurrentPosition } from '@/utils/geolocation'

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
  completedPhotos: ''
})

const processingFileList = ref([])
const completedFileList = ref([])
const locationStatus = ref('')
const locationData = reactive({
  longitude: '',
  latitude: ''
})

/** 获取浏览器定位（用于照片水印） */
function getLocation() {
  if (window.isSecureContext === false) {
    locationStatus.value = 'denied'
    return Promise.resolve()
  }
  if (!navigator.geolocation) {
    locationStatus.value = 'denied'
    return Promise.resolve()
  }
  locationStatus.value = 'loading'
  return new Promise((resolve) => {
    getCurrentPosition(
      (position) => {
        locationData.longitude = String(position.coords.longitude.toFixed(6))
        locationData.latitude = String(position.coords.latitude.toFixed(6))
        locationStatus.value = 'success'
        resolve()
      },
      () => {
        locationStatus.value = 'denied'
        resolve()
      },
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
    )
  })
}

/** 图片上传后回调 */
async function afterRead(fileItem, photoType) {
  const items = Array.isArray(fileItem) ? fileItem : [fileItem]
  items.forEach(item => { item.status = 'uploading'; item.message = '正在获取定位' })
  if (locationStatus.value !== 'success' && locationStatus.value !== 'denied') {
    await getLocation()
  }
  await Promise.all(items.map((item) => uploadOne(item, photoType)))
}

async function uploadOne(fileItem, photoType) {
  const currentList = photoType === 'processing' ? processingFileList : completedFileList
  if (!currentList.value.includes(fileItem)) return
  fileItem.status = 'uploading'
  fileItem.message = '上传中'
  try {
    const res = await uploadRepairPhoto(
      fileItem.file,
      form.repairOrderId,
      photoType,
      locationData.longitude,
      locationData.latitude
    )
    if (!currentList.value.includes(fileItem)) return
    const payload = res?.data
    const fileId = payload?.fileId || payload?.fileName
    if (!fileId) {
      fileItem.status = 'failed'
      fileItem.message = '上传失败'
      return
    }
    fileItem.fileId = String(fileId)
    fileItem.status = 'done'
    fileItem.message = ''
    await bindUploaderWatermarkPreview(fileItem, payload.fileId || payload.url)
  } catch (e) {
    console.error('维修照片上传失败', e)
    fileItem.status = 'failed'
    fileItem.message = e?.message || '上传失败'
  }
}

/** 文件超限提示 */
function beforeDelete(fileItem) {
  if (fileItem.status === 'uploading') {
    showFailToast('照片正在处理，请稍后再删除')
    return false
  }
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
  await openEomsImagePreview(images, startIndex >= 0 ? startIndex : 0)
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
  if (loading.value) return
  if (!form.repairOrderId) {
    showFailToast('工单ID缺失')
    return
  }
  if ([...processingFileList.value, ...completedFileList.value].some(item => !['done', 'failed'].includes(item.status))) {
    showFailToast('照片正在处理，请等待上传完成')
    return
  }
  const photoIds = list => list.filter(item => item.status === 'done' && item.fileId).map(item => item.fileId).join(',')
  form.processingPhotos = photoIds(processingFileList.value)
  form.completedPhotos = photoIds(completedFileList.value)
  loading.value = true
  try {
    const res = await submitRecord(form)
    if (res) {
      showSuccessToast('维修记录提交成功')
      setTimeout(() => {
        router.back()
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
  margin-bottom: 0;
}

.form {
  padding: 12px 0;
}

.upload-section {
  padding: 12px 16px;
}

.upload-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.upload-label {
  font-size: 14px;
  color: #323233;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #969799;
}

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
</style>
