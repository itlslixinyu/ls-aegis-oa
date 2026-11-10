import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSiteOptions } from '@/api/system'

/** localStorage 缓存 key：用于页面刷新时首屏同步设置标题，避免闪烁 */
const CACHE_KEY = 'h5-app-title-cache'

function readCache() {
  try {
    return JSON.parse(localStorage.getItem(CACHE_KEY) || '{}')
  } catch (e) {
    return {}
  }
}

function writeCache(cache) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache))
  } catch (e) {
    // localStorage 被禁用时静默忽略
  }
}

/**
 * 将平台 LabelValue 列表转为 code→value Map
 * （后端 LabelValueResp：label=option.code，value=配置值）
 */
function toOptionMap(list) {
  const map = new Map()
  if (!Array.isArray(list)) return map
  for (const item of list) {
    if (item && item.label != null) {
      map.set(item.label, item.value)
    }
  }
  return map
}

/**
 * 系统外观配置状态管理
 * 从平台 SITE option 加载名称、Logo、版权等，H5 各页共享
 */
export const useSystemStore = defineStore('system', () => {
  const systemName = ref('ITLS-EOMS 维修服务')
  const subtitle = ref('设备运维一体化管理平台')
  const logoUrl = ref('')
  const footerContent = ref('Copyright © 2018-2026 ITLS. All Rights Reserved.')
  const h5Title = ref('ITLS-EOMS 维修服务')
  const faviconUrl = ref('')
  const icpNumber = ref('')
  const publicSecurityNumber = ref('')
  /** 当前页面标题（路由 meta.title），供刷新时恢复成"页面名"而非纯系统名 */
  const lastPageTitle = ref('')

  /**
   * 持久化标题缓存 + 按优先级计算并设置最终标题
   * 优先级：lastPageTitle（当前页面名，来自路由 meta.title）> h5Title（系统级 H5 标题）
   */
  function persistTitleCache() {
    const cache = readCache()
    if (h5Title.value) cache.h5Title = h5Title.value
    if (lastPageTitle.value) cache.lastPageTitle = lastPageTitle.value
    writeCache(cache)
  }

  /** 按当前缓存的优先级设置 document.title */
  function applyTitle() {
    document.title = lastPageTitle.value || h5Title.value
    persistTitleCache()
  }

  /** 设置当前页面标题（路由切换时调用），会立刻应用并持久化缓存 */
  function setPageTitle(pageTitle) {
    if (pageTitle) {
      lastPageTitle.value = pageTitle
    } else {
      lastPageTitle.value = ''
    }
    applyTitle()
  }

  /** 从平台 SITE 配置加载外观 */
  async function loadSystemInfo() {
    try {
      const res = await getSiteOptions()
      // 拦截器成功时返回 R；data 为 LabelValue 列表
      const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
      const map = toOptionMap(list)
      if (map.size === 0) return

      const title = map.get('SITE_TITLE') || map.get('SITE_TITLE_SHORT')
      if (title) {
        systemName.value = title
        h5Title.value = title
      }
      const short = map.get('SITE_TITLE_SHORT')
      if (short) {
        subtitle.value = short
      }
      logoUrl.value = map.get('SITE_LOGO') || ''
      footerContent.value = map.get('SITE_COPYRIGHT') || footerContent.value
      faviconUrl.value = map.get('SITE_FAVICON') || ''
      icpNumber.value = map.get('SITE_BEIAN') || ''
      publicSecurityNumber.value = map.get('SITE_BEIAN_GONGAN') || ''

      applyTitle()
      if (faviconUrl.value) {
        let link = document.querySelector("link[rel*='icon']")
        if (!link) {
          link = document.createElement('link')
          link.rel = 'icon'
          document.head.appendChild(link)
        }
        link.href = faviconUrl.value
      }
    } catch (e) {
      // 加载失败使用默认值，不影响系统运行
      console.warn('加载系统信息失败，使用默认值', e)
    }
  }

  return {
    systemName, subtitle, logoUrl, footerContent, h5Title, faviconUrl, icpNumber, publicSecurityNumber,
    lastPageTitle, loadSystemInfo, setPageTitle, applyTitle, persistTitleCache
  }
})
