<template>
  <img :src="src && !failed ? src : brandLogo" alt="系统标志" @error="failed = true" />
</template>

<script setup>
import { ref, watch } from 'vue'
import brandLogo from '@/assets/images/brand-logo.png'

const props = defineProps({ src: { type: String, default: '' } })
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>
