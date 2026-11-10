<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 720 ? 720 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <GiForm ref="formRef" v-model="form" :columns="columns" layout="vertical">
      <template #sealFile>
        <div class="seal-source-row">
          <a-upload
            draggable
            :auto-upload="false"
            accept="image/png,image/jpeg,image/jpg,image/gif"
            :show-file-list="false"
            class="seal-source-upload"
            @change="onFileChange"
          >
            <template #upload-button>
              <div class="seal-source-card seal-source-card--upload" :class="{ 'is-filled': !!uploadPreviewUrl }">
                <template v-if="uploadPreviewUrl">
                  <img :src="uploadPreviewUrl" class="seal-source-card__preview" alt="上传印章" />
                  <a-button
                    class="seal-source-card__clear"
                    size="mini"
                    status="danger"
                    @click.stop="clearSealImage"
                  >
                    清除
                  </a-button>
                </template>
                <template v-else>
                  <div class="seal-source-card__badge">UP</div>
                  <div class="seal-source-card__title">点击或拖拽上传印章图片</div>
                  <div class="seal-source-card__desc">支持 JPG、PNG、GIF 格式，最大 50MB</div>
                </template>
              </div>
            </template>
            <template #tip />
          </a-upload>
          <div
            class="seal-source-card seal-source-card--get"
            :class="{ 'is-filled': !!extractPreviewUrl }"
            @click="openExtractPicker"
          >
            <template v-if="extractPreviewUrl">
              <img :src="extractPreviewUrl" class="seal-source-card__preview" alt="提取印章" />
              <a-button
                class="seal-source-card__clear"
                size="mini"
                status="danger"
                @click.stop="clearSealImage"
              >
                清除
              </a-button>
            </template>
            <template v-else>
              <div class="seal-source-card__badge">GET</div>
              <div class="seal-source-card__title">从提取记录获取</div>
              <div class="seal-source-card__desc">选择已提取的印章图片</div>
            </template>
          </div>
        </div>
      </template>
      <template #certInfo>
        <a-descriptions
          class="cert-info"
          :column="2"
          bordered
          size="small"
          :label-style="{ width: '100px' }"
        >
          <a-descriptions-item label="证书状态">{{ form.certStatus || '未绑定' }}</a-descriptions-item>
          <a-descriptions-item label="证书序列号">{{ form.serialNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="有效期">{{ form.notBefore || '-' }} ~ {{ form.notAfter || '-' }}</a-descriptions-item>
          <a-descriptions-item label="密钥引用">{{ form.keyRef || '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </GiForm>

    <a-modal v-model:visible="extractPickerVisible" title="选择印章图片" :width="700" :footer="false">
      <a-spin :loading="extractListLoading" style="width: 100%">
        <a-row :gutter="12">
          <a-col v-for="item in extractList" :key="item.id" :span="8" style="margin-bottom: 12px">
            <div class="extract-card" @click="selectFromExtract(item)">
              <img v-if="item.imageUrl" :src="item.imageUrl" />
              <div class="name">{{ item.extractFileName || '印章图片' }}</div>
            </div>
          </a-col>
        </a-row>
        <a-empty v-if="!extractListLoading && !extractList.length" description="暂无提取记录" />
      </a-spin>
    </a-modal>
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { addSeal,
  fetchSignImageObjectUrl,
  getExtractImageResource,
  getExtractList,
  getNextSealCode,
  getSeal,
  getSealCertInfo,
  getSealImageResource,
  updateSeal,
} from '@/apis/sign'
import { listDept } from '@/apis/system/dept'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { DATE_FORMAT } from '@/constant/datetime'
import { useResetReactive } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'
import { ensureSelectOpt, toBizId } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { sign_seal_category, sign_seal_status } = useDict('sign_seal_category', 'sign_seal_status')

const dataId = ref<string | number>('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改印章' : '新建印章'))
const formRef = ref<InstanceType<typeof GiForm>>()

const deptOptions = ref<{ label: string, value: string }[]>([])
const sealPreviewUrl = ref('')
const sealPreviewOwned = ref(false)
const uploadFile = ref<File | null>(null)
const selectedExtractId = ref<string | number | null>(null)

/** 上传来源预览（含编辑态已有图片）；提取来源预览只出现在右侧框 */
const uploadPreviewUrl = computed(() => (selectedExtractId.value ? '' : sealPreviewUrl.value))
const extractPreviewUrl = computed(() => (selectedExtractId.value ? sealPreviewUrl.value : ''))

const extractPickerVisible = ref(false)
const extractListLoading = ref(false)
const extractList = ref<Array<{ id: string | number, extractFileName?: string, imageUrl?: string }>>([])

const [form, resetForm] = useResetReactive({
  status: '0',
  sealName: '',
  sealCode: '',
  sealCategory: '',
  enableDate: '',
  deptId: undefined as string | number | undefined,
  remark: '',
  certStatus: '',
  serialNo: '',
  notBefore: '',
  notAfter: '',
  keyRef: '',
})

const columns: ColumnItem[] = reactive([
  { label: '印章名称', field: 'sealName', type: 'input', span: 12, required: true, props: { maxLength: 100 } },
  { label: '印章编号', field: 'sealCode', type: 'input', span: 12, props: { maxLength: 50 } },
  {
    label: '印章类型',
    field: 'sealCategory',
    type: 'select',
    span: 12,
    required: true,
    props: { options: computed(() => dictSelectOptions(sign_seal_category.value)) },
  },
  {
    label: '启用日期',
    field: 'enableDate',
    type: 'date-picker',
    span: 12,
    required: true,
    props: { format: DATE_FORMAT, valueFormat: DATE_FORMAT },
  },
  {
    label: '所属部门',
    field: 'deptId',
    type: 'select',
    span: 12,
    props: { allowSearch: true, options: computed(() => deptOptions.value) },
  },
  {
    label: '印章状态',
    field: 'status',
    type: 'select',
    span: 12,
    required: true,
    props: {
      options: computed(() => dictSelectOptions(sign_seal_status.value)),
    },
  },
  { label: '印章图片', field: 'sealFile', span: 24 },
  { label: '备注说明', field: 'remark', type: 'textarea', span: 24, props: { maxLength: 1000 } },
  { label: '平台证书', field: 'certInfo', span: 24 },
])

const setSealPreviewUrl = (url: string, owned = true) => {
  if (sealPreviewUrl.value && sealPreviewOwned.value) URL.revokeObjectURL(sealPreviewUrl.value)
  sealPreviewUrl.value = url
  sealPreviewOwned.value = !!url && owned
}

const clearSealImage = () => {
  setSealPreviewUrl('', false)
  uploadFile.value = null
  selectedExtractId.value = null
}

const flattenDept = (nodes: any[], result: any[] = []) => {
  nodes.forEach((node) => {
    if (node.status === 1 || node.status === '0' || node.status === 0) result.push(node)
    if (node.children?.length) flattenDept(node.children, result)
  })
  return result
}

const loadDeptOptions = async () => {
  try {
    const res = await listDept({})
    const depts = flattenDept(Array.isArray(res.data) ? res.data : [])
    deptOptions.value = depts.map((d: any) => ({
      label: d.name || d.deptName,
      value: String(d.id ?? d.deptId),
    }))
  } catch { /* ignore */ }
}

const formatDate = (date = new Date()) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const onFileChange = (fileList: any[]) => {
  const fileItem = fileList?.[fileList.length - 1]
  const file = fileItem?.file as File | undefined
  if (!file) return
  const okType = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif'].includes(file.type)
  if (!okType) {
    Message.error('只能上传 JPG/PNG/GIF 图片')
    return
  }
  if (file.size / 1024 / 1024 >= 50) {
    Message.error('图片大小不能超过 50MB')
    return
  }
  uploadFile.value = file
  selectedExtractId.value = null
  setSealPreviewUrl(URL.createObjectURL(file), true)
}

const openExtractPicker = async () => {
  extractPickerVisible.value = true
  extractListLoading.value = true
  extractList.value.forEach((i) => i.imageUrl && URL.revokeObjectURL(i.imageUrl))
  try {
    const res = await getExtractList({ page: 1, size: 50 })
    extractList.value = await Promise.all(
      (res.data?.list || []).map(async (item) => {
        let imageUrl = ''
        try {
          imageUrl = await fetchSignImageObjectUrl(getExtractImageResource(item.id))
        } catch { /* ignore */ }
        return {
          id: item.id,
          extractFileName: item.extractFileName || item.extractFileDisplayName || '印章图片',
          imageUrl,
        }
      }),
    )
  } finally {
    extractListLoading.value = false
  }
}

const selectFromExtract = (item: { id: string | number, imageUrl?: string }) => {
  uploadFile.value = null
  selectedExtractId.value = item.id
  setSealPreviewUrl(item.imageUrl || '', false)
  extractPickerVisible.value = false
  Message.success('已选择，可继续完善其他信息')
}

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
  clearSealImage()
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    if (isUpdate.value) {
      await updateSeal({ ...form, sealId: dataId.value })
      Message.success('修改成功')
    } else {
      if (!uploadFile.value && !selectedExtractId.value) {
        Message.warning('请上传印章图片或从提取记录选择')
        return false
      }
      const fd = new FormData()
      if (uploadFile.value) fd.append('file', uploadFile.value)
      if (selectedExtractId.value) fd.append('extractRecordId', String(selectedExtractId.value))
      fd.append('sealName', form.sealName || '')
      fd.append('sealCode', form.sealCode || '')
      fd.append('sealCategory', form.sealCategory || '')
      fd.append('enableDate', form.enableDate || '')
      if (form.deptId) fd.append('deptId', String(form.deptId))
      fd.append('status', form.status || '0')
      if (form.remark) fd.append('remark', form.remark)
      await addSeal(fd)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  dataId.value = ''
  reset()
  form.enableDate = formatDate()
  await loadDeptOptions()
  try {
    const res = await getNextSealCode()
    if (res.data) form.sealCode = res.data
  } catch { /* ignore */ }
  visible.value = true
}

const onUpdate = async (id: string | number) => {
  reset()
  dataId.value = id
  await loadDeptOptions()
  const { data } = await getSeal(String(id))
  Object.assign(form, {
    ...data,
    deptId: toBizId(data.deptId),
    enableDate: data.enableDate || formatDate(),
  })
  ensureSelectOpt(deptOptions.value, form.deptId, (data as any).deptName)
  try {
    const cert = await getSealCertInfo(String(id))
    Object.assign(form, cert.data || {})
  } catch { /* ignore */ }
  if (data.sealFilePath) {
    try {
      setSealPreviewUrl(await fetchSignImageObjectUrl(getSealImageResource(String(id))), true)
    } catch {
      Message.error('印章图片加载失败')
    }
  }
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>

<style scoped>
.seal-source-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.seal-source-upload {
  flex: 1;
  min-width: 0;
}
.seal-source-upload :deep(.arco-upload),
.seal-source-upload :deep(.arco-upload-trigger),
.seal-source-upload :deep(.arco-upload-list) {
  width: 100%;
}
.seal-source-upload :deep(.arco-upload-drag) {
  padding: 0;
  border: none;
  background: transparent;
}
.seal-source-upload :deep(.arco-upload-tip),
.seal-source-upload :deep(.arco-upload-drag-icon) {
  display: none;
}
.seal-source-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  min-height: 140px;
  height: 140px;
  padding: 16px 12px;
  border: 1px dashed var(--color-border-3);
  border-radius: 6px;
  text-align: center;
  cursor: pointer;
  box-sizing: border-box;
  overflow: hidden;
  transition: border-color 0.2s, background-color 0.2s;
}
.seal-source-card--upload {
  border-color: rgb(var(--primary-3));
  background: rgba(var(--primary-1), 0.45);
}
.seal-source-card--upload:hover {
  border-color: rgb(var(--primary-6));
  background: rgba(var(--primary-1), 0.7);
}
.seal-source-card--get {
  flex: 1;
  min-width: 0;
  background: var(--color-bg-2);
}
.seal-source-card--get:hover {
  border-color: rgb(var(--primary-6));
}
.seal-source-card.is-filled {
  padding: 8px;
  background: var(--color-fill-1);
  border-style: solid;
}
.seal-source-card__badge {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 1px;
  color: var(--color-text-4);
}
.seal-source-card__title {
  font-size: 14px;
  line-height: 1.4;
  color: var(--color-text-1);
}
.seal-source-card__desc {
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text-3);
}
.seal-source-card__preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.seal-source-card__clear {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
}
.cert-info {
  display: block;
  width: 100%;
}
.cert-info :deep(.arco-descriptions-body) {
  width: 100%;
}
.cert-info :deep(.arco-descriptions-table) {
  width: 100% !important;
  table-layout: auto;
}
.cert-info :deep(.arco-descriptions-item-label-block),
.cert-info :deep(.arco-descriptions-item-label) {
  width: 100px;
  white-space: nowrap;
  vertical-align: middle;
}
.cert-info :deep(.arco-descriptions-item-value-block),
.cert-info :deep(.arco-descriptions-item-value) {
  word-break: break-all;
  vertical-align: middle;
}
.extract-card {
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  text-align: center;
}
.extract-card:hover {
  border-color: rgb(var(--primary-6));
}
.extract-card img {
  width: 100%;
  height: 120px;
  object-fit: contain;
  background: var(--color-fill-1);
}
.extract-card .name {
  padding: 8px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 640px) {
  .seal-source-row {
    flex-direction: column;
  }
}
</style>
