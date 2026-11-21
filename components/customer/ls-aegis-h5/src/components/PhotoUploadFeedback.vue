<template>
  <div v-if="failed.length" class="photo-upload-feedback" role="status">
    <div v-for="entry in failed" :key="entry.index" class="photo-upload-feedback__item">
      <span>第 {{ entry.index + 1 }} 张：{{ entry.file.message || '上传失败' }}</span>
      <van-button size="mini" plain type="primary" @click="emit('retry', entry.file)">重试</van-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ files: { type: Array, required: true } })
const emit = defineEmits(['retry'])
const failed = computed(() => props.files.map((file, index) => ({ file, index })).filter(entry => entry.file.status === 'failed'))
</script>

<style scoped>
.photo-upload-feedback { margin: 10px 0 0; color: #be4c57; font-size: 12px; }
.photo-upload-feedback__item { display: flex; align-items: center; gap: 10px; margin-top: 6px; padding: 8px 10px; border: 1px solid #f6dce0; border-radius: 9px; background: #fff6f7; }
.photo-upload-feedback__item span { flex: 1; min-width: 0; overflow-wrap: anywhere; }
</style>
