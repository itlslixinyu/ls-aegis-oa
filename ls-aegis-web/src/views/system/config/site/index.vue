<template>
  <div class="gi_page">
    <a-spin :loading="loading" class="site-settings">
      <header class="site-header">
        <div>
          <h2>网站配置</h2>
          <p>管理品牌图片、平台名称和页脚信息。</p>
        </div>
        <a-space wrap>
          <template v-if="editing">
            <a-button :disabled="saving || !!uploading" @click="cancel">取消</a-button>
            <a-button v-permission="['system:siteConfig:update']" type="primary" :loading="saving" :disabled="!!uploading" @click="save">
              保存更改
            </a-button>
          </template>
          <template v-else>
            <a-button v-permission="['system:siteConfig:update']" :disabled="loading || !options.length" @click="confirmReset">
              恢复默认
            </a-button>
            <a-button v-permission="['system:siteConfig:update']" type="primary" :disabled="loading || !options.length" @click="editing = true">
              编辑配置
            </a-button>
          </template>
        </a-space>
      </header>

      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="saving">
        <section v-for="section in sections" :key="section.title" class="site-section">
          <div class="section-heading">
            <h3>{{ section.title }}</h3>
            <p>{{ section.description }}</p>
          </div>
          <div class="site-grid" :class="{ 'site-grid--images': section.images }">
            <a-form-item
              v-for="field in section.fields"
              :key="field.code"
              :field="field.code"
              :label="field.label"
              :class="{ 'site-field--wide': field.wide }"
            >
              <div v-if="section.images" class="image-preview">
                <img
                  v-if="previewSources[field.code] && brokenImages[field.code] !== previewSources[field.code]"
                  :key="previewSources[field.code]"
                  :src="previewSources[field.code]"
                  :alt="field.label + '预览'"
                  referrerpolicy="no-referrer"
                  @error="brokenImages[field.code] = previewSources[field.code]"
                />
                <span v-else>{{ form[field.code] ? '图片无法预览' : '未配置图片' }}</span>
              </div>
              <div v-if="field.imageSize" class="image-format">
                {{ field.imageSize.square ? field.imageSize.maxEdge + ' × ' + field.imageSize.maxEdge : '长边不超过 ' + field.imageSize.maxEdge }} px · PNG
              </div>

              <a-input
                v-model="form[field.code]"
                :max-length="field.maxLength ?? (field.short ? 8 : 512)"
                :readonly="!editing"
                :disabled="!!uploading"
                :placeholder="section.images ? '图片地址' : undefined"
                :allow-clear="editing"
                @change="section.images && updatePreview(field.code)"
              />

              <div v-if="section.images && editing" v-permission="['system:file:upload']" class="site-upload">
                <label class="upload-trigger" :class="{ 'upload-trigger--disabled': saving || !!uploading }">
                  <icon-upload aria-hidden="true" />
                  <span>{{ uploading === field.code ? '转换并上传中…' : '上传图片' }}</span>
                  <input
                    type="file"
                    :aria-label="'上传' + field.label"
                    accept="image/*,.svg,.ico,.heic,.heif,.tif,.tiff"
                    :disabled="saving || !!uploading"
                    @change="uploadSiteImage(field, $event)"
                  />
                </label>
              </div>
            </a-form-item>
          </div>
        </section>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { Message, Modal, type FormInstance } from '@arco-design/web-vue'
import { uploadFile } from '@/apis/system/file'
import { listOption, resetOptionValue, updateOption, type OptionResp } from '@/apis/system/option'
import { useAppStore } from '@/stores'
import { convertSiteImage, type SiteImageSize } from './site-image'

defineOptions({ name: 'SystemSiteConfig' })

interface SiteField {
  code: string
  label: string
  required?: boolean
  maxLength?: number
  short?: boolean
  wide?: boolean
  imageSize?: SiteImageSize
}

const sections: { title: string, description: string, images?: boolean, fields: SiteField[] }[] = [
  {
    title: '品牌图片',
    description: '选择图片后会按用途调整尺寸并转为 PNG。也可填写已有图片地址。',
    images: true,
    fields: [
      { code: 'SITE_LOGO', label: '系统 Logo', imageSize: { maxEdge: 512 } },
      { code: 'SITE_FAVICON', label: '网站图标', imageSize: { maxEdge: 32, square: true } },
      { code: 'SITE_BEIAN_GONGAN_ICON', label: '公安备案图标', imageSize: { maxEdge: 64, square: true } },
    ],
  },
  {
    title: '平台信息',
    description: '设置平台与公司的显示名称。',
    fields: [
      { code: 'SITE_TITLE', label: '系统名称', required: true },
      { code: 'SITE_TITLE_SHORT', label: '系统简称', maxLength: 32, required: true },
      { code: 'SITE_COMPANY', label: '公司名称', required: true },
      { code: 'SITE_COMPANY_SHORT', label: '公司简称', short: true, required: true },
      { code: 'SITE_DESCRIPTION', label: '系统描述', wide: true },
    ],
  },
  {
    title: '页脚与备案',
    description: '设置页面底部显示的版权与备案信息。',
    fields: [
      { code: 'SITE_COPYRIGHT', label: '版权声明', wide: true },
      { code: 'SITE_BEIAN', label: 'ICP备案号' },
      { code: 'SITE_BEIAN_GONGAN', label: '公安备案号' },
    ],
  },
]
const fields = sections.flatMap((section) => section.fields)
const imageFields = sections.filter((section) => section.images).flatMap((section) => section.fields)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, string>>({})
const previewSources = reactive<Record<string, string>>({})
const brokenImages = reactive<Record<string, string>>({})
const options = ref<OptionResp[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref('')
const editing = ref(false)
const appStore = useAppStore()
const query = { category: 'SITE' }
const rules: FormInstance['rules'] = Object.fromEntries(fields.filter((field) => field.required).map((field) => [
  field.code, [{ required: true, whitespace: true, message: `请输入${field.label}` }],
]))

function updatePreview(code: string) {
  const address = form[code]?.trim() || ''
  const source = /^(?:https?:\/\/|\/(?!\/))/i.test(address) ? address : ''
  setPreviewSource(code, source)
}

function setPreviewSource(code: string, source: string) {
  const previous = previewSources[code]
  if (previous === source) return
  if (previous?.startsWith('blob:')) URL.revokeObjectURL(previous)
  delete brokenImages[code]
  previewSources[code] = source
}

function cancel() {
  for (const field of fields) {
    form[field.code] = options.value.find((option) => option.code === field.code)?.value ?? ''
  }
  for (const field of imageFields) updatePreview(field.code)
  formRef.value?.clearValidate()
  editing.value = false
}

async function load() {
  loading.value = true
  try {
    const { data } = await listOption(query)
    options.value = data
    cancel()
  } finally {
    loading.value = false
  }
}

async function save() {
  if (saving.value || uploading.value) return
  if (await formRef.value?.validate() || saving.value || uploading.value || !editing.value) return
  saving.value = true
  try {
    // 只提交本表单识别且服务端已存在的参数，不改写其他 SITE 扩展配置。
    const codes = new Set(fields.map((field) => field.code))
    await updateOption(options.value.filter((option) => codes.has(option.code)).map((option) => ({
      id: option.id, code: option.code, value: form[option.code].trim(),
    })))
    await load()
    await appStore.initSiteConfig()
    Message.success('保存成功')
  } finally {
    saving.value = false
  }
}

async function uploadSiteImage({ code, imageSize }: SiteField, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !imageSize || !editing.value || uploading.value || saving.value) return
  uploading.value = code
  try {
    const converted = await convertSiteImage(file, imageSize).catch((error: unknown) => {
      Message.warning(error instanceof Error ? error.message : '图片转换失败，请换一张图片')
    })
    if (!converted || !editing.value) return
    const body = new FormData()
    body.append('file', converted)
    const { data } = await uploadFile(body)
    if (!editing.value) return
    if (!data?.url) {
      Message.error('上传响应未返回图片地址')
      return
    }
    // 本地存储使用同源文件代理，供后台和两端 H5 共用。
    form[code] = data.url.replace(/^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?\/(?:api\/)?file\//i, '/api/file/')
    setPreviewSource(code, URL.createObjectURL(converted))
    Message.success('图片已上传，点击保存更改后生效')
  } catch {
    // HTTP 层已显示上传错误。
  } finally {
    uploading.value = ''
  }
}

function confirmReset() {
  Modal.warning({
    title: '恢复默认配置',
    content: '确认将网站配置恢复为默认值吗？',
    hideCancel: false,
    onOk: async () => {
      loading.value = true
      try {
        await resetOptionValue(query)
        await load()
        await appStore.initSiteConfig()
        Message.success('恢复成功')
      } finally {
        loading.value = false
      }
    },
  })
}

onMounted(load)
onBeforeUnmount(() => {
  editing.value = false
  for (const field of imageFields) setPreviewSource(field.code, '')
})
</script>

<style scoped>
.site-settings {
  display: block;
  max-width: 1080px;
  margin-inline: auto;
}

.site-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px 20px;
  padding: 8px 0 20px;
}

.site-header h2,
.section-heading h3 {
  margin: 0;
  color: var(--color-text-1);
}

.site-header h2 {
  font-size: 22px;
  font-weight: 600;
}

.site-header p,
.section-heading p {
  margin: 5px 0 0;
  color: var(--color-text-2);
}

.site-section {
  margin-bottom: 16px;
  padding: 20px;
  border: 1px solid var(--color-border-2);
  border-radius: 10px;
  background: var(--color-bg-2);
}

.section-heading {
  margin-bottom: 18px;
}

.section-heading h3 {
  font-size: 16px;
  font-weight: 600;
}

.site-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.site-grid--images {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.site-grid :deep(.arco-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.site-grid--images :deep(.arco-form-item-content-wrapper) {
  min-width: 0;
}

.site-grid--images :deep(.arco-form-item-content) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.site-field--wide {
  grid-column: 1 / -1;
}

.image-preview {
  display: grid;
  height: 92px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  background: var(--color-fill-1);
  color: var(--color-text-3);
  font-size: 13px;
}

.image-preview img {
  width: 100%;
  height: 90px;
  padding: 10px;
  box-sizing: border-box;
  object-fit: contain;
}

.image-format {
  color: var(--color-text-3);
  font-size: 12px;
}

.upload-trigger {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--color-border-2);
  border-radius: 4px;
  color: rgb(var(--primary-6));
  cursor: pointer;
}

.upload-trigger:focus-within {
  outline: 2px solid rgb(var(--primary-6));
  outline-offset: 2px;
}

.upload-trigger--disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.upload-trigger input {
  position: absolute;
  inset: 0;
  width: 100%;
  opacity: 0;
  cursor: inherit;
}

@media (max-width: 850px) {
  .site-grid--images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .site-section {
    padding: 14px;
  }

  .site-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
