<template>
  <van-cell v-if="inMiniProgram" :title="title" icon="bell" is-link :clickable="!busy" @click="subscribe" />
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { showFailToast } from 'vant'
import request from '@/utils/request'

const props = defineProps({
  title: { type: String, required: true },
  endpoint: { type: String, required: true },
})

const inMiniProgram = ref(false)
const busy = ref(false)
let sdkPromise

function detectMiniProgram() {
  let supportsSubscription = new URLSearchParams(window.location.search).get('wechatSubscription') === '1'
  try {
    supportsSubscription ||= sessionStorage.getItem('wechat-subscription-supported') === '1'
  } catch {
    // The entry URL can still indicate support when session storage is unavailable.
  }
  inMiniProgram.value = supportsSubscription && window.__wxjs_environment === 'miniprogram'
}

onMounted(() => {
  detectMiniProgram()
  document.addEventListener('WeixinJSBridgeReady', detectMiniProgram)
})
onUnmounted(() => document.removeEventListener('WeixinJSBridgeReady', detectMiniProgram))

function loadMiniProgramSdk() {
  if (window.wx?.miniProgram) return Promise.resolve(window.wx.miniProgram)
  if (!sdkPromise) {
    sdkPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://res.wx.qq.com/open/js/jweixin-1.6.0.js'
      script.onload = () => window.wx?.miniProgram ? resolve(window.wx.miniProgram) : reject(new Error())
      script.onerror = () => reject(new Error())
      document.head.appendChild(script)
    }).catch(error => {
      sdkPromise = undefined
      throw error
    })
  }
  return sdkPromise
}

async function subscribe() {
  if (busy.value) return
  busy.value = true
  try {
    const bridge = await loadMiniProgramSdk()
    const { data } = await request.post(props.endpoint, null, { skipErrorToast: true })
    const ticket = encodeURIComponent(data.ticket)
    const templateId = encodeURIComponent(data.templateId)
    await new Promise((resolve, reject) => {
      bridge.navigateTo({
        url: '/pages/subscription/index?ticket=' + ticket + '&templateId=' + templateId,
        success: resolve,
        fail: reject,
      })
    })
  } catch (error) {
    showFailToast(error?.message || '暂时无法打开微信订阅，请稍后重试')
  } finally {
    busy.value = false
  }
}
</script>
