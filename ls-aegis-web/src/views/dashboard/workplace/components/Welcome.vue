<template>
  <a-card class="card" :class="{ 'card--dense': dense }" :bordered="false">
    <div class="content">
      <div class="module module--welcome">
        <a-space :size="dense ? 'small' : 'medium'">
          <Avatar :src="userStore.avatar" :name="userStore.nickname" :size="dense ? 44 : 68" />
          <div class="welcome">
            <p class="hello">{{ goodTimeText() }}！{{ userStore.nickname }}</p>
            <p v-if="!dense">北海虽赊，扶摇可接；东隅已逝，桑榆非晚。</p>
          </div>
        </a-space>
      </div>

      <span v-if="weatherEnabled" class="panel-divider" aria-hidden="true" />

      <div v-if="weatherEnabled" class="module module--weather">
        <WeatherBrief
          :weather="weather"
          :loading="weatherLoading"
          :title="weatherTitle"
        />
      </div>

      <span class="panel-divider" aria-hidden="true" />

      <div class="module module--notice" :class="{ 'module--notice-wide': !weatherEnabled }">
        <div class="notice-panel">
          <div class="notice-header">
            <span class="notice-title">公&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;告</span>
            <a-link class="notice-more" @click="openMessage">更多</a-link>
          </div>
          <a-skeleton v-if="loading" :loading="loading" :animation="true">
            <a-skeleton-line :rows="2" />
          </a-skeleton>
          <div v-else-if="dataList.length === 0" class="notice-empty">暂无公告</div>
          <div v-else class="notice-viewport" @mouseenter="paused = true" @mouseleave="paused = false">
            <div
              class="notice-track"
              :class="{ paused, scroll: needScroll }"
              :style="needScroll
                ? {
                    animationDuration: `${Math.max(dataList.length * 3, 6)}s`,
                    '--notice-scroll-y': `-${dataList.length * 28}px`,
                  }
                : undefined"
            >
              <div
                v-for="(item, idx) in displayList"
                :key="`${item.id}-${idx}`"
                class="notice-item"
                @click="onDetail(item.id)"
              >
                <div class="notice-main">
                  <span class="notice-dot" :class="`level-${item.level}`" />
                  <span class="notice-label" :class="`level-${item.level}`">{{ item.label }}：</span>
                  <span class="notice-text">{{ item.title }}</span>
                  <span v-if="item.isTop" class="notice-tag">置顶</span>
                </div>
                <span
                  v-if="item.timeText"
                  class="notice-time"
                  :title="item.publishTime"
                >{{ item.timeText }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { listDashboardNotice } from '@/apis'
import { useUserStore } from '@/stores'
import { goodTimeText } from '@/utils'
import { useDashboardWeather } from '../../composables/useDashboardWeather'
import {
  mapDashboardNotice,
  pickDashboardNotices,
  type NoticeTickerItem,
} from '../../utils/noticeTicker'
import WeatherBrief from './WeatherBrief.vue'

defineOptions({ name: 'WorkplaceWelcome' })

withDefaults(
  defineProps<{
    /** 工作总览一屏：压缩欢迎条高度 */
    dense?: boolean
  }>(),
  { dense: false },
)

const userStore = useUserStore()
const router = useRouter()

const {
  weatherEnabled,
  weather,
  weatherTitle,
  loading: weatherLoading,
} = useDashboardWeather()

const dataList = ref<NoticeTickerItem[]>([])
const loading = ref(false)
const paused = ref(false)

const needScroll = computed(() => dataList.value.length > 1)

/** 列表复制一份用于无缝滚动 */
const displayList = computed(() => {
  if (!needScroll.value) {
    return dataList.value
  }
  return [...dataList.value, ...dataList.value]
})

const loadNotice = async () => {
  loading.value = true
  try {
    const { data } = await listDashboardNotice()
    dataList.value = pickDashboardNotices(data, 20).map(mapDashboardNotice)
  } finally {
    loading.value = false
  }
}

const onDetail = (id: string | number) => {
  router.push({ path: '/user/notice', query: { id: String(id) } })
}

const openMessage = () => {
  router.push({ path: '/user/message', query: { tab: 'notice' } })
}

onMounted(() => {
  loadNotice()
})
</script>

<style scoped lang="scss">
:deep(.arco-statistic-title) {
  margin-bottom: 0;
}

.card {
  .content {
    display: flex;
    align-items: center;
    gap: 0;
    padding: 8px 20px;
    min-width: 0;

    .welcome {
      margin: 8px 0;
      color: $color-text-3;
      .hello {
        font-size: 1.25rem;
        color: $color-text-1;
        margin-bottom: 10px;
      }
    }
  }

  &--dense {
    :deep(.arco-card-body) {
      padding: 0;
    }

    .content {
      padding: 4px 14px;
    }

    .welcome {
      margin: 0;

      .hello {
        font-size: 1rem;
        margin-bottom: 0;
        line-height: 1.35;
      }
    }

    .panel-divider {
      height: 44px;
      margin: 0 12px;
    }

    .notice-panel {
      min-height: 44px;

      .notice-header {
        margin-bottom: 2px;
        padding: 0 16px;

        .notice-title {
          font-size: 13px;
        }

        .notice-more {
          right: 8px;
        }
      }
    }

    .notice-empty,
    .notice-viewport {
      height: 28px;
      max-width: none;
    }

    .notice-item {
      height: 28px;
    }
  }
}

.module {
  min-width: 0;

  &--welcome {
    flex: 5 1 0;
  }

  &--weather {
    flex: 9 1 0;
  }

  &--notice {
    flex: 10 1 0;

    &.module--notice-wide {
      flex: 19 1 0;
    }
  }
}

/** 模块间浅灰竖分隔线 */
.panel-divider {
  flex-shrink: 0;
  align-self: center;
  width: 1px;
  height: 72px;
  margin: 0 16px;
  background-color: var(--color-border-2);
}

.notice-panel {
  min-height: 68px;

  .notice-header {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8px;
    padding: 0 24px;
    box-sizing: border-box;

    .notice-title {
      font-size: 1rem;
      font-weight: 600;
      line-height: 1.2;
      color: var(--color-text-1);
    }

    .notice-more {
      position: absolute;
      right: 24px;
      top: 50%;
      transform: translateY(-50%);
    }
  }
}

.notice-empty {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
  font-size: 13px;
}

.notice-viewport {
  height: 56px;
  overflow: hidden;
  width: 100%;
  max-width: 666px;
  margin: 0 auto;
  box-sizing: border-box;
}

.notice-track {
  will-change: transform;
  backface-visibility: hidden;
  transform: translateZ(0);

  &.scroll {
    animation-name: notice-scroll;
    animation-timing-function: linear;
    animation-iteration-count: infinite;
  }

  &.paused {
    animation-play-state: paused;
  }

  // 滚动中禁用 hover 变色，避免字在指针下闪蓝
  &:not(.paused) .notice-item:hover .notice-text {
    color: var(--color-text-1);
  }
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 28px;
  gap: 32px;
  cursor: pointer;
  padding: 0 8px 0 12px;
  min-width: 0;
  box-sizing: border-box;

  .notice-main {
    display: flex;
    align-items: center;
    gap: 16px;
    min-width: 0;
    flex: 1;
  }

  .notice-dot {
    flex-shrink: 0;
    width: 9px;
    height: 9px;
    border-radius: 2px;

    &.level-primary {
      background: rgb(var(--arcoblue-6));
    }
    &.level-success {
      background: rgb(var(--green-6));
    }
    &.level-warning {
      background: rgb(var(--orangered-6));
    }
    &.level-error {
      background: rgb(var(--red-6));
    }
    &.level-default {
      background: rgb(var(--gray-6));
    }
  }

  .notice-label {
    flex-shrink: 0;
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.1em;

    &.level-primary {
      color: rgb(var(--arcoblue-6));
    }
    &.level-success {
      color: rgb(var(--green-6));
    }
    &.level-warning {
      color: rgb(var(--orangered-6));
    }
    &.level-error {
      color: rgb(var(--red-6));
    }
    &.level-default {
      color: rgb(var(--gray-8));
    }
  }

  .notice-text {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--color-text-2);
    font-size: 13px;
    letter-spacing: 0.12em;
  }

  .notice-tag {
    flex-shrink: 0;
    margin-right: 4px;
    padding: 0 6px;
    height: 18px;
    line-height: 18px;
    border-radius: 2px;
    font-size: 12px;
    letter-spacing: 0.12em;
    color: rgb(var(--red-6));
    background: rgba(var(--red-6), 0.1);
    border: 1px solid rgba(var(--red-6), 0.35);
  }

  .notice-time {
    flex-shrink: 0;
    margin-left: 8px;
    font-size: 12px;
    color: var(--color-text-3);
    font-variant-numeric: tabular-nums;
    letter-spacing: 0.08em;
  }

  &:hover {
    .notice-text {
      color: rgb(var(--arcoblue-6));
    }
  }
}

@keyframes notice-scroll {
  0% {
    transform: translate3d(0, 0, 0);
  }
  100% {
    transform: translate3d(0, var(--notice-scroll-y, -50%), 0);
  }
}

@media (max-width: 575px) {
  .card .content {
    flex-direction: column;
    align-items: stretch;
  }

  .module {
    &--welcome,
    &--weather,
    &--notice,
    &--notice.module--notice-wide {
      flex: none;
      width: 100%;
    }
  }

  .panel-divider {
    width: 72px;
    height: 1px;
    margin: 12px auto;
  }
}
</style>
