<template>
  <div class="all-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="all-head"><h1>我的报修</h1><p>查看你提交的全部报修记录</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" v-model:error="failed" error-text="加载失败，点击重试" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished && !failed" class="empty-panel"><van-empty image="search" description="暂无报修记录" /></div>
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
import { usePagedList } from '@/composables/usePagedList'
import { useRouter } from 'vue-router'
import { getRepairList } from '@/api/customer'
import { getApplyStatusText, formatDateTime } from '@/utils/dict'
const router = useRouter()
const {list,loading,finished,refreshing,failed,load,refresh}=usePagedList(params=>getRepairList(params))
function tagType(status) { return ({ WAIT: 'warning', CONFIRM: 'primary', ORDER: 'success', CANCEL: 'default' })[status] || 'default' }
function goWorkbench() { router.push('/customer/workbench') }
</script>
<style scoped src="@/styles/service-list.css"></style>
