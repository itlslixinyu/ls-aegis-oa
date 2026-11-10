<template>
  <GiPageLayout
    :margin="true"
    :default-collapsed="false"
    :header-style="isDesktop ? { padding: 0, borderBottomWidth: 0 } : { borderBottomWidth: '1px' } "
  >
    <template v-if="isDesktop" #left>
      <a-tabs v-model:active-key="activeKey" type="rounded" position="left" hide-content size="large" @change="change">
        <a-tab-pane v-for="(item) in menuList" :key="item.key">
          <template #title>
            <div style="display: flex; align-items: center">
              <GiSvgIcon :name="item.icon" :size="18" style="margin-right: 4px" />
              {{ item.name }}
            </div>
          </template>
        </a-tab-pane>
      </a-tabs>
    </template>
    <template #header>
      <a-tabs v-if="!isDesktop" v-model:active-key="activeKey" type="rounded" position="top" size="large" @change="change">
        <a-tab-pane v-for="(item) in menuList" :key="item.key" :title="item.name">
          <template #title>
            <div style="display: flex; align-items: center">
              <GiSvgIcon :name="item.icon" :size="18" style="margin-right: 4px" />
              {{ item.name }}
            </div>
          </template>
        </a-tab-pane>
      </a-tabs>
    </template>
    <transition name="fade-slide" mode="out-in" appear>
      <component :is="menuList.find((item) => item.key === activeKey)?.value"></component>
    </transition>
  </GiPageLayout>
</template>

<script setup lang="tsx">
import { defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDevice } from '@/hooks'
import has from '@/utils/has'

const SiteConfig = defineAsyncComponent(() => import('./site/index.vue'))
const SecurityConfig = defineAsyncComponent(() => import('./security/index.vue'))
const LoginConfig = defineAsyncComponent(() => import('./login/index.vue'))
const MailConfig = defineAsyncComponent(() => import('./mail/index.vue'))
const WeatherConfig = defineAsyncComponent(() => import('./weather/index.vue'))
const DashboardConfig = defineAsyncComponent(() => import('./dashboard/index.vue'))
const ModuleConfig = defineAsyncComponent(() => import('./module/index.vue'))
const EomsConfig = defineAsyncComponent(() => import('./eoms/index.vue'))
const H5AuthConfig = defineAsyncComponent(() => import('./h5-auth/index.vue'))
const StorageConfig = defineAsyncComponent(() => import('./storage/index.vue'))
const ClientConfig = defineAsyncComponent(() => import('./client/index.vue'))

defineOptions({ name: 'SystemConfig' })

const { isDesktop } = useDevice()

const data = [
  { name: '网站配置', key: 'site', icon: 'apps', permissions: ['system:siteConfig:get'], value: SiteConfig },
  { name: '安全配置', key: 'security', icon: 'safe', permissions: ['system:securityConfig:get'], value: SecurityConfig },
  { name: '登录配置', key: 'login', icon: 'lock', permissions: ['system:loginConfig:get'], value: LoginConfig },
  { name: '邮件配置', key: 'mail', icon: 'email', permissions: ['system:mailConfig:get'], value: MailConfig },
  { name: '天气配置', key: 'weather', icon: 'cloud', permissions: ['system:weatherConfig:get'], value: WeatherConfig },
  { name: '大屏配置', key: 'dashboard', icon: 'dashboard', permissions: ['system:dashboardConfig:get'], value: DashboardConfig },
  { name: '功能模块', key: 'module', icon: 'apps', permissions: ['system:moduleConfig:get'], value: ModuleConfig },
  { name: '运维报修', key: 'eoms', icon: 'tool', permissions: ['system:eomsConfig:get'], value: EomsConfig },
  { name: 'H5客户授权', key: 'h5Auth', icon: 'mobile', permissions: ['system:h5Auth:get', 'system:h5Auth:list'], value: H5AuthConfig },
  { name: '存储配置', key: 'storage', icon: 'storage', permissions: ['system:storage:list'], value: StorageConfig },
  { name: '客户端配置', key: 'client', icon: 'mobile', permissions: ['system:client:list'], value: ClientConfig },
]

const menuList = computed(() => {
  return data.filter((item) => {
    return has.hasPermOr(item.permissions)
  })
})

const route = useRoute()
const router = useRouter()
const activeKey = ref(menuList.value[0]?.key || '')
watch(
  menuList,
  (list) => {
    if (!list.length) {
      activeKey.value = ''
      return
    }
    if (!list.some((item) => item.key === activeKey.value)) {
      activeKey.value = list[0].key
    }
  },
  { immediate: true },
)
watch(
  () => route.query,
  () => {
    if (route.query.tab) {
      const tab = String(route.query.tab)
      if (menuList.value.some((item) => item.key === tab)) {
        activeKey.value = tab
      }
    }
  },
  { immediate: true },
)
const change = (key: string | number) => {
  activeKey.value = key as string
  router.replace({ path: route.path, query: { tab: key } })
}
</script>

<style scoped lang="scss">
.gi_page {
  padding-top: 0;
}

.tab-pane-item{
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

:deep(.arco-tabs-nav-vertical.arco-tabs-nav-type-line .arco-tabs-tab) {
  margin: 0;
  padding: 8px 16px;

  &:hover {
    background: var(--color-fill-1);

    .arco-tabs-tab-title {
      &::before {
        display: none !important;
      }
    }
  }

  &.arco-tabs-tab-active {
    background: rgba(var(--primary-6), 0.08);
  }
}

:deep(.arco-tabs-nav-vertical::before) {
  left: 0;
  display: none;
}

:deep(.arco-tabs-nav-vertical .arco-tabs-nav-ink) {
  left: 0;
}

:deep(.arco-tabs-nav-vertical) {
  float: none;
  flex-direction: row;
}

:deep(.arco-tabs .arco-tabs-nav-type-card-gutter .arco-tabs-tab-active) {
  box-shadow: inset 0 2px 0 rgb(var(--primary-6)), inset -1px 0 0 var(--color-border-2),
  inset 1px 0 0 var(--color-border-2);
  position: relative;
}

:deep(.arco-tabs-nav-type-card-gutter .arco-tabs-tab) {
  border-radius: var(--border-radius-medium) var(--border-radius-medium) 0 0;
}

:deep(.arco-tabs-type-card-gutter > .arco-tabs-content) {
  border: none;
}

:deep(.arco-tabs-nav::before) {
  left: -20px;
  right: -20px;
}

:deep(.arco-tabs) {
  overflow: hidden;
}

:deep(.arco-tabs-nav) {
  overflow: visible;
}

:deep(.arco-tabs-nav-type-rounded .arco-tabs-tab){
  border-radius: 8px;
}

:deep(.arco-tabs-tab-title){
  width: 100%;
}
</style>
