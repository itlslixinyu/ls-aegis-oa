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
import mapTree from 'xe-utils/mapTree'
import { addGoods, getGoods, listGoodsCategory, updateGoods } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useDict } from '@/hooks/app'
import { useResetReactive } from '@/hooks'
import { toBizId } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success', id?: string): void }>()
const { width } = useWindowSize()
const { psi_tax_rate } = useDict('psi_tax_rate')

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改商品' : '新增商品'))
const formRef = ref<InstanceType<typeof GiForm>>()
const categoryTree = ref<any[]>([])

const [form, resetForm] = useResetReactive({
  status: 1,
  unit: '个',
  defaultTaxRate: 13,
})

const columns: ColumnItem[] = reactive([
  { label: '商品名称', field: 'goodsName', type: 'input', span: 24, required: true, props: { maxLength: 128 } },
  {
    label: '商品分类',
    field: 'categoryId',
    type: 'tree-select',
    span: 12,
    required: true,
    props: {
      data: categoryTree,
      allowClear: true,
      allowSearch: true,
      fallbackOption: false,
      placeholder: '请选择二级分类（编码=分类码+序号）',
      filterTreeNode(searchKey: string, nodeData: any) {
        return String(nodeData.title || '').toLowerCase().includes(String(searchKey || '').toLowerCase())
      },
    },
  },
  { label: '规格型号', field: 'spec', type: 'input', span: 12, props: { maxLength: 128 } },
  { label: '计量单位', field: 'unit', type: 'input', span: 12, required: true, props: { maxLength: 16 } },
  {
    label: '默认税率',
    field: 'defaultTaxRate',
    type: 'select',
    span: 12,
    required: true,
    props: {
      options: computed(() => (psi_tax_rate.value || []).map((i: any) => ({
        label: i.label,
        value: String(i.value),
      }))),
    },
  },
  { label: '条码', field: 'barcode', type: 'input', span: 12, props: { maxLength: 64 } },
  { label: '采购参考价', field: 'purchasePrice', type: 'input-number', span: 12, props: { min: 0, precision: 2 } },
  { label: '销售参考价', field: 'salePrice', type: 'input-number', span: 12, props: { min: 0, precision: 2 } },
  { label: '库存下限', field: 'stockLower', type: 'input-number', span: 12, props: { min: 0, precision: 4, placeholder: '低于则预警' } },
  { label: '库存上限', field: 'stockUpper', type: 'input-number', span: 12, props: { min: 0, precision: 4, placeholder: '高于则预警' } },
  {
    label: '状态',
    field: 'status',
    type: 'switch',
    span: 24,
    props: { type: 'round', checkedValue: 1, uncheckedValue: 2, checkedText: '启用', uncheckedText: '禁用' },
  },
  { label: '备注', field: 'remark', type: 'textarea', span: 24 },
])

const loadCategories = async () => {
  const { data } = await listGoodsCategory({})
  categoryTree.value = mapTree(data || [], (i: any) => {
    const isLevel1 = Number(i.level) === 1 || i.parentId == null || String(i.parentId) === '0'
    return {
      key: String(i.id),
      title: `${i.levelName || (isLevel1 ? '一级' : '二级')} ${i.code ? `${i.code} ` : ''}${i.name}`,
      // 一级仅作分组展示，不可选
      disabled: isLevel1,
      children: i.children,
    }
  })
}

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    const payload = {
      ...form,
      categoryId: form.categoryId == null || form.categoryId === '' ? undefined : form.categoryId,
    }
    let savedId = dataId.value
    if (isUpdate.value) {
      await updateGoods(payload, dataId.value)
      Message.success('修改成功')
    } else {
      const { data } = await addGoods(payload)
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
  await loadCategories()
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  await loadCategories()
  const { data } = await getGoods(id)
  Object.assign(form, data, {
    categoryId: data.categoryId == null ? undefined : String(data.categoryId),
  })
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
