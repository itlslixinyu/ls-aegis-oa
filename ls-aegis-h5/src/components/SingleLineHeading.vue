<template>
  <h1 ref="container" class="single-line-heading" :style="{ fontSize: `${fontSize}px` }" tabindex="0"><span ref="label">{{ text }}</span></h1>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { fitHeadingSize } from '@/utils/fitHeading'
const props = defineProps({ text: { type: String, required: true } })
const container = ref(null)
const label = ref(null)
const fontSize = ref(23)
let observer
let frame
let disposed = false
function scheduleFit() {
  if (disposed) return
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(() => {
    if (!container.value || !label.value) return
    fontSize.value = fitHeadingSize(label.value.getBoundingClientRect().width, fontSize.value, container.value.clientWidth)
  })
}
watch(() => props.text, () => nextTick(scheduleFit))
onMounted(() => {
  observer = new ResizeObserver(scheduleFit)
  observer.observe(container.value)
  scheduleFit()
  document.fonts?.ready.then(scheduleFit)
})
onBeforeUnmount(() => { disposed = true; observer?.disconnect(); cancelAnimationFrame(frame) })
</script>

<style scoped>
.single-line-heading { min-width: 0; max-width: 100%; margin: 0; line-height: 1.4; white-space: nowrap; overflow-x: auto; overflow-y: hidden; }
.single-line-heading span { display: inline-block; white-space: nowrap; }
</style>
