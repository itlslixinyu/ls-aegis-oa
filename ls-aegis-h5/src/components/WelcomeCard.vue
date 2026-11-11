<template>
  <section class="welcome-card">
    <div class="eyebrow">{{ eyebrow }}</div>
    <div class="welcome-body">
      <div class="welcome-copy">
        <SingleLineHeading :text="name" />
        <div class="greeting">您好</div>
        <p>{{ description }}</p>
      </div>
      <SystemLogo :src="logo" class="workbench-logo" />
    </div>
  </section>
</template>

<script setup>
import SingleLineHeading from '@/components/SingleLineHeading.vue'
import SystemLogo from '@/components/SystemLogo.vue'
defineProps({
  eyebrow: { type: String, required: true },
  name: { type: String, required: true },
  description: { type: String, required: true },
  logo: { type: String, default: '' }
})
</script>

<style scoped>
.welcome-card { margin: 12px 16px 24px; padding: 22px; border-radius: 24px; color: #fff; background: linear-gradient(135deg, #315ec4, #668ce8); box-shadow: 0 14px 28px rgba(49,94,196,.22); }
.eyebrow { margin-bottom: 18px; text-align: center; font-size: 12px; opacity: .78; letter-spacing: .1em; }
.welcome-body { display: grid; grid-template-columns: minmax(0, 1fr) 88px; gap: 18px; align-items: center; }
.welcome-copy { min-width: 0; }
.greeting { margin-top: 6px; font-size: 22px; font-weight: 700; }
p { margin: 10px 0 0; font-size: 13px; opacity: .82; }
.workbench-logo { width: 88px; height: 88px; object-fit: contain; }
</style>
