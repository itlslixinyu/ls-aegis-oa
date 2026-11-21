<template>
  <div
    class="screen-panel"
    :class="{ 'is-compact': compact, 'is-enter': enter, 'has-connectors': connectors }"
    :style="enter ? { '--enter-delay': enterDelay } : undefined"
  >
    <div class="screen-panel__corners" aria-hidden="true">
      <span class="c c-tl" />
      <span class="c c-tr" />
      <span class="c c-bl" />
      <span class="c c-br" />
    </div>
    <div v-if="connectors" class="screen-panel__connectors" aria-hidden="true">
      <span class="conn conn-l" />
      <span class="conn conn-r" />
    </div>
    <div v-if="title" class="screen-panel__head">
      <span class="screen-panel__title-dot" aria-hidden="true" />
      <span class="screen-panel__title">{{ title }}</span>
      <div class="screen-panel__title-decor" aria-hidden="true" />
      <div v-if="$slots.extra" class="screen-panel__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="screen-panel__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    compact?: boolean
    /** 是否启用入场动画 */
    enter?: boolean
    /** 入场延迟，如 0.4s */
    enterDelay?: string
    /** 是否显示底部连接点 */
    connectors?: boolean
  }>(),
  {
    enter: true,
    enterDelay: '0s',
    connectors: true,
  },
)
</script>

<style scoped lang="scss">
.screen-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  /* 上下左右内边距对称；3×4 时略收以腾出绘图区 */
  padding: 6px 10px;
  background: linear-gradient(180deg, rgba(11, 37, 69, 0.88) 0%, rgba(8, 24, 52, 0.88) 100%);
  border: 1px solid rgba(64, 159, 255, 0.2);
  border-radius: 4px;
  overflow: hidden;
  opacity: 0;
  transform: translateY(10px);
  transition:
    border-color 0.25s ease,
    box-shadow 0.25s ease,
    transform 0.25s ease;

  &.is-enter {
    animation: panel-fade-in 0.55s ease-out forwards;
    animation-delay: var(--enter-delay, 0s);
  }

  /* 面板间横向连接线：仅开启 connectors 时显示，避免侵占 gap 造成间距不均 */
  &.has-connectors:not(:last-child)::after {
    content: '';
    position: absolute;
    right: -8px;
    top: 40%;
    width: 6px;
    height: 2px;
    background: rgba(64, 159, 255, 0.5);
    box-shadow: 0 0 6px rgba(64, 159, 255, 0.7);
    animation: panel-link-pulse 2.4s ease-in-out infinite;
    pointer-events: none;
    z-index: 2;
  }

  &:hover {
    border-color: rgba(64, 159, 255, 0.48);
    box-shadow: 0 6px 20px rgba(64, 159, 255, 0.14);

    .c-tl {
      transform: translate(-1px, -1px);
      width: 16px;
      height: 24px;
      opacity: 1;
    }

    .c-tr {
      transform: translate(1px, -1px);
      width: 16px;
      height: 24px;
      opacity: 1;
    }

    .c-bl {
      transform: translate(-1px, 1px);
      width: 16px;
      height: 24px;
      opacity: 1;
    }

    .c-br {
      transform: translate(1px, 1px);
      width: 16px;
      height: 24px;
      opacity: 1;
    }
  }

  &.is-compact {
    padding: 10px 12px;
  }

  &__corners {
    pointer-events: none;

    .c {
      position: absolute;
      width: 12px;
      height: 18px;
      border: 2px solid #409fff;
      opacity: 0.85;
      transition: all 0.25s ease;
    }

    .c-tl {
      top: -1px;
      left: -1px;
      border-right: none;
      border-bottom: none;
    }

    .c-tr {
      top: -1px;
      right: -1px;
      border-left: none;
      border-bottom: none;
    }

    .c-bl {
      bottom: -1px;
      left: -1px;
      border-right: none;
      border-top: none;
    }

    .c-br {
      bottom: -1px;
      right: -1px;
      border-left: none;
      border-top: none;
    }
  }

  &__connectors {
    pointer-events: none;

    .conn {
      position: absolute;
      bottom: 2px;
      width: 6px;
      height: 6px;
      background: #409fff;
      border-radius: 50%;
      box-shadow: 0 0 6px rgba(64, 159, 255, 0.8);
      animation: panel-conn-blink 2.4s ease-in-out infinite;
    }

    .conn-l {
      left: 15%;
    }

    .conn-r {
      right: 15%;
    }
  }

  &__head {
    display: flex;
    align-items: center;
    height: 24px;
    margin-bottom: 4px;
    flex-shrink: 0;
  }

  &__title-dot {
    display: inline-block;
    width: 4px;
    height: 16px;
    margin-right: 10px;
    border-radius: 1px;
    flex-shrink: 0;
    background: linear-gradient(180deg, #409fff, #00d4ff);
    box-shadow: 0 0 8px rgba(64, 159, 255, 0.6);
    animation: panel-dot-glow 2.2s ease-in-out infinite;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 2px;
    color: #e6f2ff;
    white-space: nowrap;
  }

  &__title-decor {
    flex: 1;
    height: 1px;
    margin-left: 12px;
    background: linear-gradient(90deg, rgba(64, 159, 255, 0.4), transparent 70%);
    position: relative;
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: -40%;
      width: 40%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.6), transparent);
      animation: panel-line-sweep 3.8s ease-in-out infinite;
    }
  }

  &__extra {
    margin-left: 10px;
    color: #3ad1a8;
    font-size: 12px;
    font-weight: 400;
    letter-spacing: 0.5px;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
}

@keyframes panel-fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes panel-dot-glow {
  0%,
  100% {
    box-shadow: 0 0 6px rgba(64, 159, 255, 0.5);
    opacity: 0.85;
  }
  50% {
    box-shadow: 0 0 14px rgba(0, 212, 255, 0.9);
    opacity: 1;
  }
}

@keyframes panel-line-sweep {
  0% {
    left: -40%;
  }
  60% {
    left: 100%;
  }
  100% {
    left: 100%;
  }
}

@keyframes panel-link-pulse {
  0%,
  100% {
    opacity: 0.4;
  }
  50% {
    opacity: 1;
  }
}

@keyframes panel-conn-blink {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@media (max-width: 1200px) {
  .screen-panel.has-connectors:not(:last-child)::after {
    display: none;
  }

  .screen-panel__connectors {
    display: none;
  }
}
</style>
