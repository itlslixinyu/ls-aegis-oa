/** 运营中枢顶栏天气（城市等由系统配置 WEATHER 驱动） */

/**
 * 天气现象（含可独立预警的灾害性天气）
 * 晴/多云/雨为常规；暴雨、雷电、冰雹、雪、雾、沙尘等可触发现象预警
 */
export type WeatherKind =
  | 'sunny'
  | 'cloudy'
  | 'rainy'
  | 'storm'
  | 'thunder'
  | 'hail'
  | 'snow'
  | 'fog'
  | 'sand'

/**
 * 国家气象灾害预警等级（国务院应急 / 中国气象局）
 * 蓝 → 黄 → 橙 → 红，由低到高
 */
export type WeatherAlertLevel = 'none' | 'blue' | 'yellow' | 'orange' | 'red'

/** 分项预警（各自独立，互不合成） */
export interface WeatherAlerts {
  /** 天气现象预警（冰雹、暴雨、雷电等） */
  weather: WeatherAlertLevel
  /** 气温预警（高温 / 低温） */
  temp: WeatherAlertLevel
  /** 空气质量预警（污染等级） */
  aqi: WeatherAlertLevel
  /** 风力预警（大风） */
  wind: WeatherAlertLevel
}

export interface DashboardWeather {
  city: string
  kind: WeatherKind
  label: string
  /** 本地天气图标路径（public/weather-icons） */
  iconSrc: string
  temp: number
  /** 体感温度（℃） */
  feelsLike: number | null
  /** 空气质量指数 */
  aqi: number | null
  /** 空气质量类别，如优/良/轻度污染 */
  aqiCategory: string
  /** 风力等级（蒲福风级 0–12） */
  windLevel: number
  /** 风向 */
  windDir: string
  /** 相对湿度（%） */
  humidity: number | null
  /** 能见度（公里） */
  visibility: number | null
  /** 大气压（百帕） */
  pressure: number | null
  /** 降水量（毫米） */
  precip: number | null
  /** 分项预警等级 */
  alerts: WeatherAlerts
}

/** 顶栏展示用的单条预警 */
export interface ActiveWeatherAlert {
  key: keyof WeatherAlerts
  level: Exclude<WeatherAlertLevel, 'none'>
  /** 如：冰雹橙色预警、高温黄色预警 */
  label: string
}

export interface WeatherRuntimeConfig {
  enabled: boolean
  city: string
  cityMode: 'auto' | 'fixed'
  /** 刷新间隔（秒） */
  refreshInterval: number
  provider: 'mock' | 'qweather' | string
}

const KIND_META: Record<
  WeatherKind,
  { label: string, icon: string, /** 现象默认预警（占位；接真接口后以服务端为准） */ alert: WeatherAlertLevel }
> = {
  sunny: { label: '晴', icon: '/weather-icons/sunny.svg', alert: 'none' },
  cloudy: { label: '多云', icon: '/weather-icons/cloudy.svg', alert: 'none' },
  rainy: { label: '雨', icon: '/weather-icons/rainy.svg', alert: 'none' },
  storm: { label: '暴雨', icon: '/weather-icons/storm.svg', alert: 'orange' },
  thunder: { label: '雷电', icon: '/weather-icons/thunder.svg', alert: 'yellow' },
  hail: { label: '冰雹', icon: '/weather-icons/hail.svg', alert: 'orange' },
  snow: { label: '暴雪', icon: '/weather-icons/snow.svg', alert: 'yellow' },
  fog: { label: '大雾', icon: '/weather-icons/fog.svg', alert: 'yellow' },
  sand: { label: '沙尘', icon: '/weather-icons/sand.svg', alert: 'yellow' },
}

/** 现象预警名称（无预警时不展示） */
const WEATHER_ALERT_NAME: Partial<Record<WeatherKind, string>> = {
  storm: '暴雨',
  thunder: '雷电',
  hail: '冰雹',
  snow: '暴雪',
  fog: '大雾',
  sand: '沙尘',
}

export const ALERT_LEVEL_LABEL: Record<WeatherAlertLevel, string> = {
  none: '无预警',
  blue: '蓝色预警',
  yellow: '黄色预警',
  orange: '橙色预警',
  red: '红色预警',
}

const ALERT_COLOR_LABEL: Record<Exclude<WeatherAlertLevel, 'none'>, string> = {
  blue: '蓝色',
  yellow: '黄色',
  orange: '橙色',
  red: '红色',
}

const DEFAULT_CITY = '北京'

const ALERT_RANK: Record<WeatherAlertLevel, number> = {
  none: 0,
  blue: 1,
  yellow: 2,
  orange: 3,
  red: 4,
}

/** 环境变量兜底城市 */
export const getEnvWeatherCity = (): string => {
  const fromEnv = (import.meta.env.VITE_DASHBOARD_WEATHER_CITY as string | undefined)?.trim()
  return fromEnv || DEFAULT_CITY
}

export const defaultWeatherRuntimeConfig = (): WeatherRuntimeConfig => ({
  enabled: true,
  city: getEnvWeatherCity(),
  cityMode: 'auto',
  refreshInterval: 600,
  provider: 'mock',
})

/** 浏览器定位（失败返回 null，不弹错） */
export const tryBrowserGeolocation = (): Promise<{ lat: number, lon: number } | null> => {
  return new Promise((resolve) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      resolve(null)
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude })
      },
      () => resolve(null),
      { enableHighAccuracy: false, timeout: 5000, maximumAge: 10 * 60 * 1000 },
    )
  })
}

/** 取更高等级（仅用于展示排序，不合成业务预警） */
export const higherAlertLevel = (a: WeatherAlertLevel, b: WeatherAlertLevel): WeatherAlertLevel =>
  ALERT_RANK[a] >= ALERT_RANK[b] ? a : b

/**
 * 气温独立预警（占位规则，对齐高温/低温预警量级）
 * 高温：蓝≥35 / 黄≥37 / 橙≥39 / 红≥40
 * 低温：蓝≤4 / 黄≤0 / 橙≤-8 / 红≤-12
 */
export const resolveTempAlert = (temp: number): { level: WeatherAlertLevel, kind: 'high' | 'low' | 'none' } => {
  if (temp >= 40) return { level: 'red', kind: 'high' }
  if (temp >= 39) return { level: 'orange', kind: 'high' }
  if (temp >= 37) return { level: 'yellow', kind: 'high' }
  if (temp >= 35) return { level: 'blue', kind: 'high' }
  if (temp <= -12) return { level: 'red', kind: 'low' }
  if (temp <= -8) return { level: 'orange', kind: 'low' }
  if (temp <= 0) return { level: 'yellow', kind: 'low' }
  if (temp <= 4) return { level: 'blue', kind: 'low' }
  return { level: 'none', kind: 'none' }
}

/**
 * 空气质量独立预警（对齐国标 AQI 类别）
 * 轻度污染→蓝 / 中度→黄 / 重度→橙 / 严重→红；优、良无预警
 */
export const resolveAqiAlert = (
  aqi: number | null | undefined,
  category?: string,
): WeatherAlertLevel => {
  const cat = (category || '').trim()
  if (cat.includes('严重')) return 'red'
  if (cat.includes('重度')) return 'orange'
  if (cat.includes('中度')) return 'yellow'
  if (cat.includes('轻度')) return 'blue'
  if (cat === '优' || cat === '良' || cat.includes('Good') || cat.includes('Excellent')) {
    return 'none'
  }
  if (aqi == null || Number.isNaN(Number(aqi))) return 'none'
  const n = Number(aqi)
  if (n > 300) return 'red'
  if (n > 200) return 'orange'
  if (n > 150) return 'yellow'
  if (n > 100) return 'blue'
  return 'none'
}

/**
 * 风力独立预警（占位规则，对齐大风预警量级）
 * 蓝≥6 / 黄≥8 / 橙≥10 / 红≥12
 */
export const resolveWindAlert = (windLevel: number): WeatherAlertLevel => {
  if (windLevel >= 12) return 'red'
  if (windLevel >= 10) return 'orange'
  if (windLevel >= 8) return 'yellow'
  if (windLevel >= 6) return 'blue'
  return 'none'
}

/** 天气现象独立预警（按 kind；接真接口后以服务端等级为准） */
export const resolveWeatherAlert = (kind: WeatherKind): WeatherAlertLevel => KIND_META[kind].alert

/** 汇总分项预警 */
export const resolveWeatherAlerts = (
  kind: WeatherKind,
  temp: number,
  aqi: number | null,
  aqiCategory: string,
  windLevel: number,
): WeatherAlerts => ({
  weather: resolveWeatherAlert(kind),
  temp: resolveTempAlert(temp).level,
  aqi: resolveAqiAlert(aqi, aqiCategory),
  wind: resolveWindAlert(windLevel),
})

/** 将后端实时天气归一为顶栏展示模型 */
export const mapWeatherNowToDashboard = (data: {
  city: string
  kind: string
  label?: string
  temp: number
  feelsLike?: number | null
  aqi?: number | null
  aqiCategory?: string
  humidity?: number | null
  windLevel: number
  windDir?: string | null
  visibility?: number | null
  pressure?: number | null
  precip?: number | null
}): DashboardWeather => {
  const kind = (Object.prototype.hasOwnProperty.call(KIND_META, data.kind)
    ? data.kind
    : 'cloudy') as WeatherKind
  const meta = KIND_META[kind]
  const temp = Number(data.temp) || 0
  const feelsLike = data.feelsLike == null || Number.isNaN(Number(data.feelsLike))
    ? null
    : Number(data.feelsLike)
  const aqi = data.aqi == null || Number.isNaN(Number(data.aqi)) ? null : Number(data.aqi)
  const aqiCategory = (data.aqiCategory || '').trim() || (aqi != null ? String(aqi) : '--')
  const windLevel = Number(data.windLevel) || 0
  const humidity = data.humidity == null || Number.isNaN(Number(data.humidity))
    ? null
    : Number(data.humidity)
  const visibility = data.visibility == null || Number.isNaN(Number(data.visibility))
    ? null
    : Number(data.visibility)
  const pressure = data.pressure == null || Number.isNaN(Number(data.pressure))
    ? null
    : Number(data.pressure)
  const precip = data.precip == null || Number.isNaN(Number(data.precip))
    ? null
    : Number(data.precip)
  return {
    city: data.city || getEnvWeatherCity(),
    kind,
    label: data.label || meta.label,
    iconSrc: meta.icon,
    temp,
    feelsLike,
    aqi,
    aqiCategory,
    windLevel,
    windDir: (data.windDir || '').trim(),
    humidity,
    visibility,
    pressure,
    precip,
    alerts: resolveWeatherAlerts(kind, temp, aqi, aqiCategory, windLevel),
  }
}

/** 列出当前生效的独立预警（用于顶栏文案） */
export const listActiveWeatherAlerts = (weather: DashboardWeather): ActiveWeatherAlert[] => {
  const result: ActiveWeatherAlert[] = []
  const { alerts, kind, temp } = weather

  if (alerts.weather !== 'none') {
    const name = WEATHER_ALERT_NAME[kind] || weatherKindLabel(kind)
    result.push({
      key: 'weather',
      level: alerts.weather,
      label: `${name}${ALERT_COLOR_LABEL[alerts.weather]}预警`,
    })
  }

  if (alerts.temp !== 'none') {
    const tempKind = resolveTempAlert(temp).kind
    const name = tempKind === 'low' ? '低温' : '高温'
    result.push({
      key: 'temp',
      level: alerts.temp,
      label: `${name}${ALERT_COLOR_LABEL[alerts.temp]}预警`,
    })
  }

  if (alerts.aqi !== 'none') {
    result.push({
      key: 'aqi',
      level: alerts.aqi,
      label: `空气质量${ALERT_COLOR_LABEL[alerts.aqi]}预警`,
    })
  }

  if (alerts.wind !== 'none') {
    result.push({
      key: 'wind',
      level: alerts.wind,
      label: `大风${ALERT_COLOR_LABEL[alerts.wind]}预警`,
    })
  }

  return result
}

/** 演示用占位天气 */
export const createMockWeather = (city = getEnvWeatherCity()): DashboardWeather => {
  const kinds = Object.keys(KIND_META) as WeatherKind[]
  const kind = kinds[Math.floor(Math.random() * kinds.length)]
  const meta = KIND_META[kind]
  const aqiSamples: Array<{ aqi: number, category: string }> = [
    { aqi: 35, category: '优' },
    { aqi: 72, category: '良' },
    { aqi: 120, category: '轻度污染' },
    { aqi: 175, category: '中度污染' },
    { aqi: 250, category: '重度污染' },
    { aqi: 350, category: '严重污染' },
  ]
  const air = aqiSamples[Math.floor(Math.random() * aqiSamples.length)]
  const temp = -15 + Math.floor(Math.random() * 58)
  const windLevel = Math.floor(Math.random() * 13)
  const windDirs = ['北风', '东北风', '东风', '东南风', '南风', '西南风', '西风', '西北风']
  return {
    city,
    kind,
    label: meta.label,
    iconSrc: meta.icon,
    temp,
    feelsLike: temp - 2 + Math.floor(Math.random() * 5),
    aqi: air.aqi,
    aqiCategory: air.category,
    windLevel,
    windDir: windDirs[Math.floor(Math.random() * windDirs.length)],
    humidity: 20 + Math.floor(Math.random() * 71),
    visibility: Math.round((0.5 + Math.random() * 24.5) * 10) / 10,
    pressure: 990 + Math.floor(Math.random() * 40),
    precip: Math.round(Math.random() * 120) / 10,
    alerts: resolveWeatherAlerts(kind, temp, air.aqi, air.category, windLevel),
  }
}

export const weatherKindLabel = (kind: WeatherKind): string => KIND_META[kind].label
