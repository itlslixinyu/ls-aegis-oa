<template>
  <a-space class="gi-cell-avatar" :size="8" align="center">
    <Avatar :src="props.avatar" :name="props.name" :size="24" />
    <a-link v-if="props.isLink" @click="emit('click')">
      <a-typography-paragraph
        class="gi-cell-avatar__name link-text"
        :ellipsis="{
          rows: 1,
          showTooltip: true,
          css: true,
        }"
      >
        {{ props.name }}
      </a-typography-paragraph>
    </a-link>
    <a-typography-paragraph
      v-else
      class="gi-cell-avatar__name"
      :ellipsis="{
        rows: 1,
        showTooltip: true,
        css: true,
      }"
    >
      {{ props.name }}
    </a-typography-paragraph>
  </a-space>
</template>

<script setup lang="ts">
defineOptions({ name: 'GiCellAvatar' })

const props = withDefaults(defineProps<Props>(), {
  avatar: '',
  name: '',
  isLink: false, // 是否可以点击
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

interface Props {
  avatar?: string
  name: string
  isLink?: boolean
}
</script>

<style scoped lang="scss">
.gi-cell-avatar {
  max-width: 100%;
}

.gi-cell-avatar__name {
  margin-bottom: 0 !important;
  line-height: 24px;
}
</style>
