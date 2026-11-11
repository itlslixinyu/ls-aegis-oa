<template>
  <div class="all-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="all-head"><h1>全部任务</h1><p>查看分配给你的全部维修任务</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="list-wrap">
      <van-tabs v-model:active="activeTab" class="list-filters" @change="onTabChange">
        <van-tab title="全部" name="" />
        <van-tab title="待接单" name="WAIT_ACCEPT" />
        <van-tab title="已接单" name="ACCEPTED" />
        <van-tab title="维修中" name="PROCESSING" />
        <van-tab title="待结算" name="WAIT_SETTLEMENT" />
        <van-tab title="已完成" name="FINISHED" />
      </van-tabs>
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="onLoad">
        <div v-if="!list.length && finished" class="empty-panel"><van-empty image="search" description="暂无维修任务" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="goDetail(item.id)">
          <template #title><div class="item-title"><strong>{{ item.orderNo }}</strong><van-tag :type="getTagType(item.status)">{{ getOrderStatusText(item.status) }}</van-tag></div></template>
          <template #label>
            <div class="item-meta">维修地址：{{ item.address || '-' }}</div>
            <div class="item-meta">接单时间：{{ formatDateTime(item.acceptTime) }}</div>
            <div v-if="item.startTime" class="item-meta">开始时间：{{ formatDateTime(item.startTime) }}</div>
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <AppTabbar>
      <van-tabbar-item to="/engineer/tasks" icon="orders-o">全部任务</van-tabbar-item>
    </AppTabbar>
  </div>
</template>

<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTaskList } from '@/api/engineer'
import { getOrderStatusText, formatDateTime, orderStatusMap } from '@/utils/dict'

const router = useRouter()
const route = useRoute()

const activeTab = ref(typeof route.query.status === 'string' ? route.query.status : '')
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 10

function getTagType(status) {
  return orderStatusMap[status]?.tagType || 'default'
}

async function onLoad() {
  loading.value = true
  try {
    const res = await getTaskList({
      page: pageNum.value,
      size: pageSize,
      status: activeTab.value || undefined
    })
    const rows = res?.data?.list || []
    if (rows.length) {
      list.value.push(...rows)
    }
    if (rows.length < pageSize) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
  refreshing.value = false
}

function onTabChange() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function goDetail(id) {
  router.push(`/engineer/task/${id}`)
}

function goWorkbench() { router.push('/engineer/workbench') }
</script>

<style scoped src="@/styles/service-list.css"></style>
