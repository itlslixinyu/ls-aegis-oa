<template>
  <div class="biz-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>库存预警</span>
        <a-tag size="small" :color="list.length ? 'red' : 'gray'">{{ list.length }}</a-tag>
      </div>
      <a-space>
        <a-link @click="emit('refresh')">刷新</a-link>
        <a-link @click="goGoods">商品档案</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body">
      <a-empty v-if="!list.length" description="暂无上下限预警" />
      <div v-else class="doc-list">
        <div v-for="item in list" :key="String(item.goodsId)" class="doc-item" @click="goGoods">
          <div class="doc-main">
            <a-tag v-if="item.alertType === 'LOW'" size="small" color="orangered">低于下限</a-tag>
            <a-tag v-else-if="item.alertType === 'HIGH'" size="small" color="arcoblue">高于上限</a-tag>
            <a-tag v-else size="small">{{ item.alertType }}</a-tag>
            <span class="doc-no" :title="item.goodsName">{{ item.goodsName || item.goodsCode || '-' }}</span>
          </div>
          <div class="doc-meta">
            <span>结存 {{ item.stockQty ?? '-' }}</span>
            <span>下限 {{ item.stockLower ?? '-' }} / 上限 {{ item.stockUpper ?? '-' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PsiStockAlert } from '@/apis/psi'

defineOptions({ name: 'BizStockAlert' })

defineProps<{
  list: PsiStockAlert[]
}>()

const emit = defineEmits<{
  refresh: []
}>()

const router = useRouter()
const goGoods = () => router.push('/psi/goods')
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
</style>
