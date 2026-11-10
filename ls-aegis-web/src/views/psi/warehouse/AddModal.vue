<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 640 ? 640 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <GiForm ref="formRef" v-model="form" :columns="columns" layout="vertical" />
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { addWarehouse, getWarehouse, updateWarehouse } from '@/apis/psi'
import { listAllUser } from '@/apis/system/user'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { isSuperRole } from '@/constant/role'
import { useResetReactive } from '@/hooks'
import { useUserStore } from '@/stores'
import { ensureSelectOpt, toBizId, toUserSelectOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success', id?: string): void }>()
const { width } = useWindowSize()
const userStore = useUserStore()
/** 超管/租户管理员可指定任意负责人；普通用户只能负责自己的仓 */
const canPickAnyManager = computed(() => userStore.roles.some((r) => isSuperRole(r)))

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改仓库' : '新增仓库'))
const formRef = ref<InstanceType<typeof GiForm>>()
const userOptions = ref<{ label: string, value: string }[]>([])

const [form, resetForm] = useResetReactive({
  status: 1,
  isDefault: false,
  managerUserId: undefined as string | undefined,
  manager: undefined as string | undefined,
})

const columns: ColumnItem[] = reactive([
  { label: '仓库名称', field: 'warehouseName', type: 'input', span: 24, required: true, props: { maxLength: 64 } },
  {
    label: '负责人',
    field: 'managerUserId',
    type: 'select',
    span: 12,
    required: true,
    disabled: () => !canPickAnyManager.value,
    props: {
      options: userOptions,
      allowSearch: true,
      placeholder: '请选择负责人',
    },
  },
  {
    label: '默认仓',
    field: 'isDefault',
    type: 'switch',
    span: 12,
    props: { type: 'round', checkedValue: true, uncheckedValue: false },
  },
  { label: '地址', field: 'address', type: 'input', span: 24, props: { maxLength: 256 } },
  {
    label: '状态',
    field: 'status',
    type: 'switch',
    span: 24,
    props: { type: 'round', checkedValue: 1, uncheckedValue: 2, checkedText: '启用', uncheckedText: '禁用' },
  },
  { label: '备注', field: 'remark', type: 'textarea', span: 24 },
])

const ensureCurrentUserOption = () => {
  const currentUserId = toBizId(userStore.userInfo.id)
  if (!currentUserId) return
  if (!userOptions.value.some((o) => o.value === currentUserId)) {
    userOptions.value.push({
      label: userStore.nickname || currentUserId,
      value: currentUserId,
    })
  }
}

const loadUsers = async () => {
  if (canPickAnyManager.value) {
    const { data } = await listAllUser({})
    userOptions.value = toUserSelectOpts(data || [])
  } else {
    userOptions.value = []
  }
  ensureCurrentUserOption()
}

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    const selected = userOptions.value.find((o) => o.value === form.managerUserId)
    const payload = {
      ...form,
      manager: selected?.label || form.manager,
    }
    let savedId = dataId.value
    if (isUpdate.value) {
      await updateWarehouse(payload, dataId.value)
      Message.success('修改成功')
    } else {
      const { data } = await addWarehouse(payload)
      savedId = toBizId(data) || ''
      Message.success('新增成功')
    }
    emit('save-success', savedId || undefined)
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  dataId.value = ''
  reset()
  await loadUsers()
  const currentUserId = toBizId(userStore.userInfo.id)
  if (currentUserId) {
    form.managerUserId = currentUserId
  }
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  await loadUsers()
  const { data } = await getWarehouse(id)
  const managerUserId = toBizId(data.managerUserId)
  ensureSelectOpt(userOptions.value, managerUserId, data.manager)
  Object.assign(form, {
    ...data,
    managerUserId,
  })
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
