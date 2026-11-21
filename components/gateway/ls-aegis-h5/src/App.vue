<template>
  <van-config-provider>
    <router-view />
    <!-- Vant 4 函数式组件容器：Toast / Dialog / Notify 必须挂载根容器才能显示 -->
    <van-toast />
    <van-dialog />
    <van-notify />
  </van-config-provider>
</template>

<script setup>
import { onMounted } from 'vue'
import { loadBizDicts } from '@/utils/dict'

onMounted(() => {
  loadBizDicts()
})
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #323233;
  background-color: #F0F5FC;
  min-height: 100vh;
}
</style>
