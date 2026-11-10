<template>
  <div class="not-found">
    <div class="status">404</div>
    <h2>页面不存在</h2>
    <p>抱歉，您访问的页面不存在或已被移除</p>
    <van-button type="primary" round @click="goBack">返回上一页</van-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/')
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 24px;
}
.status { font-size: 72px; font-weight: 700; color: #1677ff; line-height: 1; margin-bottom: 16px; }
h2 { font-size: 20px; color: #1f2937; margin: 0 0 8px; }
p { font-size: 14px; color: #6b7280; margin: 0 0 32px; text-align: center; }
</style>
