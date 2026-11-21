<template>
          <div class="quote-form__panel-body quote-form__panel-body--preview">
            <div v-if="loading" class="quote-form__preview-loading">
              <a-spin :size="32" tip="预览加载中..." />
            </div>
            <iframe
              class="quote-form__preview-frame"
              :key="frame.html"
              :srcdoc="frame.html"
              title="报价单导出预览"
              @load="frame.loaded"
            />
          </div>
</template>
<script setup lang="ts">
import { onUnmounted, ref, shallowRef, watch } from 'vue'
const props = defineProps<{ html: string }>()
let generation = 0
const frame = shallowRef({ html: '', loaded: () => {} })
const loading = ref(true)
let debounce: ReturnType<typeof setTimeout> | undefined
let fallback: ReturnType<typeof setTimeout> | undefined
const onLoad = () => { clearTimeout(fallback); fallback = undefined; loading.value = false }
watch(() => props.html, (html) => {
  const version = ++generation
  clearTimeout(debounce)
  clearTimeout(fallback)
  debounce = setTimeout(() => {
    loading.value = true
    const loaded = () => { if (version === generation) onLoad() }
    frame.value = { html, loaded }
    fallback = setTimeout(loaded, 2500)
  }, 180)
}, { immediate: true })
onUnmounted(() => { generation++; clearTimeout(debounce); clearTimeout(fallback) })
</script>
<style scoped>
.quote-form__panel-body.quote-form__panel-body--preview {
  position: relative;
  padding: 12px;
  background: var(--color-fill-2);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: auto;
}
.quote-form__preview-loading {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.78);
}
.quote-form__preview-frame {
  width: 210mm;
  max-width: 100%;
  min-height: 297mm;
  border: none;
  background: transparent;
  flex-shrink: 0;
}
@media (max-width: 1200px) { .quote-form__panel-body--preview { min-height: 420px; } }
</style>
