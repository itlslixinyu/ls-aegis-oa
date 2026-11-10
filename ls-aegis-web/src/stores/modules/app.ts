import { defineStore } from 'pinia'
import { computed, reactive, toRefs } from 'vue'
import { generate, getRgbStr } from '@arco-design/color'
import { type BasicConfig, listSiteOptionDict } from '@/apis/system'
import { getUserUiSettings } from '@/apis/system/user-profile'
import { getSettings } from '@/config/setting'

const storeSetup = () => {
  // App配置
  const settingConfig = reactive({ ...getSettings() }) as App.AppSettings
  // 兼容旧本地缓存：缺省字段补齐
  if (settingConfig.showBreadcrumb === undefined) {
    settingConfig.showBreadcrumb = true
  }
  if (settingConfig.tableBordered === undefined) {
    settingConfig.tableBordered = true
  }
  if (settingConfig.tableSize === undefined) {
    settingConfig.tableSize = 'large'
  }
  if (settingConfig.isOpenWatermark === undefined) {
    settingConfig.isOpenWatermark = false
  }
  // 页面切换动画类名
  const transitionName = computed(() => (settingConfig.animate ? settingConfig.animateMode : ''))

  // 深色菜单主题色变量
  const themeCSSVar = computed<Record<string, string>>(() => {
    const obj: Record<string, string> = {}
    const list = generate(settingConfig.themeColor, { list: true, dark: true }) as string[]
    list.forEach((color, index) => {
      obj[`--primary-${index + 1}`] = getRgbStr(color)
    })
    return obj
  })

  // 设置主题色
  const setThemeColor = (color: string) => {
    if (!color) return
    settingConfig.themeColor = color
    const list = generate(settingConfig.themeColor, { list: true, dark: settingConfig.theme === 'dark' }) as string[]
    list.forEach((color, index) => {
      const rgbStr = getRgbStr(color)
      document.body.style.setProperty(`--primary-${index + 1}`, rgbStr)
    })
  }

  // 切换主题 暗黑模式|简白模式
  const toggleTheme = (dark: boolean) => {
    if (dark) {
      settingConfig.theme = 'dark'
      document.body.setAttribute('arco-theme', 'dark')
    } else {
      settingConfig.theme = 'light'
      document.body.removeAttribute('arco-theme')
    }
    setThemeColor(settingConfig.themeColor)
  }

  // 初始化主题
  const initTheme = () => {
    if (!settingConfig.themeColor) return
    setThemeColor(settingConfig.themeColor)
  }

  // 设置左侧菜单折叠状态
  const setMenuCollapse = (collapsed: boolean) => {
    settingConfig.menuCollapse = collapsed
  }

  /** 导出当前可持久化的界面配置 */
  const exportSettings = (): App.AppSettings => {
    return {
      theme: settingConfig.theme,
      themeColor: settingConfig.themeColor,
      tab: settingConfig.tab,
      tabMode: settingConfig.tabMode,
      animate: settingConfig.animate,
      animateMode: settingConfig.animateMode,
      menuCollapse: settingConfig.menuCollapse,
      menuAccordion: settingConfig.menuAccordion,
      menuDark: settingConfig.menuDark,
      copyrightDisplay: settingConfig.copyrightDisplay,
      layout: settingConfig.layout,
      showBreadcrumb: settingConfig.showBreadcrumb,
      tableBordered: settingConfig.tableBordered,
      tableSize: settingConfig.tableSize,
      isOpenWatermark: settingConfig.isOpenWatermark,
      watermark: settingConfig.watermark,
      enableColorWeaknessMode: settingConfig.enableColorWeaknessMode,
      enableMourningMode: settingConfig.enableMourningMode,
    }
  }

  /** 应用账号绑定的界面配置（覆盖本地） */
  const applySettings = (settings: Partial<App.AppSettings>) => {
    if (!settings) return
    Object.assign(settingConfig, settings)
    if (settingConfig.showBreadcrumb === undefined) {
      settingConfig.showBreadcrumb = true
    }
    if (settingConfig.tableBordered === undefined) {
      settingConfig.tableBordered = true
    }
    if (settingConfig.tableSize === undefined) {
      settingConfig.tableSize = 'large'
    }
    if (settingConfig.isOpenWatermark === undefined) {
      settingConfig.isOpenWatermark = false
    }
    toggleTheme(settingConfig.theme === 'dark')
  }

  /** 登录后拉取并应用账号界面配置 */
  const loadAccountSettings = async () => {
    const res = await getUserUiSettings()
    if (res.data) {
      applySettings(res.data)
    }
  }

  // 系统配置配置
  const siteConfig = reactive({}) as BasicConfig
  // 初始化系统配置
  const initSiteConfig = () => {
    listSiteOptionDict().then((res) => {
      const resMap = new Map()
      res.data.forEach((item) => {
        resMap.set(item.label, item.value)
      })
      siteConfig.SITE_FAVICON = resMap.get('SITE_FAVICON')
      siteConfig.SITE_LOGO = resMap.get('SITE_LOGO')
      siteConfig.SITE_TITLE = resMap.get('SITE_TITLE')
      siteConfig.SITE_TITLE_SHORT = resMap.get('SITE_TITLE_SHORT')
      siteConfig.SITE_COMPANY = resMap.get('SITE_COMPANY')
      siteConfig.SITE_COMPANY_SHORT = resMap.get('SITE_COMPANY_SHORT')
      siteConfig.SITE_COPYRIGHT = resMap.get('SITE_COPYRIGHT')
      siteConfig.SITE_BEIAN = resMap.get('SITE_BEIAN')
      siteConfig.SITE_BEIAN_GONGAN = resMap.get('SITE_BEIAN_GONGAN')
      siteConfig.SITE_BEIAN_GONGAN_ICON = resMap.get('SITE_BEIAN_GONGAN_ICON')
      document.title = resMap.get('SITE_TITLE')
      document
        .querySelector('link[rel="shortcut icon"]')
        ?.setAttribute('href', resMap.get('SITE_FAVICON') || '/favicon.ico')
    })
  }

  // 设置系统配置
  const setSiteConfig = (config: BasicConfig) => {
    Object.assign(siteConfig, config)
    document.title = config.SITE_TITLE || ''
    document.querySelector('link[rel="shortcut icon"]')?.setAttribute('href', config.SITE_FAVICON || '/favicon.ico')
  }
  // 监听 色弱模式 和 哀悼模式
  watch([
    () => settingConfig.enableMourningMode,
    () => settingConfig.enableColorWeaknessMode,
  ], ([mourningMode, colorWeaknessMode]) => {
    const filters = [] as string[]
    if (mourningMode) {
      filters.push('grayscale(100%)')
    }
    if (colorWeaknessMode) {
      filters.push('invert(80%)')
    }
    // 如果没有任何滤镜条件，移除 `filter` 样式
    if (filters.length === 0) {
      document.documentElement.style.removeProperty('filter')
    } else {
      document.documentElement.style.setProperty('filter', filters.join(' '))
    }
  }, {
    immediate: true,
  })

  const getFavicon = () => {
    return siteConfig.SITE_FAVICON
  }

  const getLogo = () => {
    return siteConfig.SITE_LOGO
  }

  const getTitle = () => {
    return siteConfig.SITE_TITLE
  }

  const getTitleShort = () => {
    const short = (siteConfig.SITE_TITLE_SHORT || '').trim()
    if (short) {
      return short.slice(0, 8)
    }
    return '雷铄御警'
  }

  const getCompany = () => {
    return siteConfig.SITE_COMPANY
  }

  const getCompanyShort = () => {
    const short = (siteConfig.SITE_COMPANY_SHORT || '').trim()
    return short ? short.slice(0, 8) : siteConfig.SITE_COMPANY
  }

  const getCopyright = () => {
    return siteConfig.SITE_COPYRIGHT
  }

  const getForRecord = () => {
    return siteConfig.SITE_BEIAN
  }

  const getBeianGongan = () => {
    return siteConfig.SITE_BEIAN_GONGAN
  }

  const getBeianGonganIcon = () => {
    return siteConfig.SITE_BEIAN_GONGAN_ICON || '/beian-gongan.png'
  }
  return {
    ...toRefs(settingConfig),
    ...toRefs(siteConfig),
    transitionName,
    themeCSSVar,
    toggleTheme,
    setThemeColor,
    initTheme,
    setMenuCollapse,
    exportSettings,
    applySettings,
    loadAccountSettings,
    initSiteConfig,
    setSiteConfig,
    getFavicon,
    getLogo,
    getTitle,
    getTitleShort,
    getCompany,
    getCompanyShort,
    getCopyright,
    getForRecord,
    getBeianGongan,
    getBeianGonganIcon,
  }
}

export const useAppStore = defineStore('app', storeSetup, { persist: true })
