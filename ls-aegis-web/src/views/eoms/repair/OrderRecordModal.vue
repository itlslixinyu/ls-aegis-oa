<template>
  <a-modal
    v-model:visible="visible"
    :title="`维修记录 · ${orderNo || repairOrderId}`"
    :footer="false"
    :mask-closable="false"
    :width="width >= 720 ? 720 : '100%'"
    unmount-on-close
  >
    <div class="toolbar">
      <a-space>
        <a-button
          v-if="canAdd"
          v-permission="['eoms:record:add']"
          type="primary"
          size="small"
          @click="onAdd"
        >
          <template #icon><icon-plus /></template>
          补录记录
        </a-button>
        <a-button
          v-if="canEdit"
          v-permission="['eoms:record:update']"
          size="small"
          @click="onEdit"
        >
          修改记录
        </a-button>
      </a-space>
      <a-typography-text v-if="!rows.length" type="secondary">
        暂无维修记录；提交结算前须先补录（与师傅端共用同一份历史）
      </a-typography-text>
    </div>

    <a-spin :loading="loading" style="width: 100%">
      <a-empty v-if="!loading && !rows.length" description="暂无维修记录" />
      <a-descriptions
        v-for="(rec, idx) in rows"
        :key="rec.id"
        :column="1"
        bordered
        size="small"
        class="rec-block"
        :title="`记录 ${idx + 1}`"
      >
        <a-descriptions-item label="录入人">{{ rec.createBy || rec.engineerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="录入时间">{{ rec.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="故障原因">{{ rec.faultReason || '-' }}</a-descriptions-item>
        <a-descriptions-item label="检测过程">{{ rec.checkProcess || '-' }}</a-descriptions-item>
        <a-descriptions-item label="解决方案">{{ rec.solution || '-' }}</a-descriptions-item>
        <a-descriptions-item label="维修结果">{{ rec.result || '-' }}</a-descriptions-item>
        <a-descriptions-item v-if="photoList(rec, 'processing').length" label="维修中照片">
          <a-space wrap>
            <EomsAuthImage
              v-for="(url, pi) in photoList(rec, 'processing')"
              :key="'p' + pi"
              :src="url"
              width="72"
              height="72"
              fit="cover"
            />
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item v-if="photoList(rec, 'completed').length" label="完成照片">
          <a-space wrap>
            <EomsAuthImage
              v-for="(url, pi) in photoList(rec, 'completed')"
              :key="'c' + pi"
              :src="url"
              width="72"
              height="72"
              fit="cover"
            />
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item v-if="rec.remark" label="备注">{{ rec.remark }}</a-descriptions-item>
      </a-descriptions>
    </a-spin>

    <RecordAddModal ref="RecordAddModalRef" @save-success="onSaved" />
  </a-modal>
</template>

<script setup lang="ts">
import { useWindowSize } from '@vueuse/core'
import RecordAddModal from './RecordAddModal.vue'
import { type RepairRecordResp, listRepairRecord } from '@/apis/eoms'
import EomsAuthImage from '@/components/EomsAuthImage/index.vue'

const emit = defineEmits<{ (e: 'changed'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const loading = ref(false)
const editable = ref(false)
const repairOrderId = ref<string>('')
const orderNo = ref('')
const rows = ref<RepairRecordResp[]>([])
const RecordAddModalRef = ref<InstanceType<typeof RecordAddModal>>()

const canAdd = computed(() => editable.value && rows.value.length === 0)
const canEdit = computed(() => editable.value && rows.value.length > 0)

const unwrapList = (raw: any): RepairRecordResp[] => {
  if (Array.isArray(raw)) return raw
  if (Array.isArray(raw?.list)) return raw.list
  if (Array.isArray(raw?.records)) return raw.records
  if (Array.isArray(raw?.rows)) return raw.rows
  return []
}

const photoList = (rec: RepairRecordResp, kind: 'processing' | 'completed') => {
  const src =
    kind === 'processing'
      ? rec.processingPhotoUrls || rec.processingPhotos || ''
      : rec.completedPhotoUrls || rec.completedPhotos || ''
  return String(src)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

const loadRows = async () => {
  if (!repairOrderId.value) return
  loading.value = true
  try {
    const { data } = await listRepairRecord({
      repairOrderId: repairOrderId.value,
      page: 1,
      size: 20,
    })
    rows.value = unwrapList(data)
  } finally {
    loading.value = false
  }
}

const onAdd = () => {
  RecordAddModalRef.value?.onAddForOrder(repairOrderId.value, orderNo.value)
}

const onEdit = () => {
  const id = rows.value[0]?.id
  if (id != null) RecordAddModalRef.value?.onUpdate(String(id))
}

const onSaved = async () => {
  await loadRows()
  emit('changed')
}

const onOpen = async (orderId: string | number, no: string | undefined, canEditFlag: boolean) => {
  repairOrderId.value = String(orderId)
  orderNo.value = no || ''
  editable.value = canEditFlag
  rows.value = []
  visible.value = true
  await loadRows()
}

defineExpose({ onOpen, loadRows, hasRecord: () => rows.value.length > 0 })
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.rec-block {
  margin-bottom: 12px;
}
.rec-block:last-child {
  margin-bottom: 0;
}
</style>
