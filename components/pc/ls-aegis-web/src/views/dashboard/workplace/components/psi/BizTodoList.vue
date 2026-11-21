<template>
  <div class="biz-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>业务待办</span>
        <a-tag size="small" color="orangered">{{ list.length }}</a-tag>
      </div>
      <a-space>
        <a-link @click="emit('refresh')">刷新</a-link>
        <a-link @click="goAll">全部</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body">
      <a-empty v-if="!list.length" description="暂无未完成待办" />
      <div v-else class="doc-list">
        <div v-for="item in list" :key="item.id" class="doc-item" @click="goTodo(item)">
          <div class="doc-main">
            <a-tag size="small" color="arcoblue">{{ todoTypeLabel(item.todoType) }}</a-tag>
            <span class="doc-no" :title="item.title">{{ item.title }}</span>
          </div>
          <div class="doc-meta">
            <span class="doc-unit">{{ item.bizNo || '-' }}</span>
            <a-link @click.stop="goTodo(item)">处理</a-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PsiTodoResp } from '@/apis/psi'
import { resolvePsiTodoRoute, todoTypeLabel } from '@/utils/psi-todo-nav'

defineOptions({ name: 'BizTodoList' })

defineProps<{
  list: PsiTodoResp[]
}>()

const emit = defineEmits<{
  refresh: []
}>()

const router = useRouter()
const goAll = () => router.push('/psi/todo')
const goTodo = (record: PsiTodoResp) => router.push(resolvePsiTodoRoute(record))
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
</style>
