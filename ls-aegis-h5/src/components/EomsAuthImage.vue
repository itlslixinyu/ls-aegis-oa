<template>
  <van-image v-bind="$attrs" :src="displaySrc" />
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { fetchEomsFileObjectUrl, needsAuthFetch, releaseEomsFileObjectUrl } from '@/utils/eomsFile'

const props = defineProps({
  src: {
    type: String,
    default: ''
  }
})

const displaySrc = ref('')
let activeSrc = ''
let requestVersion = 0

watch(
  () => props.src,
  async (val, _previous, onCleanup) => {
    const request = ++requestVersion
    onCleanup(() => {
      if (request === requestVersion) requestVersion++
    })
    if (activeSrc) {
      releaseEomsFileObjectUrl(activeSrc)
      activeSrc = ''
    }
    displaySrc.value = ''
    if (!val) return
    if (!needsAuthFetch(val)) {
      displaySrc.value = val
      return
    }
    try {
      const url = await fetchEomsFileObjectUrl(val)
      if (request !== requestVersion) {
        releaseEomsFileObjectUrl(val)
        return
      }
      displaySrc.value = url
      activeSrc = val
    } catch {
      if (request === requestVersion) displaySrc.value = ''
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  requestVersion++
  if (activeSrc) {
    releaseEomsFileObjectUrl(activeSrc)
  }
})
</script>
