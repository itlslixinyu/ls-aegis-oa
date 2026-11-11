<template>
  <div class="all-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="all-head"><h1>我的报修</h1><p>查看你提交的全部报修记录</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished" class="empty-panel"><van-empty image="search" description="暂无报修记录" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/customer/repair/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.repairNo }}</strong><van-tag :type="tagType(item.status)">{{ getApplyStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.faultType || '未填写故障类型' }} · {{ formatDateTime(item.applyTime) }}</div><div class="item-desc">{{ item.faultDesc || '暂无描述' }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <AppTabbar>
      <van-tabbar-item to="/customer/repairs/all" icon="orders-o">我的报修</van-tabbar-item>
    </AppTabbar>
  </div>
</template>
<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRepairList } from '@/api/customer'
import { getApplyStatusText, formatDateTime } from '@/utils/dict'
const router = useRouter(); const list = ref([]); const loading = ref(false); const finished = ref(false); const refreshing = ref(false); const page = ref(1)
function tagType(status) { return ({ WAIT: 'warning', CONFIRM: 'primary', ORDER: 'success', CANCEL: 'default' })[status] || 'default' }
async function load() { loading.value = true; try { const res = await getRepairList({ page: page.value, size: 10 }); const rows = res?.data?.list || []; list.value.push(...rows); if (rows.length < 10) finished.value = true; else page.value++ } finally { loading.value = false; finished.value = true } }
function refresh() { list.value = []; page.value = 1; finished.value = false; refreshing.value = false; load() }
function goWorkbench() { router.push('/customer/workbench') }
</script>
<style scoped src="@/styles/service-list.css"></style>
