<template>
  <div class="page repair-apply-page">
    <van-nav-bar
      title="在线报修"
      left-text="返回"
      left-arrow
      @click-left="onBack"
    />

    <van-form @submit="onSubmit" class="form" label-align="top">
      <p class="form-intro">填写故障信息并添加现场照片，方便我们及时安排处理。</p>
      <section class="form-card">
        <h2 class="card-title"><van-icon name="contact-o" aria-hidden="true" />联系人信息</h2>
        <van-cell-group class="contact-grid" :border="false">
        <van-field
          v-model="form.contactName"
          label-align="left"
          label-width="88px"
          label="联系人姓名"
          placeholder="请输入联系人姓名"
          :rules="[{ required: true, message: '请输入联系人姓名' }]"
          required
        />
        <van-field
          v-model="form.contactPhone"
          label-align="left"
          label-width="88px"
          label="联系人电话"
          type="tel"
          placeholder="请输入联系电话"
          :rules="[{ required: true, message: '请输入联系电话' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }]"
          required
        />
        </van-cell-group>
      </section>

      <section class="form-card">
        <h2 class="card-title"><van-icon name="notes-o" aria-hidden="true" />故障信息</h2>
        <van-cell-group :border="false">
        <van-field
          v-model="form.faultType"
          class="fault-type-field"
          label="故障类型"
          placeholder="请选择故障类型"
          readonly
          right-icon="arrow-down"
          required
          :rules="[{ required: true, message: '请选择故障类型' }]"
          @click="showFaultPicker = true"
        />
        <van-popup v-model:show="showFaultPicker" position="bottom" round>
          <van-picker
            :columns="faultTypeColumns"
            @confirm="onFaultConfirm"
            @cancel="showFaultPicker = false"
          />
        </van-popup>
        <van-field
          v-model="form.faultDesc"
          type="textarea"
          label="故障描述"
          placeholder="请描述设备问题、发生时间及影响情况"
          rows="3"
          autosize
          maxlength="500"
          show-word-limit
          :rules="[
            { required: true, message: '请输入故障描述' },
            { validator: (v) => v && v.trim() && v.trim().length >= 5, message: '故障描述至少5个字', trigger: 'onBlur' }
          ]"
          required
        />
        <van-field label="紧急程度" required>
          <template #input>
            <van-radio-group v-model="form.urgentLevel" direction="horizontal">
              <van-radio name="NORMAL">一般</van-radio>
              <van-radio name="URGENT">紧急</van-radio>
              <van-radio name="VERY_URGENT">特急</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field v-model="form.remark" label="备注" type="textarea" placeholder="选填，请在添加照片前填写" rows="1" autosize maxlength="500" show-word-limit />
        </van-cell-group>
      </section>

      <PhotoUploadSection
        title="现场照片" :files="fileList" :max-count="9" required
        :show-hint="false"
      >
            <van-uploader
              multiple
              v-model="fileList"
              :max-count="9"
              :data-photo-remaining="9 - fileList.length"
              :max-size="10 * 1024 * 1024"
              :before-read="beforeRead"
              :after-read="afterRead"
              :before-delete="beforeDelete"
              @oversize="onOversize"
              @click-preview="previewImage"
              :preview-full-image="false"
              accept="image/png,image/jpeg,image/jpg"
            >
              <div class="repair-photo-trigger">
                <span class="repair-photo-trigger__title"><van-icon name="photograph" />添加照片</span>
                <span class="repair-photo-trigger__description">自动加时间水印，定位成功后附位置</span>
                <span class="repair-photo-trigger__hint">JPG / PNG · 单张不超过 10MB</span>
              </div>
            </van-uploader>
            <PhotoUploadFeedback :files="fileList" @retry="photoSession.retry" />
        <template #status>
          <div v-if="locationStatus" class="location-tip" :class="`is-${locationStatus}`" role="status">
            <van-icon :name="locationStatus === 'success' ? 'location-o' : locationStatus === 'loading' ? 'clock-o' : 'info-o'" />
            <span>{{ locationStatus === 'success' ? '已获取位置，将添加定位水印' : locationStatus === 'loading' ? '正在获取位置…' : '暂未获取位置，将使用时间水印' }}</span>
          </div>
        </template>
      </PhotoUploadSection>

      <div class="submit-area">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          提交报修
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onBeforeUnmount } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { showSuccessToast, showFailToast, showConfirmDialog } from 'vant'
import { openEomsImagePreview, bindUploaderWatermarkPreview } from '@/utils/eomsFile'
import { submitApply, uploadWithWatermark } from '@/api/customer'
import { faultTypeOptions } from '@/utils/dict'
import { getCurrentPosition } from '@/utils/geolocation'
import { getNativeAddress } from '@/utils/nativeAddress'
import { createPhotoUploadSession, photoSubmissionError } from '@/utils/photoUploadSession'
import { validateUploadImage } from '@/utils/uploadImage'
import { releaseEomsFileObjectUrl, closeEomsImagePreview } from '@/utils/eomsFile'
import PhotoUploadFeedback from '@/components/PhotoUploadFeedback.vue'
import PhotoUploadSection from '@/components/PhotoUploadSection.vue'

const router = useRouter()

const form = reactive({
  contactName: '',
  contactPhone: '',
  faultType: '',
  faultDesc: '',
  urgentLevel: 'NORMAL',
  photos: '',
  remark: ''
})

const showFaultPicker = ref(false)
const faultTypeColumns = computed(() => faultTypeOptions.map(item => ({ text: item.text, value: item.value })))

function onFaultConfirm({ selectedValues }) {
  form.faultType = selectedValues[0]
  showFaultPicker.value = false
}

const loading = ref(false)
const fileList = ref([])
const locationStatus = ref('') // '' | 'loading' | 'success' | 'denied'
const locationData = reactive({
  longitude: '',
  latitude: '',
  address: ''
})

/** 获取浏览器地理位置 */
let locationPromise
function getLocation() {
  if (['success', 'denied'].includes(locationStatus.value)) return Promise.resolve()
  if (locationPromise) return locationPromise
  // 安全上下文检查：geolocation API 仅在 HTTPS 或 localhost 下可用
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
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    )
  }).finally(() => { locationPromise = null })
  return locationPromise
}

/** 文件读取后上传 */
const selectedPhotos = () => fileList.value
const photoSession = createPhotoUploadSession({
  getItems: selectedPhotos,
  prepare: getLocation,
  upload: (file, context, options) => uploadWithWatermark(file, { executor: context.reporter, remark: context.remark, location: locationData.address, longitude: locationData.longitude, latitude: locationData.latitude, ...options }),
  preview: bindUploaderWatermarkPreview,
  release: releaseEomsFileObjectUrl,
})
function afterRead(files) { return Promise.all((Array.isArray(files) ? files : [files]).map(item => photoSession.enqueue(item, { reporter: form.contactName.trim(), remark: form.remark }))) }
async function beforeRead(files) {
  if (!form.contactName.trim()) { showFailToast("请先填写联系人姓名，作为照片的报修人"); throw new Error("缺少报修人") }
  try {
    const validated = await Promise.all((Array.isArray(files) ? files : [files]).map(validateUploadImage))
    return Array.isArray(files) ? validated : validated[0]
  } catch (error) { showFailToast(error.message); throw error }
}
let submitted = false
let navigationTimer
onBeforeUnmount(() => { clearTimeout(navigationTimer); photoSession.dispose(); closeEomsImagePreview(photoSession) })
onBeforeRouteLeave(async () => {
  if (submitted || !selectedPhotos().length) return true
  try { await showConfirmDialog({ title: '离开当前页面？', message: '已选照片和填写内容不会保留，未完成上传将被取消。' }); return true }
  catch { return false }
})

/** 点击预览带水印图片 */
async function previewImage(fileItem) {
  const items = fileList.value.filter(f => f.url || f.content)
  const images = items.map(f => f.url || f.content)
  if (images.length === 0) return
  const startIndex = fileItem.url
    ? items.findIndex(f => f === fileItem)
    : items.findIndex(f => f.content === fileItem.content)
  await openEomsImagePreview(images, startIndex >= 0 ? startIndex : 0, { owner: photoSession, isActive: photoSession.isActive })
}

/** 删除照片 */
function beforeDelete(fileItem) {
  photoSession.remove(fileItem)
  return true
}

/** 文件超出大小限制 */
function onOversize() {
  showFailToast('图片大小不能超过 10MB')
}

async function onSubmit() {
  if (loading.value || submitted) return
  const photoError = photoSubmissionError(selectedPhotos(), 1)
  if (photoError) { showFailToast(photoError); return }
  const uploadedIds = [...new Set(fileList.value.map(item => item.fileId))]
  loading.value = true
  try {
    form.photos = uploadedIds.join(',')
    const reportAddress = locationData.address || (locationData.longitude && locationData.latitude
      ? `${locationData.latitude}, ${locationData.longitude}` : '')
    const res = await submitApply({ ...form, reportAddress })
    if (res) {
      submitted = true
      showSuccessToast('报修提交成功')
      navigationTimer = setTimeout(() => {
        router.replace('/customer/repairs/all')
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
</script>

<style scoped>
.page { min-height: 100vh; min-height: 100dvh; background: #f4f6fa; color: #17243a; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.page :deep(.van-nav-bar) { position: sticky; top: 0; z-index: 20; }
.page :deep(.van-nav-bar__content) { height: 56px; }
.page :deep(.van-nav-bar__title) { color: #17243a; font-size: 17px; font-weight: 600; }
.page :deep(.van-nav-bar__text), .page :deep(.van-nav-bar .van-icon) { color: #475569; }
.form { display: flex; flex-direction: column; gap: 14px; width: 100%; max-width: 560px; margin: 0 auto; padding: 14px 14px 0; box-sizing: border-box; }
.form-intro { margin: 0 2px; color: #64748b; font-size: 12px; line-height: 20px; }
.form-card { padding: 16px; border: 1px solid #e7ecf3; border-radius: 16px; background: #fff; }
.card-title { display: flex; align-items: center; gap: 8px; margin: 0 0 16px; color: #17243a; font-size: 16px; font-weight: 600; line-height: 24px; }
.card-title .van-icon { color: #4775d5; font-size: 20px; }
.form-card :deep(.van-cell-group) { display: flex; flex-direction: column; gap: 18px; }
.form-card :deep(.van-field) { padding: 0; overflow: visible; }
.form-card :deep(.van-cell::after) { display: none; }
.form-card :deep(.van-field__label) { color: #475569; font-size: 13px; line-height: 20px; }
.form-card :deep(.van-field__body) { min-height: 44px; padding: 10px 12px; border: 1px solid #e7ecf3; border-radius: 10px; background: #f8fafc; box-sizing: border-box; }
.form-card :deep(.contact-grid .van-field__label) { display: flex; align-items: center; margin-right: 10px; margin-bottom: 0; flex-shrink: 0; }
.form-card :deep(.contact-grid .van-field__value) { min-width: 0; }
.form-card :deep(.van-field__control) { color: #24334a; font-size: 14px; line-height: 22px; }
.form-card :deep(.contact-grid .van-field__control), .form-card :deep(.fault-type-field .van-field__control) { text-align: center; }
.form-card :deep(.van-field__control::placeholder) { color: #9aa5b6; }
.form-card :deep(.van-field__word-limit) { margin-top: 5px; color: #9aa5b6; }
.form-card :deep(.van-field__error-message) { padding-top: 4px; font-size: 12px; }
.form-card :deep(.van-cell--required::before) { position: static; display: none; }
.form-card :deep(.van-cell--required .van-field__label)::after { content: ' *'; color: #df6670; }
.form-card :deep(.van-radio-group) { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); width: 100%; padding: 0 4px; box-sizing: border-box; gap: 6px; }
.form-card :deep(.van-radio--horizontal) { margin: 0; justify-content: center; }
.form-card :deep(.van-radio__label) { margin-left: 5px; font-size: 13px; }
.repair-apply-page :deep(.van-uploader__input-wrapper) { grid-column: 1 / -1; height: 100px; aspect-ratio: auto; }
.repair-photo-trigger { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px; width: 100%; height: 100%; padding: 10px; box-sizing: border-box; border: 1px dashed #c3d2ed; border-radius: 10px; background: #f6f8fd; text-align: center; }
.repair-photo-trigger__title { display: flex; align-items: center; gap: 8px; color: #5c739b; font-size: 14px; }
.repair-photo-trigger__title .van-icon { color: #6487ca; font-size: 24px; }
.repair-photo-trigger__description { color: #64748b; font-size: 11px; line-height: 16px; }
.repair-photo-trigger__hint { color: #6b7789; font-size: 10px; line-height: 15px; }
.location-tip { display: flex; align-items: flex-start; gap: 6px; margin-top: 10px; padding: 8px 10px; border-radius: 8px; background: #f5f7fa; color: #7c889b; font-size: 12px; line-height: 18px; }
.location-tip .van-icon { flex: none; margin-top: 2px; }
.location-tip.is-success { color: #4a8171; background: #f0f8f5; }
.submit-area { margin-top: 0; padding: 2px 0 calc(16px + env(safe-area-inset-bottom)); }
.submit-area :deep(.van-button) { height: 46px; border: 0; border-radius: 12px; background: #3d6ed3; font-size: 15px; font-weight: 500; }
</style>
