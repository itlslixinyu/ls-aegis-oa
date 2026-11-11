<template>
  <van-tabbar route>
    <van-tabbar-item :to="`${prefix}/todos`" icon="todo-list-o">我的待办</van-tabbar-item>
    <van-tabbar-item :to="`${prefix}/workbench`" icon="apps-o">工作台</van-tabbar-item>
    <slot />
    <van-tabbar-item :to="`${prefix}/profile`" icon="manager-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup>
const prefix = __H5_ROLE__ === 'engineer' ? '/engineer' : '/customer'
</script>

<style scoped>
:deep(.van-tabbar-item__text) { font-size: 12px; white-space: nowrap; }
</style>
