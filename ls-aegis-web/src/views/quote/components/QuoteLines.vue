<template>
            <div class="quote-form__section">
              <div class="quote-form__section-head">
                <h3>{{ material ? '一、材料部分' : '二、技术服务费部分' }}</h3>
                <a-button v-if="!locked" :disabled="!ready" type="outline" size="mini" @click="emit('add')">{{ material ? '+ 添加材料行' : '+ 添加服务行' }}</a-button>
              </div>
              <a-table
                class="quote-form__line-table"
                :data="lines"
                :pagination="false"
                size="mini"
                :bordered="{ cell: true }"
                :scroll="{ x: tableScrollX }"
                row-key="uid"
              >
                <template #columns>
                  <a-table-column title="序号" :width="48" align="center">
                    <template #cell="{ rowIndex }">{{ rowIndex + 1 }}</template>
                  </a-table-column>
                  <a-table-column :title="material ? '材料名称/规格' : '服务项目'" :width="material ? 200 : 180" align="center">
                    <template #cell="{ record }">
                      <a-select
                        v-if="material"
                        v-model="record.goodsId"
                        size="mini"
                        :options="goodsOptions"
                        :loading="goodsLoading"
                        :disabled="locked || !ready"
                        allow-search
                        allow-clear
                        placeholder="选择库存商品"
                        @change="(v: string | undefined) => emit('goodsChange', record, v)"
                        @popup-visible-change="(v: boolean) => emit('goodsPopup', v, record)"
                      >
                        <template #footer>
                          <div class="quote-form__select-footer" @mousedown.prevent @click="emit('addGoods', record)">
                            + 新增商品
                          </div>
                        </template>
                      </a-select>
                      <a-input v-else v-model="record.itemName" size="mini" placeholder="服务项目"
                        :disabled="locked || !ready" @change="emit('recalc', record)" />
                    </template>
                  </a-table-column>
                  <a-table-column :title="material ? '品牌/型号' : '说明'" :width="TableCol.spec" align="center">
                    <template #cell="{ record }">
                      <a-input
                        v-model="record[material ? 'brandModel' : 'description']"
                        size="mini"
                        :placeholder="material ? '品牌/型号' : '说明'"
                        :disabled="locked || !ready"
                      />
                    </template>
                  </a-table-column>
                  <a-table-column title="单位" :width="TableCol.unit" align="center">
                    <template #cell="{ record }">
                      <a-select
                        v-model="record.unit"
                        size="mini"
                        :options="unitOptions"
                        :disabled="locked || !ready"
                      >
                        <template #arrow-icon />
                      </a-select>
                    </template>
                  </a-table-column>
                  <a-table-column :title="material ? '数量' : '工作量'" :width="TableCol.qty" align="center">
                    <template #cell="{ record }">
                      <a-input-number
                        v-model="record.quantity"
                        size="mini"
                        :min="0"
                        :precision="2"
                        hide-button
                        :disabled="locked || !ready"
                        @change="() => emit('recalc', record)"
                      />
                    </template>
                  </a-table-column>
                  <a-table-column title="单价(¥)" :width="TableCol.qty" align="center">
                    <template #cell="{ record }">
                      <a-input-number
                        v-model="record.price"
                        size="mini"
                        :min="0"
                        :precision="2"
                        hide-button
                        :disabled="locked || !ready"
                        @change="() => emit('recalc', record)"
                      />
                    </template>
                  </a-table-column>
                  <a-table-column title="金额(¥)" :width="TableCol.qty" align="center">
                    <template #cell="{ record }">{{ formatMoney(record.amount) }}</template>
                  </a-table-column>
                  <a-table-column v-if="!locked" title="" :width="40" align="center">
                    <template #cell="{ rowIndex }">
                      <a-link status="danger" @click="emit('remove', rowIndex)">×</a-link>
                    </template>
                  </a-table-column>
                </template>
              </a-table>
              <div class="quote-form__subtotal">{{ material ? '材料小计' : '服务小计' }}：{{ formatMoney(subtotal) }}</div>
            </div>

</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TableCol } from '@/constant/table-col'
import type { QuoteLine } from '../quote-line'

const props = defineProps<{
  kind: 'MATERIAL' | 'SERVICE'
  lines: QuoteLine[]
  subtotal: number
  locked: boolean
  ready: boolean
  unitOptions: Array<{ label: string, value: string }>
  goodsOptions?: Array<{ label: string, value: string, stockQty?: number }>
  goodsLoading?: boolean
}>()
const emit = defineEmits<{
  add: []
  remove: [index: number]
  recalc: [line: QuoteLine]
  goodsChange: [line: QuoteLine, id?: string]
  goodsPopup: [visible: boolean, line: QuoteLine]
  addGoods: [line: QuoteLine]
}>()
const material = computed(() => props.kind === 'MATERIAL')
const tableScrollX = computed(() => 48 + (material.value ? 200 : 180) + TableCol.spec + TableCol.unit + TableCol.qty * 3 + 40)
const formatMoney = (value?: number | null) => Number(value ?? 0).toFixed(2)
</script>

<style scoped>
.quote-form__section { padding: 12px 16px 16px; border-bottom: 1px solid var(--color-border-2); }
.quote-form__line-table :deep(.arco-table-th),
.quote-form__line-table :deep(.arco-table-td) {
  padding: 3px 4px;
  font-size: 11px;
  line-height: 1.4;
  text-align: center;
  vertical-align: middle;
}
.quote-form__line-table :deep(.arco-table-th) {
  white-space: normal;
  word-break: keep-all;
}
.quote-form__line-table :deep(.arco-table-cell) {
  justify-content: center;
}
.quote-form__line-table :deep(.arco-input-wrapper),
.quote-form__line-table :deep(.arco-input-number),
.quote-form__line-table :deep(.arco-select) {
  width: 100%;
}
.quote-form__line-table :deep(.arco-input),
.quote-form__line-table :deep(.arco-input-number .arco-input) {
  font-size: 11px;
  text-align: center;
}
.quote-form__line-table :deep(.arco-select-view-single) {
  font-size: 11px;
  justify-content: center;
  padding-inline: 4px;
}
.quote-form__line-table :deep(.arco-select-view-value) {
  font-size: 11px;
  flex: 1;
  text-align: center;
  justify-content: center;
}
.quote-form__line-table :deep(.arco-select-view-suffix) {
  display: none;
}
.quote-form__select-footer {
  padding: 6px 12px;
  color: rgb(var(--primary-6));
  cursor: pointer;
  font-size: 12px;
  border-top: 1px solid var(--color-border-2);
}
.quote-form__select-footer:hover {
  background: var(--color-fill-2);
}
.quote-form__subtotal {
  margin-top: 8px;
  text-align: right;
  font-size: 11px;
}
.quote-form__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.quote-form__section-head h3 {
  margin: 0 0 8px;
  font-size: 14px;
}
</style>
