import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useTenantStore = defineStore('tenant', () => {
  const tenantEnabled = ref<boolean>(false)
  const tenantId = ref<string>()

  const setTenantEnable = (status: boolean) => {
    tenantEnabled.value = status
  }
  const setTenantId = (id: string) => {
    tenantId.value = id
  }

  // 多租户开启即显示租户编码框（与域名解析解耦）
  const needInputTenantCode = computed(() => tenantEnabled.value)

  // 判断租户是否已正确配置
  const isTenantConfigured = computed(() => {
    return tenantEnabled.value && !!tenantId.value
  })

  // 清空租户ID
  const resetTenantId = () => {
    tenantId.value = undefined
  }
  return {
    tenantEnabled,
    tenantId,
    setTenantEnable,
    setTenantId,
    needInputTenantCode,
    isTenantConfigured,
    resetTenantId,
  }
}, {
  persist: { paths: ['tenantEnabled', 'tenantId'], storage: localStorage },
})
