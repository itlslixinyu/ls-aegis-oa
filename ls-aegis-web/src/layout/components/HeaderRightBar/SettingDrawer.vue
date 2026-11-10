<template>
  <a-drawer
    v-model:visible="visible"
    title="项目配置"
    width="300px"
    unmount-on-close
    :footer="false"
    class="setting-drawer-wrap"
  >
    <div class="setting-drawer">
      <!-- 整体风格设置 -->
      <section class="setting-section">
        <div class="setting-section__title">整体风格设置</div>
        <a-row :gutter="12">
          <a-col v-for="item in STYLE_OPTIONS" :key="item.value" :span="8">
            <StyleItem :mode="item.value" :active="currentStyle === item.value" @click="toggleStyle(item.value)" />
          </a-col>
        </a-row>
      </section>

      <!-- 整体界面布局 -->
      <section v-if="settingOpen" class="setting-section">
        <div class="setting-section__title">整体界面布局</div>
        <a-row :gutter="[12, 12]">
          <a-col v-for="item in LAYOUT_OPTIONS" :key="item.value" :span="8">
            <LayoutItem :mode="item.value" :name="item.label" @click="toggleLayout(item.value)" />
          </a-col>
        </a-row>
      </section>

      <!-- 主题色 -->
      <section class="setting-section">
        <div class="setting-section__title">主题色</div>
        <div class="theme-colors">
          <div
            v-for="color in themeColorList"
            :key="color"
            class="theme-colors__item"
            :style="{ backgroundColor: color }"
            @click="appStore.setThemeColor(color)"
          >
            <icon-check v-if="appStore.themeColor?.toLowerCase() === color.toLowerCase()" :size="14" />
          </div>
        </div>
      </section>

      <!-- 功能开关 -->
      <section class="setting-section setting-section--list">
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">面包屑</span>
          <a-switch v-model="appStore.showBreadcrumb" size="small" />
        </div>
        <div class="setting-row">
          <span class="setting-row__label">表格边框</span>
          <a-switch v-model="appStore.tableBordered" size="small" />
        </div>
        <div class="setting-row">
          <span class="setting-row__label">表格尺寸</span>
          <a-select
            v-model="appStore.tableSize"
            placeholder="请选择"
            size="small"
            :options="tableSizeOptions"
            :trigger-props="{ autoFitPopupMinWidth: true }"
            :style="{ width: '110px' }"
          />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">多标签</span>
          <a-switch v-model="appStore.tab" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">页签风格</span>
          <a-select
            v-model="appStore.tabMode"
            placeholder="请选择"
            size="small"
            :options="tabModeList"
            :disabled="!appStore.tab"
            :trigger-props="{ autoFitPopupMinWidth: true }"
            :style="{ width: '110px' }"
          />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">折叠菜单</span>
          <a-switch v-model="appStore.menuCollapse" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">菜单排他展开</span>
          <a-switch v-model="appStore.menuAccordion" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">登录用户水印</span>
          <a-switch v-model="appStore.isOpenWatermark" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">页脚版权信息</span>
          <a-switch v-model="appStore.copyrightDisplay" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">动画显示</span>
          <a-switch v-model="appStore.animate" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">动画类型</span>
          <a-select
            v-model="appStore.animateMode"
            placeholder="请选择"
            size="small"
            :options="animateModeList"
            :disabled="!appStore.animate"
            :style="{ width: '110px' }"
          />
        </div>
        <div class="setting-row">
          <span class="setting-row__label">色弱模式</span>
          <a-switch v-model="appStore.enableColorWeaknessMode" size="small" />
        </div>
        <div v-if="settingOpen" class="setting-row">
          <span class="setting-row__label">灰色模式</span>
          <a-switch v-model="appStore.enableMourningMode" size="small" />
        </div>
      </section>

      <a-space direction="vertical" fill class="setting-actions">
        <a-alert :show-icon="false" type="info">
          点击「保存到我的账号」可将当前样式永久绑定到登录账号，换设备登录后自动生效
        </a-alert>
        <a-button type="primary" long :loading="saving" @click="saveToAccount">
          <template #icon>
            <icon-save />
          </template>
          保存到我的账号
        </a-button>
      </a-space>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import LayoutItem from './components/LayoutItem.vue'
import StyleItem from './components/StyleItem.vue'
import type { StyleMode } from './components/StyleItem.vue'
import { saveUserUiSettings } from '@/apis/system/user-profile'
import { useAppStore } from '@/stores'

defineOptions({ name: 'SettingDrawer' })

const appStore = useAppStore()
const visible = ref(false)
const saving = ref(false)
const settingOpen = JSON.parse(import.meta.env.VITE_APP_SETTING)

interface LayoutOption {
  label: string
  value: App.AppSettings['layout']
}

/** 整体风格：深色侧栏 / 浅色 / 暗黑 */
const STYLE_OPTIONS: { label: string, value: StyleMode }[] = [
  { label: '深色侧栏', value: 'dark-menu' },
  { label: '浅色', value: 'light' },
  { label: '暗黑', value: 'dark' },
]

/** 布局选项 */
const LAYOUT_OPTIONS: LayoutOption[] = [
  { label: '默认', value: 'left' },
  { label: '混合', value: 'mix' },
  { label: '顶部', value: 'top' },
  { label: '双列', value: 'columns' },
]

const tabModeList: App.TabItem[] = [
  { label: '卡片', value: 'card' },
  { label: '间隔卡片', value: 'card-gutter' },
  { label: '圆角', value: 'rounded' },
]

const tableSizeOptions = [
  { label: '迷你', value: 'mini' },
  { label: '小型', value: 'small' },
  { label: '中等', value: 'medium' },
  { label: '大型', value: 'large' },
]

const animateModeList: App.AnimateItem[] = [
  { label: '默认', value: 'zoom-fade' },
  { label: '滑动', value: 'fade-slide' },
  { label: '渐变', value: 'fade' },
  { label: '底部滑出', value: 'fade-bottom' },
  { label: '缩放消退', value: 'fade-scale' },
]

/** 主题色色板（对齐常见管理端色块布局） */
const themeColorList = [
  '#F53F3F',
  '#F77234',
  '#FF7D00',
  '#F7BA1E',
  '#00B42A',
  '#14C9C9',
  '#165DFF',
  '#3491FA',
  '#722ED1',
  '#D91AD9',
  '#F5319D',
  '#1D2129',
]

const currentStyle = computed<StyleMode>(() => {
  if (appStore.theme === 'dark') return 'dark'
  if (appStore.menuDark) return 'dark-menu'
  return 'light'
})

const open = () => {
  visible.value = true
}

const toggleStyle = (mode: StyleMode) => {
  if (mode === 'dark') {
    appStore.toggleTheme(true)
    return
  }
  appStore.toggleTheme(false)
  appStore.menuDark = mode === 'dark-menu'
}

const toggleLayout = (layout: App.AppSettings['layout']) => {
  appStore.layout = layout
}

const saveToAccount = async () => {
  saving.value = true
  try {
    await saveUserUiSettings(appStore.exportSettings())
    Message.success({ content: '已绑定到当前账号' })
  } catch {
    Message.error({ content: '保存失败，请稍后重试' })
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.setting-drawer {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.setting-section {
  &__title {
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-1);
  }

  &--list {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 36px;
  padding: 4px 0;

  &__label {
    font-size: 14px;
    color: var(--color-text-1);
  }
}

.theme-colors {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;

  &__item {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 3px;
    cursor: pointer;
    color: #fff;
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);

    &:hover {
      transform: scale(1.08);
    }
  }
}

.setting-actions {
  margin-top: 4px;
}
</style>
