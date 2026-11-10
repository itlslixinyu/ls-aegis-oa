<template>
  <div class="page">
    <van-nav-bar
      title="在线报修"
      left-text="返回"
      left-arrow
      @click-left="onBack"
    />

    <van-form @submit="onSubmit" class="form">
      <van-cell-group inset title="联系人信息">
        <van-field
          v-model="form.contactName"
          label="联系人"
          placeholder="请输入联系人姓名"
          :rules="[{ required: true, message: '请输入联系人姓名' }]"
          required
        />
        <van-field
          v-model="form.contactPhone"
          label="联系电话"
          type="tel"
          placeholder="请输入联系电话"
          :rules="[{ required: true, message: '请输入联系电话' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }]"
          required
        />
      </van-cell-group>

      <van-cell-group inset title="故障信息" class="section">
        <van-field
          v-model="form.faultType"
          label="故障类型"
          placeholder="请选择故障类型"
          readonly
          is-link
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
          placeholder="请详细描述故障现象"
          rows="4"
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
      </van-cell-group>

      <van-cell-group inset title="现场照片（自动添加定位和时间水印）" class="section">
        <van-field label="照片">
          <template #input>
            <van-uploader
              v-model="fileList"
              :max-count="9"
              :max-size="10 * 1024 * 1024"
              :after-read="afterRead"
              :before-delete="beforeDelete"
              @oversize="onOversize"
              @click-preview="previewImage"
              :preview-full-image="false"
              capture="camera"
              accept="image/png,image/jpeg,image/jpg"
            />
          </template>
        </van-field>
        <div v-if="locationStatus" class="location-tip">
          <van-tag v-if="locationStatus === 'success'" type="success">定位已获取</van-tag>
          <van-tag v-else-if="locationStatus === 'loading'" type="warning">正在获取定位...</van-tag>
          <van-tag v-else-if="locationStatus === 'denied'" type="danger">定位获取失败（仅添加时间水印）</van-tag>
        </div>
      </van-cell-group>

      <div class="submit-area">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          提交报修
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { openEomsImagePreview, bindUploaderWatermarkPreview } from '@/utils/eomsFile'
import { submitApply, uploadWithWatermark } from '@/api/customer'
import { faultTypeOptions } from '@/utils/dict'
import { getCurrentPosition } from '@/utils/geolocation'

const router = useRouter()

const form = reactive({
  contactName: '',
  contactPhone: '',
  faultType: '',
  faultDesc: '',
  urgentLevel: 'NORMAL',
  photos: ''
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
function getLocation() {
  // 安全上下文检查：geolocation API 仅在 HTTPS 或 localhost 下可用
  if (window.isSecureContext === false) {
    locationStatus.value = 'denied'
    showFailToast('定位需要 HTTPS 环境或使用 localhost 访问，当前页面不满足安全要求')
    return Promise.resolve()
  }
  if (!navigator.geolocation) {
    locationStatus.value = 'denied'
    showFailToast('当前浏览器不支持定位功能')
    return Promise.resolve()
  }
  locationStatus.value = 'loading'
  return new Promise((resolve) => {
    getCurrentPosition(
      (position) => {
        locationData.longitude = String(position.coords.longitude.toFixed(6))
        locationData.latitude = String(position.coords.latitude.toFixed(6))
        // 地址由后端逆地理编码获取，前端只传经纬度
        locationData.address = ''
        locationStatus.value = 'success'
        resolve()
      },
      (error) => {
        locationStatus.value = 'denied'
        // 根据错误类型给出明确提示
        let msg = '定位获取失败'
        if (error.code === 1) {
          msg = '定位授权被拒绝，请在浏览器设置中允许位置访问'
        } else if (error.code === 2) {
          msg = '定位不可用，请检查设备GPS或网络'
        } else if (error.code === 3) {
          msg = '定位超时，请重试'
        }
        showFailToast(msg)
        resolve()
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    )
  })
}

/** 文件读取后上传 */
async function afterRead(fileItem) {
  const items = Array.isArray(fileItem) ? fileItem : [fileItem]
  items.forEach(item => { item.status = 'uploading'; item.message = '正在获取定位' })
  if (locationStatus.value !== 'success') {
    await getLocation()
  }
  await Promise.all(items.map(uploadOne))
}

async function uploadOne(fileItem) {
  if (!fileList.value.includes(fileItem)) return
  fileItem.status = 'uploading'
  fileItem.message = '上传中'
  try {
    const res = await uploadWithWatermark(fileItem.file, {
      // 标题和任务名称由后端从 h5-token 自动获取客户名称填充，前端不传联系人名称
      location: locationData.address,
      longitude: locationData.longitude,
      latitude: locationData.latitude
    })
    if (!fileList.value.includes(fileItem)) return
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
    // 预览走鉴权接口；失败只影响缩略图，不能把已成功的上传标成失败
    await bindUploaderWatermarkPreview(fileItem, payload.fileId || payload.url)
  } catch (e) {
    console.error('报修照片上传失败', e)
    fileItem.status = 'failed'
    fileItem.message = e?.message || '上传失败'
  }
}

/** 点击预览带水印图片 */
async function previewImage(fileItem) {
  const items = fileList.value.filter(f => f.url || f.content)
  const images = items.map(f => f.url || f.content)
  if (images.length === 0) return
  const startIndex = fileItem.url
    ? items.findIndex(f => f === fileItem)
    : items.findIndex(f => f.content === fileItem.content)
  await openEomsImagePreview(images, startIndex >= 0 ? startIndex : 0)
}

/** 删除照片 */
function beforeDelete(fileItem) {
  if (fileItem.status === 'uploading') {
    showFailToast('照片正在处理，请稍后再删除')
    return false
  }
  return true
}

/** 文件超出大小限制 */
function onOversize() {
  showFailToast('图片大小不能超过 10MB')
}

async function onSubmit() {
  if (loading.value) return
  if (fileList.value.some(item => !['done', 'failed'].includes(item.status))) {
    showFailToast('照片正在处理，请等待上传完成')
    return
  }
  const uploadedIds = fileList.value.filter(item => item.status === 'done' && item.fileId).map(item => item.fileId)
  if (uploadedIds.length < 1) {
    showFailToast('请至少上传 1 张故障现场照片')
    return
  }
  loading.value = true
  try {
    form.photos = uploadedIds.join(',')
    const res = await submitApply(form)
    if (res) {
      showSuccessToast('报修提交成功')
      setTimeout(() => {
        router.replace('/customer/repairs')
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
.form {
  padding: 12px 0;
}
.section {
  margin-top: 12px;
}
.submit-area {
  margin: 24px 16px;
}
.location-tip {
  padding: 8px 16px;
}
</style>
