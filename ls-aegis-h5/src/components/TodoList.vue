<template>
  <div class="all-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="all-head"><h1>我的待办</h1><p>{{ description }}</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-tabs v-if="filters.length" v-model:active="activeStatus" class="list-filters" @change="refresh">
        <van-tab v-for="filter in filters" :key="filter.value" :name="filter.value" :title="filter.label" />
      </van-tabs>
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length && !failed ? '没有更多了' : ''" @load="load">
        <div v-if="failed" class="empty-panel"><van-empty image="error" description="待办加载失败，请重试"><van-button round type="primary" size="small" @click="retry">重新加载</van-button></van-empty></div>
        <div v-else-if="!list.length && finished" class="empty-panel"><van-empty image="search" :description="emptyText" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(detailPath(item))">
          <template #title><strong>{{ itemTitle(item) }}</strong></template>
          <template #label><div class="item-desc">{{ itemDescription(item) }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <AppTabbar />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTabbar from '@/components/AppTabbar.vue'
import { createTodoPager } from '@/utils/todoPager'

const props = defineProps({
  loadPage: { type: Function, required: true },
  filters: { type: Array, default: () => [] },
  description: { type: String, required: true },
  emptyText: { type: String, required: true },
  itemTitle: { type: Function, required: true },
  itemDescription: { type: Function, required: true },
  detailPath: { type: Function, required: true }
})
const router = useRouter()
const activeStatus = ref(props.filters[0]?.value)
const list = ref([])
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const failed = ref(false)
const pager = createTodoPager(params => props.loadPage(params))
let generation = 0
let activeLoad = -1
async function load() {
  const current = generation
  if (activeLoad === current) return
  activeLoad = current
  loading.value = true
  failed.value = false
  try {
    const result = await pager.load(activeStatus.value)
    if (current !== generation || !result) return
    list.value.push(...result.rows)
    finished.value = result.finished
  } catch {
    if (current === generation) { failed.value = true; finished.value = true }
  } finally {
    if (activeLoad === current) activeLoad = -1
    if (current === generation) { loading.value = false; refreshing.value = false }
  }
}
function refresh() {
  generation++
  pager.reset()
  list.value = []
  finished.value = false
  load()
}
function retry() { finished.value = false; load() }
function goWorkbench() { router.push(__H5_ROLE__ === 'engineer' ? '/engineer/workbench' : '/customer/workbench') }
</script>

<style scoped src="@/styles/service-list.css"></style>
