<template>
  <div class="status-page">
    <van-nav-bar title="" left-arrow @click-left="goWorkbench" />
    <div class="status-head" :class="`status-head--${status}`"><h1>{{ statusLabel }}</h1><p>{{ statusDescription }}</p></div>
    <van-pull-refresh v-model="refreshing" @refresh="refresh" class="list-wrap">
      <van-list v-model:loading="loading" v-model:error="failed" error-text="加载失败，点击重试" :finished="finished" :finished-text="list.length ? '没有更多了' : ''" @load="load">
        <div v-if="!list.length && finished && !failed" class="empty-panel"><van-empty image="search" :description="`暂无${statusLabel}记录`" /></div>
        <van-cell v-for="item in list" :key="item.id" is-link class="item-card" @click="router.push(`/customer/repair/${item.id}`)">
          <template #title><div class="item-title"><strong>{{ item.repairNo }}</strong><van-tag type="primary">{{ getApplyStatusText(item.status) }}</van-tag></div></template>
          <template #label><div class="item-meta">{{ item.faultType || '未填写故障类型' }} · {{ formatDateTime(item.applyTime) }}</div><div class="item-desc">{{ item.faultDesc || '暂无描述' }}</div></template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
    <AppTabbar>
      <van-tabbar-item :to="`/customer/repairs/status/${status}`" icon="orders-o">{{ statusNavLabel }}</van-tabbar-item>
    </AppTabbar>
  </div>
</template>
<script setup>
import AppTabbar from '@/components/AppTabbar.vue'
import { computed, watch } from 'vue'
import { usePagedList } from '@/composables/usePagedList'
import { useRoute, useRouter } from 'vue-router'
import { getRepairList } from '@/api/customer'
import { getApplyStatusText, formatDateTime } from '@/utils/dict'
const route = useRoute(); const router = useRouter()
const labels = { WAIT: '等待接单', CONFIRM: '已确认报修', ORDER: '已转工单', CANCEL: '已取消报修' }
const navLabels = { WAIT: '等待接单', CONFIRM: '已确认', ORDER: '已转工单', CANCEL: '已取消' }
const descriptions = { WAIT: '报修已提交，等待接单；满十分钟仍未接单会开放工程师领取', CONFIRM: '平台已经确认的报修申请', ORDER: '已经转入维修工单的报修', CANCEL: '已经取消或关闭的报修申请' }
const status = computed(() => String(route.params.status || 'WAIT')); const statusLabel = computed(() => labels[status.value] || '报修记录')
const statusNavLabel = computed(() => navLabels[status.value] || '当前状态')
const statusDescription = computed(() => descriptions[status.value] || '当前状态下的报修记录')
const {list,loading,finished,refreshing,failed,load,refresh}=usePagedList(params=>getRepairList({...params,status:status.value}))
watch(status,refresh)
function goWorkbench() { router.push('/customer/workbench') }
</script>
<style scoped src="@/styles/service-list.css"></style>
