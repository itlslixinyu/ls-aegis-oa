<template>
  <GiPageLayout>
    <BaseTable
      row-key="id"
      :data="dataList"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search
          v-model="queryForm.customerName"
          placeholder="搜索客户名称"
          allow-clear
          @search="search"
        />
        <a-input
          v-model="queryForm.repairNo"
          placeholder="报修编号"
          allow-clear
          style="width: 140px"
          @press-enter="search"
        />
        <a-select
          v-model="queryForm.status"
          placeholder="报修状态"
          allow-clear
          style="width: 140px"
          :options="statusOptions"
          @change="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['eoms:repair:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
      </template>
      <template #status="{ record }">
        <GiCellTag :value="record.status" :dict="eoms_apply_status" />
      </template>
      <template #action="{ record }">
        <a-space wrap>
          <a-link
            v-if="record.status === 'WAIT'"
            v-permission="['eoms:repair:confirm']"
            @click="onConfirm(record)"
          >
            确认
          </a-link>
          <a-link
            v-if="record.status === 'WAIT'"
            v-permission="['eoms:repair:cancel']"
            status="danger"
            @click="onCancel(record)"
          >
            取消
          </a-link>
          <a-link
            v-if="record.status === 'CONFIRM'"
            v-permission="['eoms:repair:assign']"
            @click="onToOrder(record)"
          >
            转工单
          </a-link>
          <a-link
            v-if="record.status === 'CONFIRM'"
            v-permission="['eoms:repair:close']"
            @click="onClose(record)"
          >
            关闭
          </a-link>
          <a-link v-permission="['eoms:repair:query']" @click="onView(record)">详情</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
    <a-modal v-model:visible="closeVisible" title="关闭报修" :on-before-ok="submitClose" :mask-closable="!closing" :esc-to-close="!closing">
      <a-textarea v-model="closeReason" placeholder="请输入关闭原因" :max-length="500" show-word-limit />
    </a-modal>

    <a-modal v-model:visible="detailVisible" title="报修详情" :footer="false" :width="560">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="报修编号">{{ detail.repairNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ detail.contactName }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ detail.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="地址">{{ detail.address || '-' }}</a-descriptions-item>
        <a-descriptions-item label="故障描述">{{ detail.faultDesc || '-' }}</a-descriptions-item>
        <a-descriptions-item v-if="detailPhotoList.length" label="现场照片">
          <a-space wrap>
            <EomsAuthImage
              v-for="(url, idx) in detailPhotoList"
              :key="idx"
              :src="url"
              width="72"
              height="72"
              fit="cover"
            />
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <GiCellTag :value="detail.status" :dict="eoms_apply_status" />
        </a-descriptions-item>
        <a-descriptions-item label="备注">{{ detail.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import AddModal from './ApplyAddModal.vue'
import EomsAuthImage from '@/components/EomsAuthImage/index.vue'
import {
  type RepairApplyResp,
  cancelRepairApply,
  closeRepairApply,
  confirmRepairApply,
  getRepairApply,
  listRepairApply,
  toOrderRepairApply,
} from '@/apis/eoms'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'

defineOptions({ name: 'EomsRepairApply' })

const { eoms_apply_status } = useDict('eoms_apply_status')

const statusOptions = computed(() =>
  (eoms_apply_status.value || []).map((d) => ({ label: d.label, value: d.value })),
)

const queryForm = reactive<Record<string, any>>({})
const detailVisible = ref(false)
const detail = ref<Partial<RepairApplyResp>>({})
const detailPhotoList = computed(() => {
  const src = detail.value.photoUrls || ''
  if (!src) return []
  return src.split(',').filter(Boolean)
})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listRepairApply({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '报修编号', dataIndex: 'repairNo', minWidth: EomsCol.codeMin, ellipsis: false },
  { title: '客户', dataIndex: 'customerName', width: EomsCol.name },
  { title: '联系人', dataIndex: 'contactName', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', width: 130 },
  { title: '故障描述', dataIndex: 'faultDesc', ellipsis: true, tooltip: true },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: EomsCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: EomsCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: EomsCol.actionL },
]

const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const onAdd = () => AddModalRef.value?.onAdd()

const onView = async (record: RepairApplyResp) => {
  const { data } = await getRepairApply(record.id)
  detail.value = data || record
  detailVisible.value = true
}

const runAction = (content: string, api: () => Promise<any>) => {
  Modal.warning({
    title: '提示',
    content,
    hideCancel: false,
    onOk: async () => {
      await api()
      Message.success('操作成功')
      search()
    },
  })
}

const onConfirm = (record: RepairApplyResp) =>
  runAction(`确认报修单「${record.repairNo || record.id}」？`, () => confirmRepairApply(record.id))
const onCancel = (record: RepairApplyResp) =>
  runAction(`取消报修单「${record.repairNo || record.id}」？`, () => cancelRepairApply(record.id))
const closeVisible = ref(false)
const closeReason = ref('')
const closeId = ref('')
const closing = ref(false)
const onClose = (record: RepairApplyResp) => {
  if (closing.value) return
  closeId.value = record.id
  closeReason.value = ''
  closeVisible.value = true
}
const submitClose = async () => {
  if (closing.value) return false
  const id = closeId.value
  const reason = closeReason.value.trim()
  if (!id || !reason) { Message.warning('请输入关闭原因'); return false }
  if (reason.length > 500) { Message.warning('关闭原因长度不能超过500个字符'); return false }
  closing.value = true
  try {
    await closeRepairApply(id, reason)
    Message.success('报修单已关闭')
    search()
    return true
  } catch { return false }
  finally { closing.value = false }
}
const onToOrder = (record: RepairApplyResp) =>
  runAction(`将报修单「${record.repairNo || record.id}」转为工单？`, () => toOrderRepairApply(record.id))
</script>
