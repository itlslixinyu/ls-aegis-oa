<template>
  <div />
</template>

<script setup lang="ts">
defineOptions({ name: 'SignAuditLogRedirect' })

const router = useRouter()
router.replace({ path: '/monitor/log', query: { tabKey: '3' } })
</script>
