<template>
  <a-card title="基本信息" bordered class="gradient-card">
    <div class="body">
      <section>
        <a-upload
          accept="image/png,image/jpeg,image/gif,image/webp"
          :disabled="uploading"
          :show-file-list="false"
          list-type="picture-card"
          :show-upload-button="true"
          :on-before-upload="onBeforeUpload"
        >
          <template #upload-button>
            <Avatar :src="userInfo.avatar" :gender="userInfo.gender" :name="userStore.nickname" :size="100" trigger>
              <template #trigger-icon><icon-camera /></template>
            </Avatar>
          </template>
        </a-upload>
        <div class="name">
          <span style="margin-right: 10px">
            {{ userInfo.nickname }}
            <icon-man v-if="userInfo.gender === 1" style="color: #19bbf1" />
            <icon-woman v-else-if="userInfo.gender === 2" style="color: #fa7fa9" />
          </span>
          <icon-edit :size="16" class="btn" @click="onUpdate" />
        </div>
        <div class="id">
          <GiSvgIcon name="id" :size="16" />
          <span>{{ userInfo.id }}</span>
        </div>
      </section>
      <footer>
        <a-descriptions :column="4" size="large">
          <a-descriptions-item :span="4">
            <template #label> <icon-user /><span style="margin-left: 5px">用户名</span></template>
            {{ userInfo.username }}
          </a-descriptions-item>
          <a-descriptions-item :span="4">
            <template #label> <icon-phone /><span style="margin-left: 5px">手机</span></template>
            {{ userInfo.phone || '暂无' }}
          </a-descriptions-item>
          <a-descriptions-item :span="4">
            <template #label> <icon-email /><span style="margin-left: 5px">邮箱</span></template>
            {{ userInfo.email || '暂无' }}
          </a-descriptions-item>
          <a-descriptions-item :span="4">
            <template #label> <icon-mind-mapping /><span style="margin-left: 5px">部门</span></template>
            {{ userInfo.deptName }}
          </a-descriptions-item>
          <a-descriptions-item :span="4">
            <template #label> <icon-user-group /><span style="margin-left: 5px">角色</span></template>
            {{ userInfo.roleNames.join(' · ') }}
          </a-descriptions-item>
        </a-descriptions>
      </footer>
    </div>
    <div class="footer">注册于 {{ userInfo.registrationDate }}</div>
  </a-card>

  <a-modal v-model:visible="visible" title="上传头像" :width="width >= 400 ? 400 : '100%'" :footer="false" draggable @close="reset">
    <a-row>
      <a-col :span="14" style="width: 200px; height: 200px">
        <VueCropper
          ref="cropperRef"
          :img="options.img"
          :info="true"
          :auto-crop="options.autoCrop"
          :auto-crop-width="options.autoCropWidth"
          :auto-crop-height="options.autoCropHeight"
          :fixed-box="options.fixedBox"
          :fixed="options.fixed"
          :full="options.full"
          :center-box="options.centerBox"
          :can-move="options.canMove"
          :output-type="options.outputType"
          :output-size="options.outputSize"
          @real-time="handleRealTime"
        />
      </a-col>
      <a-col :span="10" style="display: flex; justify-content: center">
        <div :style="previewStyle">
          <div :style="previews.div">
            <img :src="previews.url" :style="previews.img" alt="" />
          </div>
        </div>
      </a-col>
    </a-row>
    <div style="text-align: center; padding-top: 30px">
      <a-space>
        <a-button type="primary" :loading="uploading" @click="handleUpload">确定</a-button>
        <a-button @click="reset">取消</a-button>
      </a-space>
    </div>
  </a-modal>
  <BasicInfoUpdateModal ref="BasicInfoUpdateModalRef" />
</template>

<script setup lang="ts">
import { useWindowSize } from '@vueuse/core'
import { Message } from '@arco-design/web-vue'
import { VueCropper } from 'vue-cropper'
import BasicInfoUpdateModal from './BasicInfoUpdateModal.vue'
import { uploadAvatar } from '@/apis/system'
import 'vue-cropper/dist/index.css'
import { useUserStore } from '@/stores'

const { width } = useWindowSize()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const options = reactive({
  img: '',
  autoCrop: true,
  autoCropWidth: 160,
  autoCropHeight: 160,
  fixedBox: true,
  fixed: true,
  full: false,
  centerBox: true,
  canMove: true,
  outputSize: 1,
  outputType: 'png',
})
const visible = ref(false)
const uploading = ref(false)
let disposed = false
let readVersion = 0
let uploadController: AbortController | undefined
// 打开裁剪框
const onBeforeUpload = (file: File): boolean => {
  if (uploading.value) return false
  if (!['image/png', 'image/jpeg', 'image/gif', 'image/webp'].includes(file.type)) {
    Message.error('请选择JPG/PNG/GIF/WebP图片'); return false
  }
  if (!file.size || file.size > 10 * 1024 * 1024) {
    Message.error('图片不能为空且不能超过10MB'); return false
  }
  const version = ++readVersion
  const reader = new FileReader()
  reader.onload = () => {
    if (disposed || version !== readVersion) return
    if (typeof reader.result === 'string') { options.img = reader.result; visible.value = true }
  }
  reader.onerror = () => { if (!disposed && version === readVersion) Message.error('图片读取失败，请重新选择') }
  reader.readAsDataURL(file)
  return false
}

// 重置
const reset = () => {
  readVersion++
  uploadController?.abort()
  uploading.value = false
  options.img = ''
  visible.value = false
}
onBeforeUnmount(() => { disposed = true; reset() })

const previews: any = ref({})
const previewStyle: any = ref({})
// 实时预览
const handleRealTime = (data: any) => {
  previewStyle.value = {
    width: `${data.w}px`,
    height: `${data.h}px`,
    overflow: 'hidden',
    margin: '0',
    zoom: 100 / data.h,
    borderRadius: '50%',
  }
  previews.value = data
}

const cropperRef = ref()
// 上传头像（裁剪固定输出 png，文件名与后端后缀校验一致）
const handleUpload = async () => {
  if (uploading.value || !options.img || !cropperRef.value) return
  uploading.value = true
  const version = readVersion
  const controller = new AbortController()
  uploadController = controller
  try {
  cropperRef.value.getCropBlob(async (data: Blob) => {
    try {
      if (disposed || version !== readVersion || controller.signal.aborted) return
      if (!data?.size || data.size > 10 * 1024 * 1024) { Message.error('裁剪图片无效或超过10MB'); return }
      const formData = new FormData()
      formData.append('avatarFile', data, 'avatar.png')
      const res = await uploadAvatar(formData, { signal: controller.signal })
      if (disposed || version !== readVersion || controller.signal.aborted) return
      userInfo.value.avatar = res.data.avatar
      await userStore.getInfo()
      if (disposed || version !== readVersion || controller.signal.aborted) return
      reset()
      Message.success('更新成功')
    } catch {
      // 错误由请求拦截器提示
    } finally {
      if (uploadController === controller) uploading.value = false
    }
  })
  } catch {
    uploading.value = false
    Message.error('图片裁剪失败，请重新选择')
  }
}

const BasicInfoUpdateModalRef = ref<InstanceType<typeof BasicInfoUpdateModal>>()
// 修改基本信息
const onUpdate = async () => {
  BasicInfoUpdateModalRef.value?.onUpdate()
}
</script>

<style scoped lang="scss">
:deep(.arco-avatar-trigger-icon-button) {
  width: 32px;
  height: 32px;
  line-height: 32px;
  background-color: #e8f3ff;
  .arco-icon-camera {
    margin-top: 8px;
    color: rgb(var(--arcoblue-6));
    font-size: 14px;
  }
}

.body {
  display: flex;
  flex-direction: column;
  padding: 28px 10px 20px 10px;
  .btn {
    cursor: pointer;
  }

  & > section {
    flex: 1 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 32px 0 50px;
    .name {
      font-size: 20px;
      margin: 20px 0;
    }
    .id {
      span {
        font-size: 12px;
        font-weight: 400;
        line-height: 20px;
        padding: 0 6px;
        color: var(--color-text-3);
      }
    }
  }

  & > footer .footer_item {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
    font-size: 12px;
  }
}

.footer {
  margin: 0 -16px;
  padding-top: 16px;
  font-size: 12px;
  text-align: center;
  border-top: 1px solid var(--color-border-2);
}
</style>
