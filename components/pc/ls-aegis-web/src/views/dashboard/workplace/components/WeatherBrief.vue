<template>
  <div class="weather-brief" :title="title || undefined">
    <a-skeleton v-if="loading" :animation="true">
      <a-skeleton-line :rows="2" />
    </a-skeleton>
    <div v-else-if="!weather" class="weather-brief__empty">暂无天气</div>
    <div v-else class="weather-brief__body">
      <div class="weather-brief__head">
        <img
          class="weather-brief__icon"
          :src="weather.iconSrc"
          :alt="weather.label"
        >
        <div class="weather-brief__summary">
          <div class="weather-brief__title">
            <span class="weather-brief__city">{{ weather.city }}</span>
            <span
              class="weather-brief__label"
              :class="weather.alerts.weather !== 'none' ? `alert-${weather.alerts.weather}` : undefined"
            >{{ weather.label }}</span>
          </div>
          <div class="weather-brief__temps">
            <span
              class="weather-brief__temp"
              :class="weather.alerts.temp !== 'none' ? `alert-${weather.alerts.temp}` : undefined"
            >{{ weather.temp }}°C</span>
            <span v-if="weather.feelsLike != null" class="weather-brief__feels">
              体感 {{ weather.feelsLike }}°C
            </span>
          </div>
        </div>
      </div>

      <div class="weather-brief__gauges">
        <div
          v-for="item in gauges"
          :key="item.key"
          class="gauge"
          :class="item.toneClass"
          :title="item.tip"
        >
          <a-progress
            type="circle"
            :percent="item.percent"
            :stroke-width="5"
            :width="40"
            :color="item.color"
            track-color="var(--color-fill-3)"
          >
            <template #text>
              <span class="gauge__value">{{ item.value }}</span>
            </template>
          </a-progress>
          <span class="gauge__name">{{ item.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DashboardWeather, WeatherAlertLevel } from '../../utils/weather'

defineOptions({ name: 'WeatherBrief' })

const props = defineProps<{
  weather: DashboardWeather | null
  loading?: boolean
  title?: string
}>()

const ALERT_COLOR: Record<Exclude<WeatherAlertLevel, 'none'>, string> = {
  blue: '#165dff',
  yellow: '#ff7d00',
  orange: '#f77234',
  red: '#f53f3f',
}

const ALERT_RANK: Record<WeatherAlertLevel, number> = {
  none: 0,
  blue: 1,
  yellow: 2,
  orange: 3,
  red: 4,
}

const PHENOMENON_NAME: Record<string, string> = {
  storm: '暴雨',
  thunder: '雷电',
  hail: '冰雹',
  snow: '暴雪',
  fog: '大雾',
  sand: '沙尘',
}

interface GaugeItem {
  key: string
  name: string
  value: string
  percent: number
  color: string
  toneClass?: string
  tip?: string
}

const toneOf = (level?: WeatherAlertLevel) =>
  level && level !== 'none' ? `tone-${level}` : undefined

const colorOf = (level: WeatherAlertLevel | undefined, fallback: string) =>
  level && level !== 'none' ? ALERT_COLOR[level] : fallback

const aqiValue = computed(() => {
  const w = props.weather
  if (!w) return '--'
  if (w.aqi != null) return String(w.aqi)
  const cat = (w.aqiCategory || '').trim()
  if (!cat || cat === '--') return '--'
  if (cat.includes('严重')) return '严重'
  if (cat.includes('重度')) return '重度'
  if (cat.includes('中度')) return '中度'
  if (cat.includes('轻度')) return '轻度'
  return cat.slice(0, 2)
})

const aqiPercent = computed(() => {
  const aqi = props.weather?.aqi
  if (aqi != null && !Number.isNaN(Number(aqi))) {
    return Math.min(1, Math.max(0.08, Number(aqi) / 300))
  }
  const cat = (props.weather?.aqiCategory || '').trim()
  if (cat.includes('严重')) return 1
  if (cat.includes('重度')) return 0.85
  if (cat.includes('中度')) return 0.65
  if (cat.includes('轻度')) return 0.45
  if (cat === '良') return 0.28
  if (cat === '优') return 0.12
  return 0.2
})

const weatherAlertLevel = computed<WeatherAlertLevel>(() => props.weather?.alerts.weather || 'none')

const weatherAlertShort = computed(() => {
  if (!props.weather) return '--'
  if (weatherAlertLevel.value === 'none') return '无'
  const map: Record<Exclude<WeatherAlertLevel, 'none'>, string> = {
    blue: '蓝',
    yellow: '黄',
    orange: '橙',
    red: '红',
  }
  return map[weatherAlertLevel.value]
})

const weatherAlertName = computed(() => {
  if (!props.weather) return '预警'
  if (weatherAlertLevel.value === 'none') return '预警'
  return PHENOMENON_NAME[props.weather.kind] || props.weather.label || '预警'
})

const formatPrecip = (v: number | null | undefined) => {
  if (v == null || Number.isNaN(Number(v))) return '--'
  const n = Math.round(Number(v) * 10) / 10
  return Number.isInteger(n) ? String(n) : String(n)
}

const gauges = computed<GaugeItem[]>(() => {
  const w = props.weather
  if (!w) return []

  const aqiTip = w.aqi != null
    ? `空气质量 ${w.aqiCategory}（AQI ${w.aqi}）`
    : `空气质量 ${w.aqiCategory}`

  const windTip = w.windDir
    ? `风力 ${w.windLevel}级 · ${w.windDir}`
    : `风力 ${w.windLevel}级`

  const alertTip = weatherAlertLevel.value === 'none'
    ? '暂无天气现象预警'
    : `${weatherAlertName.value}${ALERT_COLOR[weatherAlertLevel.value as Exclude<WeatherAlertLevel, 'none'>]}色预警`

  const humidity = w.humidity
  const precip = w.precip
  const pressure = w.pressure

  return [
    {
      key: 'aqi',
      name: '空气',
      value: aqiValue.value,
      percent: aqiPercent.value,
      color: colorOf(w.alerts.aqi, (w.aqiCategory === '优' || w.aqiCategory === '良') ? '#00b42a' : '#165dff'),
      toneClass: toneOf(w.alerts.aqi),
      tip: aqiTip,
    },
    {
      key: 'wind',
      name: '风力',
      value: `${w.windLevel}级`,
      percent: Math.min(1, Math.max(0.06, w.windLevel / 12)),
      color: colorOf(w.alerts.wind, '#165dff'),
      toneClass: toneOf(w.alerts.wind),
      tip: windTip,
    },
    {
      key: 'alert',
      name: weatherAlertName.value,
      value: weatherAlertShort.value,
      percent: weatherAlertLevel.value === 'none'
        ? 0.12
        : Math.min(1, 0.2 + ALERT_RANK[weatherAlertLevel.value] * 0.2),
      color: colorOf(weatherAlertLevel.value, 'var(--color-text-4)'),
      toneClass: toneOf(weatherAlertLevel.value),
      tip: alertTip,
    },
    {
      key: 'humidity',
      name: '湿度',
      value: humidity == null ? '--' : `${humidity}%`,
      percent: humidity == null ? 0.08 : Math.min(1, Math.max(0.08, humidity / 100)),
      color: '#14c9c9',
      tip: humidity == null ? '湿度暂无' : `相对湿度 ${humidity}%`,
    },
    {
      key: 'precip',
      name: '降水',
      value: precip == null ? '--' : formatPrecip(precip),
      percent: precip == null ? 0.08 : Math.min(1, Math.max(0.08, Number(precip) / 20)),
      color: '#165dff',
      tip: precip == null ? '降水暂无' : `降水量 ${formatPrecip(precip)} mm`,
    },
    {
      key: 'pressure',
      name: '气压',
      value: pressure == null ? '--' : String(pressure),
      percent: pressure == null ? 0.08 : Math.min(1, Math.max(0.08, (pressure - 960) / 80)),
      color: '#722ed1',
      tip: pressure == null ? '气压暂无' : `大气压 ${pressure} 百帕`,
    },
  ]
})
</script>

<style scoped lang="scss">
.weather-brief {
  min-height: 68px;
  display: flex;
  align-items: center;
  padding: 0 4px;
  box-sizing: border-box;
}

.weather-brief__empty {
  width: 100%;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
  font-size: 14px;
}

.weather-brief__body {
  display: flex;
  align-items: center;
  column-gap: 12px;
  min-width: 0;
  width: 100%;
}

.weather-brief__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.weather-brief__icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.weather-brief__summary {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  line-height: 1.25;
}

.weather-brief__title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.weather-brief__city {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-1);
}

.weather-brief__label {
  font-size: 13px;
  color: var(--color-text-2);
}

.weather-brief__temps {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.weather-brief__temp {
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-1);
}

.weather-brief__feels {
  font-size: 12px;
  color: var(--color-text-3);
}

.weather-brief__gauges {
  display: flex;
  align-items: center;
  justify-content: space-around;
  align-self: center;
  flex: 1 1 220px;
  min-width: 0;
  gap: 4px;
}

.gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1 1 0;
  min-width: 44px;
}

.gauge__value {
  font-size: 10px;
  font-weight: 600;
  line-height: 1.1;
  color: var(--color-text-1);
  white-space: nowrap;
}

.gauge__name {
  font-size: 11px;
  color: var(--color-text-3);
  line-height: 1.2;
  white-space: nowrap;
}

.gauge.tone-blue .gauge__name,
.gauge.tone-blue .gauge__value {
  color: #165dff;
}

.gauge.tone-yellow .gauge__name,
.gauge.tone-yellow .gauge__value {
  color: #ff7d00;
}

.gauge.tone-orange .gauge__name,
.gauge.tone-orange .gauge__value {
  color: #f77234;
}

.gauge.tone-red .gauge__name,
.gauge.tone-red .gauge__value {
  color: #f53f3f;
}

.alert-blue {
  color: #165dff;
}

.alert-yellow {
  color: #ff7d00;
}

.alert-orange {
  color: #f77234;
}

.alert-red {
  color: #f53f3f;
}

:deep(.arco-progress-circle) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.arco-progress-circle-text) {
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

@media (max-width: 575px) {
  .weather-brief {
    margin: 8px 0;
    padding: 0;
  }
}
</style>
