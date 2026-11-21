<template>
  <GiSvgIcon :name="displayName" :size="18" />
</template>

<script setup lang="ts">
interface Props {
  icon?: string
  svgIcon?: string
}

const props = withDefaults(defineProps<Props>(), {})

const displayName = computed(() => props.icon || props.svgIcon || 'menu')
</script>

<style scoped lang="scss"></style>
