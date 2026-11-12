<template>
  <section class="photo-section" :class="{ 'is-empty': !files.length }" :aria-label="title">
    <header class="photo-section__header">
      <h2><van-icon name="photo-o" aria-hidden="true" />{{ title }}<span v-if="required" class="required">*</span></h2>
      <span class="photo-section__count" aria-live="polite">{{ files.length }} / {{ maxCount }}</span>
    </header>
    <p v-if="description" class="photo-section__description">{{ description }}</p>
    <div class="photo-section__content"><slot /></div>
    <p v-if="showHint" class="photo-section__hint">支持 JPG、PNG，单张不超过 10MB</p>
    <slot name="status" />
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '现场照片' },
  description: { type: String, default: '' },
  showHint: { type: Boolean, default: true },
  files: { type: Array, required: true },
  maxCount: { type: Number, required: true },
  required: { type: Boolean, default: false },
})
</script>

<style scoped>
.photo-section { padding: 16px; border: 1px solid #e7ecf3; border-radius: 16px; background: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }
.photo-section__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2 { display: flex; align-items: center; gap: 8px; margin: 0; color: #17243a; font-size: 16px; font-weight: 600; line-height: 24px; }
h2 .van-icon { color: #4775d5; font-size: 20px; }
.required { color: #e15b65; font-size: 14px; }
.photo-section__count { flex: none; padding: 2px 9px; border-radius: 8px; background: #f0f4fa; color: #64748b; font-size: 12px; font-variant-numeric: tabular-nums; }
.photo-section__description { margin: 7px 0 14px; color: #64748b; font-size: 12px; line-height: 1.7; }
.photo-section__content { margin-top: 14px; min-width: 0; }
.photo-section__description + .photo-section__content { margin-top: 0; }
.photo-section__hint { margin: 10px 0 0; color: #6b7789; font-size: 11px; line-height: 18px; }
:deep(.van-uploader) { display: block; width: 100%; }
:deep(.van-uploader__wrapper) { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
:deep(.van-uploader__preview), :deep(.van-uploader__upload), :deep(.van-uploader__input-wrapper) { width: 100%; height: auto; aspect-ratio: 1; margin: 0; min-width: 0; border-radius: 10px; }
:deep(.van-uploader__preview-image) { width: 100%; height: 100%; overflow: hidden; border-radius: 10px; }
:deep(.van-uploader__upload) { box-sizing: border-box; border: 1px dashed #c3d2ed; background: #f6f8fd; }
:deep(.van-uploader__upload-icon) { color: #6487ca; font-size: 26px; }
:deep(.van-uploader__upload-text) { margin-top: 7px; color: #5c739b; font-size: 12px; }
.is-empty :deep(.van-uploader__upload) { grid-column: 1 / -1; height: 100px; aspect-ratio: auto; flex-direction: row; gap: 10px; }
.is-empty :deep(.van-uploader__upload-text) { margin: 0; font-size: 14px; }
:deep(.van-uploader__mask) { border-radius: 10px; }
@media (prefers-reduced-motion: no-preference) { :deep(.van-uploader__upload:active) { background: #eaf0fc; } }
</style>
