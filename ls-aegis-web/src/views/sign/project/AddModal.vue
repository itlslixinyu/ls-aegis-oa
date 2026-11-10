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
      <template #contractFileIds>
        <PlatformFileField
          v-model="form.contractFileIds"
          disabled
          multiple
          variant="drag"
          drag-title="关联合同后自动带出附件"
          tip="选择关联合同后自动带出；签章时可直接使用，无需在此重复上传。"
        />
      </template>
      <template #otherFileIds>
        <PlatformFileField
          v-model="form.otherFileIds"
          multiple
          variant="drag"
          drag-title="点击或拖拽上传其他文件"
          parent-path="/sign"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg"
          tip="仅补充非合同文档；已关联合同则不必再上传合同附件。可多文件，上传后可点击预览。"
        />
      </template>
    </GiForm>
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { getContract } from '@/apis/eoms/contract'
import { getContractPurchase, getContractSale } from '@/apis/psi'
import { addProject, availableSeals, getProject, listProjectContracts, updateProject } from '@/apis/sign'
import { listAllUser } from '@/apis/system/user'
import { listDept } from '@/apis/system/dept'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import PlatformFileField from '@/components/PlatformFileField/index.vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useResetReactive } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'
import { ensureSelectOpt, toBizId, toBizIdList, toUserSelectOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { sign_project_status } = useDict('sign_project_status')

const dataId = ref<string | number>('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改项目' : '新增项目'))
const formRef = ref<InstanceType<typeof GiForm>>()

const userOptions = ref<{ label: string, value: string }[]>([])
const deptOptions = ref<{ label: string, value: string }[]>([])
const sealOptions = ref<{ label: string, value: string }[]>([])

type ContractOpt = {
  label: string
  value: string
  contractType: string
  contractId: string
  partnerName?: string
  fileId?: string
}

const TYPE_LABEL: Record<string, string> = {
  SALE: '销售',
  PURCHASE: '采购',
  REPAIR: '维修',
}

const [form, resetForm] = useResetReactive({
  status: '0',
  managerIds: [] as string[],
  sealIds: [] as string[],
  /** 复合键 TYPE:id，单选一份合同 */
  contractKey: undefined as string | undefined,
  partnerName: undefined as string | undefined,
  /** 关联合同附件（只读展示，不落库到 otherFileIds） */
  contractFileIds: undefined as string | undefined,
  /** 平台 sys_file.id，逗号分隔 */
  otherFileIds: undefined as string | undefined,
})

const contractOptions = ref<ContractOpt[]>([])

const toContractKey = (type?: string, id?: string | number) => {
  if (!type || id == null || id === '') return undefined
  return `${type}:${id}`
}

const parseContractKey = (key?: string) => {
  if (!key) return { contractType: undefined as string | undefined, contractId: undefined as string | undefined }
  const idx = key.indexOf(':')
  if (idx <= 0) return { contractType: undefined, contractId: undefined }
  return {
    contractType: key.slice(0, idx),
    // 雪花 ID 超过 Number 安全整数，保持字符串提交
    contractId: key.slice(idx + 1) || undefined,
  }
}

const fetchContractFileIds = async (type: string, id: string) => {
  try {
    if (type === 'SALE') {
      const { data } = await getContractSale(id)
      return data?.fileId || undefined
    }
    if (type === 'PURCHASE') {
      const { data } = await getContractPurchase(id)
      return data?.fileId || undefined
    }
    if (type === 'REPAIR') {
      const { data } = await getContract(id)
      return data?.fileId || undefined
    }
  } catch {
    // 附件拉取失败不阻断表单
  }
  return undefined
}

let syncSeq = 0
const syncContractExtras = async (key?: string) => {
  const seq = ++syncSeq
  if (!key) {
    form.partnerName = undefined
    form.contractFileIds = undefined
    return
  }
  const opt = contractOptions.value.find((o) => o.value === key)
  form.partnerName = opt?.partnerName || undefined
  if (opt?.fileId) {
    form.contractFileIds = opt.fileId
    return
  }
  // 选项未带回附件时（旧后端 / keepKey 兜底）再拉合同详情
  const { contractType, contractId } = parseContractKey(key)
  if (!contractType || !contractId) {
    form.contractFileIds = undefined
    return
  }
  const fileId = await fetchContractFileIds(contractType, contractId)
  if (seq !== syncSeq) return
  form.contractFileIds = fileId
  if (opt && fileId) {
    opt.fileId = fileId
  }
}

watch(
  () => form.contractKey,
  (key) => {
    void syncContractExtras(key)
  },
)

const loadContracts = async (keepKey?: string) => {
  contractOptions.value = []
  const excludeProjectId = dataId.value || undefined
  // 按类型分别拉取，前端打上 type（避免旧后端未回 contractType 时被过滤成空）
  const types = ['SALE', 'PURCHASE', 'REPAIR'] as const
  const settled = await Promise.allSettled(
    types.map((contractType) => listProjectContracts({ contractType, excludeProjectId: excludeProjectId == null ? undefined : String(excludeProjectId) })),
  )
  const opts: ContractOpt[] = []
  settled.forEach((result, i) => {
    if (result.status !== 'fulfilled') return
    const contractType = types[i]
    const list = Array.isArray(result.value.data) ? result.value.data : []
    list.forEach((c) => {
      if (c.id == null || c.id === '') return
      const contractId = String(c.id)
      const typeLabel = TYPE_LABEL[contractType] || contractType
      opts.push({
        label: [typeLabel, c.contractNo, c.contractName, c.partnerName].filter(Boolean).join(' · '),
        value: toContractKey(contractType, contractId)!,
        contractType,
        contractId,
        partnerName: c.partnerName,
        fileId: c.fileId || undefined,
      })
    })
  })
  if (keepKey && !opts.some((o) => o.value === keepKey)) {
    const { contractType, contractId } = parseContractKey(keepKey)
    if (contractType && contractId) {
      opts.unshift({
        label: `${TYPE_LABEL[contractType] || contractType} · ${contractId}`,
        value: keepKey,
        contractType,
        contractId,
      })
    }
  }
  contractOptions.value = opts
  // 选项就绪后再同步一次，避免先选后选项晚到导致往来单位/附件空白
  await syncContractExtras(form.contractKey)
}

const columns: ColumnItem[] = reactive([
  {
    label: '项目名称',
    field: 'projectName',
    type: 'input',
    span: 24,
    required: true,
    props: { maxLength: 100 },
  },
  {
    label: '负责人',
    field: 'managerIds',
    type: 'select',
    span: 12,
    required: true,
    props: {
      multiple: true,
      allowSearch: true,
      options: computed(() => userOptions.value),
    },
  },
  {
    label: '所属部门',
    field: 'deptId',
    type: 'select',
    span: 12,
    required: true,
    props: {
      allowSearch: true,
      options: computed(() => deptOptions.value),
    },
  },
  {
    label: '开始日期',
    field: 'startDate',
    type: 'date-picker',
    span: 12,
    required: true,
    props: { showTime: true, format: DATETIME_FORMAT, valueFormat: DATETIME_FORMAT },
  },
  {
    label: '结束日期',
    field: 'endDate',
    type: 'date-picker',
    span: 12,
    required: true,
    props: { showTime: true, format: DATETIME_FORMAT, valueFormat: DATETIME_FORMAT },
  },
  {
    label: '关联合同',
    field: 'contractKey',
    type: 'select',
    span: 12,
    props: {
      allowSearch: true,
      allowClear: true,
      placeholder: '选合同后自动带出附件；与其他文件至少一项',
      options: computed(() => contractOptions.value),
    },
  },
  {
    label: '往来单位',
    field: 'partnerName',
    type: 'input',
    span: 12,
    hide: (f: any) => !f.contractKey,
    props: { disabled: true, placeholder: '选择合同后自动带出' },
  },
  {
    label: '合同附件',
    field: 'contractFileIds',
    span: 24,
    hide: (f: any) => !f.contractKey,
  },
  {
    label: '其他文件',
    field: 'otherFileIds',
    span: 24,
  },
  {
    label: '项目状态',
    field: 'status',
    type: 'radio-group',
    span: 24,
    props: {
      options: computed(() => dictSelectOptions(sign_project_status.value)),
    },
  },
  {
    label: '可用印章',
    field: 'sealIds',
    type: 'select',
    span: 24,
    required: true,
    props: {
      multiple: true,
      allowSearch: true,
      options: computed(() => sealOptions.value),
    },
  },
  { label: '项目描述', field: 'description', type: 'textarea', span: 24 },
])

const flattenDept = (nodes: any[], result: any[] = []) => {
  nodes.forEach((node) => {
    if (node.status === 1 || node.status === '0' || node.status === 0) {
      result.push(node)
    }
    if (node.children?.length) flattenDept(node.children, result)
  })
  return result
}

const loadOptions = async () => {
  try {
    const [usersRes, deptRes, sealsRes] = await Promise.all([
      listAllUser({}),
      listDept({}),
      availableSeals(),
    ])
    const users = Array.isArray(usersRes.data) ? usersRes.data : []
    // 雪花 ID 全程用字符串，禁止 Number()（超安全整数会丢精度，Select 对不上 label）
    userOptions.value = toUserSelectOpts(users)
    const depts = flattenDept(Array.isArray(deptRes.data) ? deptRes.data : [])
    deptOptions.value = depts.map((d: any) => ({
      label: d.name || d.deptName,
      value: String(d.id ?? d.deptId),
    }))
    sealOptions.value = (sealsRes.data || []).map((s: any) => ({
      label: s.sealName,
      value: String(s.sealId),
    }))
  } catch {
    /* 选项加载失败时表单仍可打开 */
  }
}

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
  contractOptions.value = []
}

const toId = toBizId

const hasOtherFiles = () => {
  return toBizIdList(form.otherFileIds).length > 0
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    if (!form.contractKey && !hasOtherFiles()) {
      Message.warning('请关联合同或上传其他文件（至少一项）')
      return false
    }
    const { contractType, contractId } = parseContractKey(form.contractKey)
    const submitData: Record<string, any> = { ...form }
    delete submitData.contractKey
    delete submitData.contractFileIds
    if (Array.isArray(submitData.managerIds)) {
      submitData.managerIds = submitData.managerIds.join(',')
    }
    // 单选：只提交汇总字段，后端落到对应类型列并清空其余
    submitData.contractType = contractType ?? null
    submitData.contractId = contractId ?? null
    submitData.saleContractId = null
    submitData.purchaseContractId = null
    submitData.repairContractId = null
    submitData.otherFileIds = hasOtherFiles() ? String(form.otherFileIds) : null
    if (isUpdate.value) {
      await updateProject({ ...submitData, projectId: dataId.value })
      Message.success('修改成功')
    } else {
      await addProject(submitData)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const pickBoundKey = (data: Record<string, any>) => {
  const saleId = toId(data.saleContractId)
  const purchaseId = toId(data.purchaseContractId)
  const repairId = toId(data.repairContractId)
  if (saleId) return toContractKey('SALE', saleId)
  if (purchaseId) return toContractKey('PURCHASE', purchaseId)
  if (repairId) return toContractKey('REPAIR', repairId)
  if (data.contractType && data.contractId != null) {
    return toContractKey(String(data.contractType), data.contractId)
  }
  return undefined
}

const onAdd = async () => {
  dataId.value = ''
  reset()
  await loadOptions()
  await loadContracts()
  visible.value = true
}

const onUpdate = async (id: string | number) => {
  reset()
  dataId.value = id
  await loadOptions()
  const { data } = await getProject(String(id))
  const managerIds = toBizIdList(data.managerIds)
  const managerNames = typeof data.managerNames === 'string'
    ? data.managerNames.split(',').map((x) => x.trim())
    : []
  managerIds.forEach((id, i) => ensureSelectOpt(userOptions.value, id, managerNames[i]))
  ensureSelectOpt(deptOptions.value, data.deptId, data.deptName)
  toBizIdList(data.sealIds).forEach((sid) => ensureSelectOpt(sealOptions.value, sid))

  const contractKey = pickBoundKey(data)
  Object.assign(form, {
    ...data,
    managerIds,
    deptId: toBizId(data.deptId),
    sealIds: toBizIdList(data.sealIds),
    contractKey,
    contractFileIds: undefined,
    otherFileIds: data.otherFileIds || undefined,
    partnerName:
      data.partnerName
      || data.salePartnerName
      || data.purchasePartnerName
      || data.repairPartnerName,
  })
  await loadContracts(contractKey)
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
