<template>
  <main class="field-page">
    <van-nav-bar title="现场报修" left-arrow @click-left="router.back()" />
    <van-cell-group inset title="报修客户">
      <form role="search" class="customer-search" @submit.prevent="search">
        <label class="search-input">
          <span class="magnifier" aria-hidden="true"></span>
          <input v-model="term" type="search" aria-label="搜索客户名称或编号" placeholder="搜索客户名称或编号" enterkeyhint="search" @input="resetSearch" />
        </label>
        <button type="submit" class="search-action">搜索</button>
      </form>
      <van-cell v-for="item in matches" :key="item.id" :title="item.name" :label="item.code" is-link @click="select(item)" />
      <van-cell v-if="searched && !matches.length" title="未找到匹配的客户" />
      <van-cell v-if="searched" title="新建客户" icon="add-o" is-link @click="openCustomerForm" />
      <div v-if="form.customerId" class="selected-customer" role="status">
        <span>已选客户</span><strong>{{ form.customerName }}</strong>
      </div>
    </van-cell-group>
    <van-popup v-model:show="creating" position="bottom" round safe-area-inset-bottom :close-on-click-overlay="!saving">
      <van-form class="customer-form" @submit="saveCustomer">
        <h2>新建客户</h2>
        <van-field v-model.trim="customerName" label="客户全称" required maxlength="128" placeholder="请输入客户全称" :rules="[{ required: true, message: '请填写客户全称' }]" />
        <van-field label="客户性质">
          <template #input><van-radio-group v-model="nature" direction="horizontal"><van-radio name="ENTERPRISE">企业</van-radio><van-radio name="PERSON">个人</van-radio></van-radio-group></template>
        </van-field>
        <div class="actions"><van-button block native-type="button" :disabled="saving" @click="creating = false">取消</van-button><van-button block type="primary" native-type="submit" :loading="saving">保存并选用</van-button></div>
      </van-form>
    </van-popup>
    <van-form @submit="submit">
      <van-cell-group inset title="本次上门信息">
        <van-field v-model.trim="form.contactName" label="联系人" required maxlength="50" />
        <van-field v-model.trim="form.contactPhone" label="联系电话" type="tel" required maxlength="30" />
        <van-field v-model.trim="form.address" label="服务地址" required maxlength="500" />
        <van-field v-model.trim="form.equipment" label="设备名称" maxlength="32" placeholder="选填" />
        <van-field v-model.trim="form.fault" label="故障描述" type="textarea" autosize required maxlength="500" placeholder="请填写至少5个字" />
      </van-cell-group>
      <van-cell-group v-if="related.length" inset title="该客户尚未完成的报修">
        <van-cell v-for="item in related" :key="item.repairNo" :title="item.repairNo + ' · ' + item.fault"
          :label="[item.address, item.engineer].filter(Boolean).join(' · ')"
          :value="item.ownOrderId ? '继续处理' : ''" :is-link="!!item.ownOrderId"
          @click="item.ownOrderId && router.push('/engineer/task/' + item.ownOrderId)" />
        <van-cell title="查看可领取的工单" is-link @click="router.push('/engineer/pool')" />
      </van-cell-group>
      <PhotoUploadSection title="现场照片" :files="files" :max-count="9">
        <van-uploader v-model="files" multiple accept="image/png,image/jpeg" :max-count="9"
          :data-photo-remaining="9-files.length" :after-read="enqueue" :before-delete="remove"
          :preview-full-image="false" @click-preview="preview" />
        <PhotoUploadFeedback :files="files" @retry="photos.retry" />
      </PhotoUploadSection>
      <div class="actions">
        <van-button native-type="button" block @click="saveDraft">暂存资料</van-button>
        <van-button type="primary" native-type="submit" block :loading="submitting" :disabled="checking || !checked">建单并接单</van-button>
      </div>
      <van-cell v-if="form.customerId && !checked && !checking" title="重新核对客户报修" is-link @click="checkOpen()" />
    </van-form>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '@/store/user'
import { createFieldCustomer, findFieldCustomers, getOpenFieldRepairs, submitFieldRepair, uploadFieldPhoto } from '@/api/engineer'
import { createPhotoUploadSession, photoSubmissionError } from '@/utils/photoUploadSession'
import { bindUploaderWatermarkPreview, releaseEomsFileObjectUrl, openEomsImagePreview, closeEomsImagePreview } from '@/utils/eomsFile'
import PhotoUploadSection from '@/components/PhotoUploadSection.vue'
import PhotoUploadFeedback from '@/components/PhotoUploadFeedback.vue'

const router = useRouter()
const store = useUserStore()
const draftKey = 'field-repair:' + store.engineerInfo?.user?.userName
function uuid() {
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 15) | 64
  bytes[8] = (bytes[8] & 63) | 128
  const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')
  return [hex.slice(0,8), hex.slice(8,12), hex.slice(12,16), hex.slice(16,20), hex.slice(20)].join('-')
}
const form = reactive({ customerId: '', customerName: '', address: '', contactName: '', contactPhone: '',
  fault: '', equipment: '', requestId: uuid() })
const term = ref('')
const matches = ref([])
const related = ref([])
const files = ref([])
const searched = ref(false)
const submitting = ref(false)
const checking = ref(false)
const checked = ref(false)
let finished = false
let searchVersion = 0
const photos = createPhotoUploadSession({
  getItems: () => files.value,
  upload: (file, _, opts) => uploadFieldPhoto(file, opts),
  preview: bindUploaderWatermarkPreview,
  release: releaseEomsFileObjectUrl,
})
function preview(item) {
  const items = files.value.filter(f => f.fileId || f.content)
  if (items.includes(item)) openEomsImagePreview(items.map(f => f.fileId || f.content), items.indexOf(item),
    { owner: photos, isActive: photos.isActive })
}
function enqueue(items) { for (const item of [].concat(items)) photos.enqueue(item) }
function remove(item) { photos.remove(item); return true }
onBeforeUnmount(() => { photos.dispose(); closeEomsImagePreview(photos) })
onMounted(() => {
  const raw = sessionStorage.getItem(draftKey)
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    Object.assign(form, draft.form)
    files.value = draft.photos.map(fileId => ({ fileId, status: 'done', isImage: false, file: { name: '已保存照片' } }))
    if (form.customerId) checkOpen()
  } catch { sessionStorage.removeItem(draftKey) }
})
const creating = ref(false)
const saving = ref(false)
const customerName = ref('')
const nature = ref('ENTERPRISE')
function resetSearch() {
  ++searchVersion
  searched.value = false
  matches.value = []
}
function openCustomerForm() {
  customerName.value = term.value.trim()
  nature.value = 'ENTERPRISE'
  creating.value = true
}
async function saveCustomer() {
  if (saving.value || !customerName.value.trim()) return
  saving.value = true
  try {
    const res = await createFieldCustomer({ name: customerName.value.trim(), nature: nature.value })
    creating.value = false
    term.value = res.data.name
    await select(res.data)
    showToast('客户已选用')
  } finally { saving.value = false }
}
async function search() {
  const version = ++searchVersion
  searched.value = false
  matches.value = []
  const res = await findFieldCustomers(term.value)
  if (version === searchVersion) { matches.value = res.data; searched.value = true }
}
async function select(item) {
  ++searchVersion
  const changed = form.customerId && form.customerId !== item.id
  Object.assign(form, { customerId: item.id, customerName: item.name,
    address: (!changed && form.address) || item.address || '',
    contactName: (!changed && form.contactName) || item.contactName || '',
    contactPhone: (!changed && form.contactPhone) || item.contactPhone || '' })
  matches.value = []
  searched.value = false
  await checkOpen()
}
let checkVersion = 0
async function checkOpen() {
  const version = ++checkVersion
  checked.value = false
  checking.value = true
  related.value = []
  try {
    const res = await getOpenFieldRepairs(form.customerId)
    if (version === checkVersion) { related.value = res.data; checked.value = true }
  } finally { if (version === checkVersion) checking.value = false }
}
function persist() {
  sessionStorage.setItem(draftKey, JSON.stringify({ form, photos: files.value.map(f => f.fileId) }))
}
function saveDraft() {
  const error = photoSubmissionError(files.value)
  if (error) return showToast(error)
  persist()
  showToast('已暂存在当前浏览器标签页')
}
onBeforeRouteLeave(async () => {
  if (submitting.value && !finished) return false
  if (finished || !files.value.length) return true
  try { await showConfirmDialog({ title: '离开现场报修？', message: '离开前可点暂存资料保存填写内容。' }); return true }
  catch { return false }
})
async function submit() {
  if (submitting.value || !checked.value) return
  if (!form.contactName || !form.contactPhone || !form.address || form.fault.length < 5) return showToast('请填写联系人、电话、地址和至少5字的故障描述')
  const error = photoSubmissionError(files.value)
  if (error) return showToast(error)
  submitting.value = true
  try {
    if (related.value.length) {
      try { await showConfirmDialog({ title: '确认是另一项故障？', message: '该客户还有未完成的报修。同一故障可返回继续处理。', confirmButtonText: '新建工单' }) }
      catch { return }
    }
    persist()
    const res = await submitFieldRepair({ ...form, photos: files.value.map(f => f.fileId) })
    finished = true
    sessionStorage.removeItem(draftKey)
    await router.replace('/engineer/task/' + res.data.orderId)
  } finally { submitting.value = false }
}
</script>

<style scoped>
.field-page { max-width: 720px; margin-inline: auto; padding-bottom: 24px; background: var(--van-background); min-height: 100vh; }
.photo-section, .actions { margin: 16px; }
.actions { display: flex; gap: 12px; }
.customer-search { display: flex; gap: 8px; align-items: center; padding: 12px 16px; }
.search-input { display: flex; flex: 1; align-items: center; gap: 10px; min-width: 0; height: 44px; box-sizing: border-box; padding: 0 14px; border: 1px solid #d3dfdd; border-radius: 12px; background: #f4f8f7; }
.search-input:focus-within { border-color: #075f57; }
.search-input input { width: 100%; min-width: 0; padding: 0; border: 0; outline: 0; background: transparent; font: inherit; font-size: 16px; color: #163e39; }
.search-input input::placeholder { color: #61746f; }
.magnifier { position: relative; box-sizing: border-box; flex: 0 0 15px; height: 15px; border: 2px solid #075f57; border-radius: 50%; }
.magnifier::after { content: ""; position: absolute; width: 7px; height: 2px; background: #075f57; border-radius: 2px; transform: rotate(45deg); right: -5px; bottom: -2px; }
.search-action { height: 44px; flex: none; padding: 0 16px; border: 0; border-radius: 12px; background: #075f57; color: white; font-size: 15px; font-weight: 600; }
.selected-customer { margin: 0 16px 12px; padding: 10px 12px; display: flex; align-items: baseline; gap: 10px; background: #edf6f3; border-radius: 10px; color: #164b43; }
.selected-customer span { flex: none; font-size: 13px; }
.selected-customer strong { font-size: 15px; overflow-wrap: anywhere; }
.customer-form h2 { margin: 20px 16px 12px; font-size: 18px; }
button { cursor: pointer; }
</style>
