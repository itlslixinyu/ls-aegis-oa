<template>
  <section class="welcome-card">
    <div class="eyebrow">{{ eyebrow }}</div>
    <div class="welcome-body">
      <div class="welcome-copy" :style="{ '--welcome-name-size': `${nameSize}px` }">
        <SingleLineHeading :text="name" @size-change="nameSize = $event" />
        <div class="greeting">您好</div>
        <p>{{ description }}</p>
      </div>
      <SystemLogo :src="logo" class="workbench-logo" />
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import SingleLineHeading from '@/components/SingleLineHeading.vue'
import SystemLogo from '@/components/SystemLogo.vue'
defineProps({
  eyebrow: { type: String, required: true },
  name: { type: String, required: true },
  description: { type: String, required: true },
  logo: { type: String, default: '' }
})
const nameSize = ref(23)
</script>

<style scoped>
.welcome-card { margin: 12px 16px 24px; padding: 18px 20px; border-radius: 24px; color: #fff; background: linear-gradient(135deg, #315ec4, #668ce8); box-shadow: 0 14px 28px rgba(49,94,196,.22); }
.eyebrow { margin-bottom: 14px; text-align: center; font-size: 12px; opacity: .78; letter-spacing: .1em; }
.welcome-body { display: grid; grid-template-columns: minmax(0, 1fr) clamp(60px, 19vw, 76px); gap: 14px; align-items: center; }
.welcome-copy { min-width: 0; }
.welcome-copy :deep(.single-line-heading) { font-weight: 700; }
.greeting { margin-top: 4px; font-size: var(--welcome-name-size, 23px); font-weight: 700; line-height: 1.4; }
p { margin: 8px 0 0; font-size: 12px; line-height: 1.5; opacity: .82; }
.workbench-logo { display: block; width: 100%; height: auto; max-height: 76px; aspect-ratio: 1; object-fit: contain; }
</style>
