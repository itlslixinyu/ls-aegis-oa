<template>
  <div class="search-trigger" @click="openSearch">
    <icon-search :size="18" style="margin-right: 4px;" />
    <span class="search-text">搜索</span>
    <span class="shortcut-key">Ctrl + K</span>
  </div>

  <a-modal
    v-model:visible="visible"
    class="layout-search-modal"
    title-align="start"
    :align-center="false"
    :closable="false"
    :mask-closable="true"
    :footer="true"
    :width="640"
    modal-class="layout-search-modal-inner"
    unmount-on-close
    @cancel="closeSearch"
  >
    <template #title>
      <div class="search-input-wrapper">
        <icon-search :size="20" class="search-icon" />
        <input
          ref="searchInput"
          v-model="searchKeyword"
          type="text"
          name="layoutPageSearch"
          id="layout-page-search"
          placeholder="搜索页面"
          class="search-input"
          autocomplete="off"
          @keydown="handleKeyDown"
        >
        <div class="esc-tip" @click="closeSearch">ESC</div>
      </div>
    </template>

    <div class="search-content">
      <div v-if="searchKeyword && !searchResults.length" class="empty-tip">
        未找到匹配页面
      </div>

      <div v-if="searchResults.length">
        <div class="result-count">
          搜索到 {{ searchResults.length }} 个结果
        </div>
        <div class="result-list">
          <div
            v-for="(item, index) in searchResults"
            :key="item.path"
            class="result-item"
            :class="{ selected: selectedIndex === index }"
            @click="handleResultClick(item)"
            @mouseenter="selectedIndex = index"
          >
            <icon-file :size="18" style="margin-right: 8px;" />
            <div class="result-title">{{ item.title }}</div>
            <div class="result-path">{{ item.path }}</div>
          </div>
        </div>
      </div>

      <div v-if="!searchKeyword && searchHistory.length" class="history-section">
        <div class="history-header">
          <div class="history-title">搜索历史</div>
          <a-link @click="clearHistory">清空历史</a-link>
        </div>
        <div class="history-list">
          <div
            v-for="item in searchHistory"
            :key="item.path"
            class="history-item"
            @click="handleHistoryClick(item)"
          >
            <icon-history :size="18" style="margin-right: 8px;" />
            <div class="result-title">{{ item.title }}</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="shortcut-bar">
        <div class="shortcut">
          <GiSvgIcon name="select" :size="12" class="shortcut-icon" />
          <span>切换</span>
        </div>
        <div class="shortcut">
          <GiSvgIcon name="shortcut-enter" :size="12" class="shortcut-icon" />
          <span>选择</span>
        </div>
      </div>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useEventListener, useStorage } from '@vueuse/core'
import { useRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useRouteStore } from '@/stores'
import { isRouteHidden } from '@/utils'
import { activeNavigation } from '@/utils/retired-navigation'

interface SearchItem {
  title: string
  path: string
}

const router = useRouter()
const routeStore = useRouteStore()

const visible = ref(false)
const searchInput = ref<HTMLInputElement | null>(null)
const searchKeyword = ref('')
const searchHistory = useStorage<SearchItem[]>('layout-search-history', [])
watch(searchHistory, (items) => {
  const active = activeNavigation(items)
  if (active.length !== items.length) searchHistory.value = active
}, { immediate: true, deep: true })
const searchResults = ref<SearchItem[]>([])
const selectedIndex = ref(-1)

const isHidden = isRouteHidden

const searchRoutes = (keyword: string) => {
  const result: SearchItem[] = []
  const key = keyword.trim().toLowerCase()
  if (!key) {
    return result
  }

  const loop = (routes: RouteRecordRaw[]) => {
    routes.forEach((route) => {
      const title = String(route.meta?.title || '')
      const path = route.path || ''
      const canOpen = !!path && !path.startsWith('http') && !isHidden(route.meta?.hidden)
      if (canOpen && title && title.toLowerCase().includes(key)) {
        // 有子级的目录节点通常不可直达，优先收录叶子或可跳转项
        if (!route.children?.length || route.component) {
          result.push({ title, path })
        }
      }
      if (route.children?.length) {
        loop(route.children)
      }
    })
  }

  loop(routeStore.routes)
  // 去重
  const map = new Map<string, SearchItem>()
  result.forEach((item) => map.set(item.path, item))
  return [...map.values()]
}

const handleSearch = (keyword: string) => {
  searchResults.value = searchRoutes(keyword)
  selectedIndex.value = searchResults.value.length ? 0 : -1
}

const openSearch = () => {
  visible.value = true
}

const closeSearch = () => {
  visible.value = false
}

const handleResultClick = (item: SearchItem) => {
  if (!searchHistory.value.some((history) => history.path === item.path)) {
    searchHistory.value = [item, ...searchHistory.value].slice(0, 8)
  }
  router.push(item.path)
  closeSearch()
}

const handleHistoryClick = (item: SearchItem) => {
  router.push(item.path)
  closeSearch()
}

const clearHistory = () => {
  searchHistory.value = []
}

useEventListener('keydown', (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    openSearch()
  }
})

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    e.preventDefault()
    closeSearch()
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (!searchResults.value.length) return
    selectedIndex.value = (selectedIndex.value + 1) % searchResults.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (!searchResults.value.length) return
    selectedIndex.value = selectedIndex.value <= 0
      ? searchResults.value.length - 1
      : selectedIndex.value - 1
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (selectedIndex.value >= 0 && selectedIndex.value < searchResults.value.length) {
      handleResultClick(searchResults.value[selectedIndex.value])
    }
  }
}

watch(visible, (open) => {
  if (open) {
    nextTick(() => {
      searchInput.value?.focus()
      searchInput.value?.select()
    })
  } else {
    searchResults.value = []
    searchKeyword.value = ''
    selectedIndex.value = -1
  }
})

watch(searchKeyword, (value) => {
  handleSearch(value)
})
</script>

<style scoped lang="scss">
.search-trigger {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 6px;
  cursor: pointer;
  background-color: var(--color-fill-2);
  border: 1px solid var(--color-border-2);
  transition: background-color 0.2s, border-color 0.2s;

  &:hover {
    background-color: var(--color-fill-3);
    border-color: var(--color-border-3);
  }
}

.search-text {
  line-height: 1;
  font-size: 13px;
  color: var(--color-text-3);
}

.shortcut-key {
  margin-left: 10px;
  padding: 2px 6px;
  font-size: 11px;
  line-height: 1.2;
  border-radius: 4px;
  background-color: var(--color-bg-1);
  border: 1px solid var(--color-border-2);
  color: var(--color-text-3);
}

.search-input-wrapper {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.search-icon {
  color: var(--color-text-3);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 36px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--color-text-1);

  &::placeholder {
    color: var(--color-text-3);
  }
}

.esc-tip {
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  background-color: var(--color-fill-2);
  color: var(--color-text-3);
  cursor: pointer;
  user-select: none;
}

.search-content {
  min-height: 120px;
  max-height: 50vh;
  overflow-y: auto;
}

.empty-tip {
  padding: 24px 0;
  text-align: center;
  color: var(--color-text-3);
  font-size: 13px;
}

.result-count {
  font-size: 12px;
  color: var(--color-text-3);
  margin-bottom: 8px;
}

.result-list,
.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-item,
.history-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--color-text-2);

  &:hover,
  &.selected {
    background-color: var(--color-fill-2);
    color: var(--color-text-1);
  }
}

.result-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-path {
  margin-left: 12px;
  max-width: 40%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--color-text-3);
}

.history-section {
  margin-top: 4px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-title {
  font-size: 12px;
  color: var(--color-text-3);
}

.shortcut-bar {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
}

.shortcut {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-3);
  font-size: 12px;
}

.shortcut-icon {
  padding: 4px;
  background-color: var(--color-fill-2);
  border-radius: 6px;
  color: var(--color-text-1);
}
</style>

<style lang="scss">
/* 挂到 body 的弹层：避开 header overflow:hidden 裁剪 */
.layout-search-modal-inner {
  top: 72px !important;
  padding: 0;

  .arco-modal-header {
    height: auto;
    padding: 12px 16px;
    border-bottom: 1px solid var(--color-border-2);
  }

  .arco-modal-body {
    padding: 12px 16px 8px;
  }

  .arco-modal-footer {
    padding: 10px 16px;
    border-top: 1px solid var(--color-border-2);
  }
}
</style>
