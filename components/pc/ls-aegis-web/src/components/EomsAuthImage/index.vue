<template>
  <a-image v-bind="$attrs" :src="displaySrc" />
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { fetchEomsFileObjectUrl, needsAuthFetch, releaseEomsFileObjectUrl } from '@/utils/eomsFile'

const props = defineProps<{
  src?: string
}>()

const displaySrc = ref('')
let activeSrc = ''

watch(
  () => props.src,
  async (val) => {
    if (activeSrc) {
      releaseEomsFileObjectUrl(activeSrc)
      activeSrc = ''
    }
    if (!val) {
      displaySrc.value = ''
      return
    }
    if (!needsAuthFetch(val)) {
      displaySrc.value = val
      return
    }
    try {
      displaySrc.value = await fetchEomsFileObjectUrl(val)
      activeSrc = val
    } catch {
      displaySrc.value = ''
    }
  },
  { immediate: true },
)

onUnmounted(() => {
  if (activeSrc) {
    releaseEomsFileObjectUrl(activeSrc)
  }
})
</script>
