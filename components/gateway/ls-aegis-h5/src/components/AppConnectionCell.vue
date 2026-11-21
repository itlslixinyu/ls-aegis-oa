<template>
  <van-cell v-if="available" title="连接线路" :value="mode" icon="exchange" center is-link @click="reconnect" />
</template>
<script setup>
import { ref } from 'vue'
import { showToast } from 'vant'
import { hasNativeConnection, connectionStatus, reconnectConnection } from '@/utils/nativeConnection'
const available=hasNativeConnection()
const mode=ref(available ? connectionStatus() : '')
function reconnect() {
  const result=reconnectConnection(true)
  if(result==='busy')showToast('请等待当前请求完成后再检测')
  else if(result!=='accepted')showToast('暂时无法检测连接，请重新打开App')
}
</script>
