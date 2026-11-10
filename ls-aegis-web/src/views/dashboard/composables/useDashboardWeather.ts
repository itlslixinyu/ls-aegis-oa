import { isEqual } from 'lodash-es'
import { useDocumentVisibility } from '@vueuse/core'
import { getWeatherNow, listWeatherOptionDict } from '@/apis/system'
import {
  createMockWeather,
  defaultWeatherRuntimeConfig,
  listActiveWeatherAlerts,
  mapWeatherNowToDashboard,
  tryBrowserGeolocation,
  type DashboardWeather,
  type WeatherRuntimeConfig,
} from '../utils/weather'

export interface UseDashboardWeatherOptions {
  /** 是否在挂载时自动加载配置并取数，默认 true */
  autoLoad?: boolean
  /** 天气联网状态变化时回调（运营中枢用于刷新运行指示灯） */
  onNetStatusChange?: (ok: boolean) => void
}

/**
 * 工作总览 / 运营中枢共用的天气配置、取数与轮询。
 * 和风（qweather）失败不回退 mock，静默降级。
 */
export function useDashboardWeather(options: UseDashboardWeatherOptions = {}) {
  const { autoLoad = true, onNetStatusChange } = options

  /** 进入页先不启用，等配置加载后再按设置取数，避免本地→联网重影 */
  const weatherRuntime = ref<WeatherRuntimeConfig>({
    ...defaultWeatherRuntimeConfig(),
    enabled: false,
  })
  const weatherEnabled = computed(() => weatherRuntime.value.enabled)
  const weather = ref<DashboardWeather | null>(null)
  const loading = ref(false)
  /** 天气互联网数据是否可用（配置本地模拟时视为可用） */
  const weatherNetOk = ref(true)

  const weatherTitle = computed(() => {
    if (!weather.value) return ''
    const w = weather.value
    const parts = [
      w.city,
      w.label,
      `温度${w.temp}°C`,
      w.feelsLike != null ? `体感${w.feelsLike}°C` : '',
      `空气质量 ${w.aqiCategory}${w.aqi != null ? `(${w.aqi})` : ''}`,
      `风力 ${w.windLevel}级`,
      w.windDir || '',
      w.humidity != null ? `湿度${w.humidity}%` : '',
      w.visibility != null ? `能见度${w.visibility}km` : '',
      w.pressure != null ? `气压${w.pressure}hPa` : '',
      w.precip != null ? `降水${w.precip}mm` : '',
    ].filter(Boolean)
    const alerts = listActiveWeatherAlerts(w).map((i) => i.label)
    return alerts.length ? `${parts.join('|')}|${alerts.join('|')}` : parts.join('|')
  })

  let weatherTimer: ReturnType<typeof setInterval> | undefined
  let active = true
  let configLoaded = false
  let pendingRefresh: Promise<void> | undefined
  const visibility = useDocumentVisibility()

  const notifyNetStatus = (ok: boolean) => {
    weatherNetOk.value = ok
    onNetStatusChange?.(ok)
  }

  const fetchWeather = async () => {
    if (!weatherRuntime.value.enabled) {
      weather.value = null
      notifyNetStatus(true)
      return
    }
    /** 联网（和风）为最高原则：只展示联网结果，失败不回退本地模拟，避免重影 */
    const needNet = weatherRuntime.value.provider === 'qweather'
    try {
      let lat: number | undefined
      let lon: number | undefined
      // 本地模拟天气不依赖定位；避免为模拟数据等待浏览器定位超时。
      if (needNet && weatherRuntime.value.cityMode === 'auto') {
        const geo = await tryBrowserGeolocation()
        if (geo) {
          // 和风坐标反查建议最多两位小数
          lat = Math.round(geo.lat * 100) / 100
          lon = Math.round(geo.lon * 100) / 100
        }
      }
      const { data } = await getWeatherNow(lat != null && lon != null ? { lat, lon } : undefined)
      if (data?.city) {
        const fromNet = String(data.provider || '') === 'qweather'
        if (needNet && !fromNet) {
          // 后端因失败回退了模拟数据：不展示，保持空白/上次联网结果
          notifyNetStatus(false)
          return
        }
        // 无变化不覆盖，避免轮询时天气区无意义重渲染
        const mapped = mapWeatherNowToDashboard(data)
        if (!isEqual(weather.value, mapped)) {
          weather.value = mapped
        }
        notifyNetStatus(!needNet || fromNet)
        return
      }
    } catch {
      // 静默：联网失败不抛错
    }
    if (needNet) {
      notifyNetStatus(false)
      return
    }
    const mock = createMockWeather(weatherRuntime.value.city)
    if (!isEqual(weather.value, mock)) {
      weather.value = mock
    }
    notifyNetStatus(true)
  }

  const refreshWeather = () => {
    if (!active || visibility.value === 'hidden') return Promise.resolve()
    // 重连、手动刷新与轮询共享同一次在途请求，不跨账号持久缓存。
    if (!pendingRefresh) {
      pendingRefresh = fetchWeather().finally(() => { pendingRefresh = undefined })
    }
    return pendingRefresh
  }

  const stopWeatherTimer = () => {
    if (weatherTimer) clearInterval(weatherTimer)
    weatherTimer = undefined
  }

  const restartWeatherTimer = () => {
    stopWeatherTimer()
    if (!configLoaded || !active || visibility.value === 'hidden' || !weatherRuntime.value.enabled) return
    const ms = Math.max(60, weatherRuntime.value.refreshInterval) * 1000
    weatherTimer = setInterval(() => {
      void refreshWeather()
    }, ms)
  }

  const loadWeatherConfig = async () => {
    loading.value = true
    try {
      try {
        const { data } = await listWeatherOptionDict()
        const map = Object.fromEntries((data || []).map((i) => [i.label, i.value]))
        weatherRuntime.value = {
          enabled: String(map.WEATHER_ENABLED ?? '1') !== '0',
          city: String(map.WEATHER_CITY || weatherRuntime.value.city || '北京').trim() || '北京',
          cityMode: String(map.WEATHER_CITY_MODE || 'auto') === 'fixed' ? 'fixed' : 'auto',
          refreshInterval: Math.max(60, Number(map.WEATHER_REFRESH_INTERVAL) || 600),
          provider: String(map.WEATHER_PROVIDER || 'mock'),
        }
      } catch {
        weatherRuntime.value = defaultWeatherRuntimeConfig()
      }
      configLoaded = true
      await refreshWeather()
      restartWeatherTimer()
    } finally {
      loading.value = false
    }
  }

  /** 浏览器离线时标记天气联网失败 */
  const markWeatherOffline = () => {
    notifyNetStatus(false)
  }

  if (autoLoad) {
    onMounted(() => {
      void loadWeatherConfig()
    })
  }

  onActivated(() => {
    active = true
    restartWeatherTimer()
  })
  onDeactivated(() => {
    active = false
    stopWeatherTimer()
  })
  watch(visibility, (state) => {
    if (state === 'hidden') stopWeatherTimer()
    else if (active && configLoaded) {
      void refreshWeather()
      restartWeatherTimer()
    }
  })

  onBeforeUnmount(() => {
    active = false
    stopWeatherTimer()
  })

  return {
    weatherRuntime,
    weatherEnabled,
    weather,
    weatherTitle,
    weatherNetOk,
    loading,
    loadWeatherConfig,
    refreshWeather,
    restartWeatherTimer,
    stopWeatherTimer,
    markWeatherOffline,
  }
}
