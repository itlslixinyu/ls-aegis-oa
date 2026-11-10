<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 520 ? 520 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <a-alert style="margin-bottom: 16px" type="info" show-icon>
      {{ isSecondLevel ? '当前为二级分类：挂在所选一级分类下，编码如 1001、1101' : '当前为一级分类：编码如 10、11、12' }}
    </a-alert>
    <GiForm ref="formRef" v-model="form" :columns="columns" layout="vertical" />
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import {
  type GoodsCategoryResp,
  addGoodsCategory,
  getGoodsCategory,
  updateGoodsCategory,
} from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'

interface Props {
  categories: GoodsCategoryResp[]
}
const props = withDefaults(defineProps<Props>(), {
  categories: () => [],
})

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const formRef = ref<InstanceType<typeof GiForm>>()

const [form, resetForm] = useResetReactive({
  parentId: '0',
  sort: 999,
  status: 1,
})

/** 从「新增二级」进入时锁定上级，避免误改成一级 */
const parentLocked = ref(false)
const isSecondLevel = computed(() => form.parentId != null && String(form.parentId) !== '' && String(form.parentId) !== '0')
const title = computed(() => {
  if (isUpdate.value) {
    return isSecondLevel.value ? '修改二级分类' : '修改一级分类'
  }
  return isSecondLevel.value ? '新增二级分类' : '新增一级分类'
})

/** 上级仅可选「作为一级」或某一级分类；二级不可再作上级 */
const parentSelectOptions = computed(() => {
  const level1 = (props.categories || []).map((i) => ({
    key: String(i.id),
    title: `[一级] ${i.code ? `${i.code} ` : ''}${i.name}`,
  }))
  return [{ key: '0', title: '（无）作为一级分类', children: level1 }]
})

const columns: ColumnItem[] = reactive([
  {
    label: '上级分类',
    field: 'parentId',
    type: 'tree-select',
    span: 24,
    required: true,
    props: {
      data: parentSelectOptions,
      allowSearch: true,
      fallbackOption: false,
      disabled: computed(() => isUpdate.value || parentLocked.value),
      filterTreeNode(searchKey: string, nodeData: any) {
        return String(nodeData.title || '').toLowerCase().includes(String(searchKey || '').toLowerCase())
      },
    },
  },
  {
    label: '分类编码',
    field: 'code',
    type: 'input',
    span: 24,
    hide: () => !isUpdate.value,
    props: {
      disabled: true,
      placeholder: '系统自动生成（一级 10/20/30，二级 1001/2001）',
    },
  },
  { label: '分类名称', field: 'name', type: 'input', span: 24, required: true, props: { maxLength: 64 } },
  {
    label: '排序',
    field: 'sort',
    type: 'input-number',
    span: 12,
    props: { min: 1, mode: 'button' },
  },
  {
    label: '状态',
    field: 'status',
    type: 'switch',
    span: 12,
    props: { type: 'round', checkedValue: 1, uncheckedValue: 2, checkedText: '启用', uncheckedText: '禁用' },
  },
])

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
  parentLocked.value = false
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    const payload = {
      ...form,
      parentId: form.parentId == null || form.parentId === '' ? 0 : form.parentId,
      code: undefined,
    }
    if (isUpdate.value) {
      await updateGoodsCategory(payload, dataId.value)
      Message.success('修改成功')
    } else {
      await addGoodsCategory(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = (parentId?: string) => {
  reset()
  const pid = parentId != null && parentId !== '' ? String(parentId) : '0'
  form.parentId = pid
  parentLocked.value = pid !== '0'
  dataId.value = ''
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  const { data } = await getGoodsCategory(id)
  Object.assign(form, data, {
    parentId: data.parentId == null ? '0' : String(data.parentId),
  })
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
