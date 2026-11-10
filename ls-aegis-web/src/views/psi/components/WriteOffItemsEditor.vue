<template>
  <div class="wo-editor">
    <a-alert type="info" style="margin-bottom: 12px">
      {{ tip }}；可核销余额 ¥{{ budget.toFixed(2) }}，已分配 ¥{{ allocated.toFixed(2) }}
    </a-alert>
    <a-table :data="lines" :pagination="false" size="small" row-key="_key">
      <template #columns>
        <a-table-column :title="openLabel" :width="280">
          <template #cell="{ record }">
            <a-select
              v-model="record.openId"
              :options="availableOptions(record)"
              allow-search
              placeholder="请选择开口"
              style="width: 100%"
              @change="(v: string) => onOpenChange(record, v)"
            />
          </template>
        </a-table-column>
        <a-table-column title="核销金额" :width="140">
          <template #cell="{ record }">
            <a-input-number v-model="record.amount" :min="0.01" :precision="2" style="width: 100%" />
          </template>
        </a-table-column>
        <a-table-column title="操作" :width="72">
          <template #cell="{ rowIndex }">
            <a-link status="danger" :disabled="lines.length <= 1" @click="removeLine(rowIndex)">删</a-link>
          </template>
        </a-table-column>
      </template>
    </a-table>
    <div style="margin-top: 8px">
      <a-button size="mini" :disabled="!canAdd" @click="addLine">
        <template #icon><icon-plus /></template>加行
      </a-button>
    </div>
    <a-form :model="formModel" layout="vertical" style="margin-top: 12px">
      <a-form-item label="原因" field="reason">
        <a-input v-model="formModel.reason" allow-clear placeholder="可选" />
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import type { BizId } from '@/utils/biz-id'

export interface OpenOption {
  label: string
  value: BizId
  unwrittenAmount?: number
}

export interface WriteOffLine {
  _key: number
  openId?: BizId
  amount?: number
}

const props = defineProps<{
  openLabel: string
  tip: string
  budget: number
  options: OpenOption[]
  reason?: string
}>()

const emit = defineEmits<{
  (e: 'update:reason', v: string): void
}>()

let keySeq = 1
const lines = ref<WriteOffLine[]>([{ _key: keySeq++, openId: undefined, amount: undefined }])
const formModel = reactive({ reason: props.reason || '' })

watch(
  () => props.reason,
  (v) => {
    const next = v || ''
    if (formModel.reason !== next) {
      formModel.reason = next
    }
  },
)
watch(
  () => formModel.reason,
  (v) => emit('update:reason', v),
)

const allocated = computed(() =>
  lines.value.reduce((s, i) => s + (Number(i.amount) > 0 ? Number(i.amount) : 0), 0),
)

const usedOpenIds = computed(() => new Set(lines.value.map((i) => i.openId).filter(Boolean) as BizId[]))

const canAdd = computed(() => props.options.some((o) => !usedOpenIds.value.has(o.value)))

const availableOptions = (row: WriteOffLine) =>
  props.options.filter((o) => o.value === row.openId || !usedOpenIds.value.has(o.value))

const onOpenChange = (row: WriteOffLine, val: string) => {
  const hit = props.options.find((i) => i.value === val)
  if (!hit) return
  const remainBudget = props.budget - allocated.value + (Number(row.amount) || 0)
  const max = Math.min(remainBudget, Number(hit.unwrittenAmount) || 0)
  row.amount = max > 0 ? Number(max.toFixed(2)) : undefined
}

const addLine = () => {
  if (!canAdd.value) return
  lines.value.push({ _key: keySeq++, openId: undefined, amount: undefined })
}

const removeLine = (idx: number) => {
  if (lines.value.length <= 1) return
  lines.value.splice(idx, 1)
}

/** 校验并返回提交用明细；失败返回 null */
const validateAndGet = (): { openId: BizId, amount: number }[] | null => {
  const items: { openId: BizId, amount: number }[] = []
  const seen = new Set<string>()
  for (const line of lines.value) {
    if (!line.openId || !line.amount || Number(line.amount) <= 0) {
      Message.warning(`请完整填写${props.openLabel}与核销金额`)
      return null
    }
    if (seen.has(line.openId)) {
      Message.warning('同一开口不可重复核销')
      return null
    }
    seen.add(line.openId)
    const hit = props.options.find((i) => i.value === line.openId)
    const unwritten = Number(hit?.unwrittenAmount) || 0
    if (Number(line.amount) > unwritten + 0.001) {
      Message.warning(`开口核销金额超过未核销余额（¥${unwritten.toFixed(2)}）`)
      return null
    }
    items.push({ openId: line.openId, amount: Number(line.amount) })
  }
  const total = items.reduce((s, i) => s + i.amount, 0)
  if (total > props.budget + 0.001) {
    Message.warning(`核销合计 ¥${total.toFixed(2)} 超过可核销余额 ¥${props.budget.toFixed(2)}`)
    return null
  }
  return items
}

const reset = (presetAmount?: number) => {
  keySeq = 1
  lines.value = [{ _key: keySeq++, openId: undefined, amount: presetAmount }]
}

defineExpose({ validateAndGet, reset })
</script>
