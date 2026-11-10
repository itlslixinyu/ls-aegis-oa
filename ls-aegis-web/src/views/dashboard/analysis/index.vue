<template>
  <div
    ref="screenRef"
    class="ops-screen"
    :class="{ 'is-fullscreen': isFullscreen, 'is-plain': !showDecor }"
  >
    <template v-if="showDecor">
      <div class="ops-screen__frame frame-tl" aria-hidden="true" />
      <div class="ops-screen__frame frame-tr" aria-hidden="true" />
      <div class="ops-screen__frame frame-bl" aria-hidden="true" />
      <div class="ops-screen__frame frame-br" aria-hidden="true" />
      <div class="ops-screen__frame frame-t" aria-hidden="true" />
      <div class="ops-screen__frame frame-b" aria-hidden="true" />
      <div class="ops-screen__scan" aria-hidden="true" />
    </template>

    <header class="ops-screen__header anim-fade-down">
      <div
        v-if="weatherEnabled && weather"
        class="ops-screen__weather"
        :title="weatherTitle"
      >
        <span class="ops-screen__weather-city">{{ weather.city }}</span>
        <span class="ops-screen__weather-pipe">|</span>
        <span
          class="ops-screen__weather-condition"
          :class="weather.alerts.weather !== 'none' ? `alert-${weather.alerts.weather}` : undefined"
        >
          <img
            class="ops-screen__weather-icon-img"
            :src="weather.iconSrc"
            :alt="weather.label"
            width="20"
            height="20"
          >
          <span class="ops-screen__weather-label">{{ weather.label }}</span>
        </span>
        <span class="ops-screen__weather-pipe">|</span>
        <span
          class="ops-screen__weather-temp"
          :class="weather.alerts.temp !== 'none' ? `alert-${weather.alerts.temp}` : undefined"
        >温度{{ weather.temp }}°C</span>
        <span class="ops-screen__weather-pipe">|</span>
        <span
          class="ops-screen__weather-metric"
          :class="weather.alerts.aqi !== 'none' ? `alert-${weather.alerts.aqi}` : undefined"
        >空气质量 {{ weather.aqiCategory }}</span>
        <span class="ops-screen__weather-pipe">|</span>
        <span
          class="ops-screen__weather-metric"
          :class="weather.alerts.wind !== 'none' ? `alert-${weather.alerts.wind}` : undefined"
        >风力 {{ weather.windLevel }}级</span>
      </div>
      <div v-else class="ops-screen__weather ops-screen__weather--empty" aria-hidden="true" />
      <div class="ops-screen__title-wrap">
        <span class="ops-screen__title-dot">·</span>
        <h1 class="ops-screen__title">{{ screenTitle }}</h1>
        <span class="ops-screen__title-dot">·</span>
      </div>
      <div v-if="showClock" class="ops-screen__clock">
        <span class="current-time-glass">
          <span class="current-time">{{ nowParts.date }}</span>
          <span class="ops-screen__weather-pipe">|</span>
          <span class="current-time">{{ nowParts.time }}</span>
          <span class="ops-screen__weather-pipe">|</span>
          <span class="current-time">{{ nowParts.week }}</span>
        </span>
      </div>
      <div v-else class="ops-screen__clock ops-screen__clock--empty" aria-hidden="true" />
    </header>

    <div class="ops-screen__fs-dock" :class="{ 'is-tipping': showFsTip }">
      <div v-if="showFsTip" class="ops-screen__fs-tip" role="status">
        <span class="ops-screen__fs-tip-title">提示</span>
        <span class="ops-screen__fs-tip-text">
          <span>将鼠标移至</span>
          <span>右下角可显</span>
          <span>示全屏按钮</span>
        </span>
        <span class="fs-tip-decor fs-tip-tl" />
        <span class="fs-tip-decor fs-tip-tr" />
        <span class="fs-tip-decor fs-tip-bl" />
        <span class="fs-tip-decor fs-tip-br" />
      </div>
      <button
        type="button"
        class="ops-screen__fs-fab"
        :title="isFullscreen ? '退出全屏' : '全屏展示'"
        :aria-label="isFullscreen ? '退出全屏' : '全屏展示'"
        @click="toggle"
      >
        <icon-fullscreen-exit v-if="isFullscreen" :size="18" />
        <icon-fullscreen v-else :size="18" />
      </button>
    </div>

    <div class="ops-screen__toolbar anim-fade-down" style="--delay: 0.08s">
      <div class="toolbar-left">
        <span class="label">工单趋势</span>
        <a-radio-group v-model="trendDays" type="button" size="small">
          <a-radio :value="7">近7天</a-radio>
          <a-radio :value="30">近30天</a-radio>
        </a-radio-group>
      </div>
      <div class="toolbar-right">
        <div v-if="showStatus" class="ops-screen__status ops-screen__status--inline">
          <span class="dot" :class="`is-${runStatus}`" :title="runStatusLabel" />
          <span>{{ runStatusLabel }}</span>
        </div>
        <span class="updated">数据更新于：<span class="update-time">{{ updatedAt }}</span></span>
      </div>
    </div>

    <section v-if="showKpi" class="ops-screen__kpi">
      <div
        v-for="(item, idx) in kpiList"
        :key="item.key"
        class="kpi-card anim-rise"
        :style="{ '--delay': `${0.05 + idx * 0.08}s` }"
      >
        <div class="kpi-card__icon" :style="{ background: item.iconBg, color: item.iconColor }">
          <icon-tool v-if="item.key === 'repair'" :size="18" />
          <icon-storage v-else-if="item.key === 'fund'" :size="18" />
          <icon-trophy v-else-if="item.key === 'biz'" :size="18" />
          <icon-file v-else :size="18" />
        </div>
        <div class="kpi-card__main">
          <div class="kpi-card__title">{{ item.title }}</div>
          <div class="kpi-card__line" />
          <div class="kpi-card__metrics">
            <div v-for="m in item.metrics" :key="m.label" class="metric">
              <div class="metric__value">
                <a-statistic
                  :key="`${item.key}-${m.label}`"
                  :value="m.value"
                  :value-from="0"
                  :precision="m.precision ?? 0"
                  :value-style="{ color: item.valueColor, fontSize: '16px', fontWeight: 700 }"
                  animation
                  show-group-separator
                >
                  <template v-if="m.prefix" #prefix>{{ m.prefix }}</template>
                  <template v-if="m.suffix" #suffix>{{ m.suffix }}</template>
                </a-statistic>
              </div>
              <div class="metric__label">{{ m.label }}</div>
            </div>
          </div>
        </div>
        <span class="kc-decor kc-tl" />
        <span class="kc-decor kc-tr" />
        <span class="kc-decor kc-bl" />
        <span class="kc-decor kc-br" />
      </div>
    </section>

    <!-- 3×4：运维 / 进销存 / 签章报价 分域铺开 -->
    <section class="ops-screen__charts">
      <!-- 第 1 行：运维 EOMS -->
      <ScreenPanel title="工单每日趋势" enter-delay="0.28s" :connectors="false">
        <Chart :option="orderTrendOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="报修工单状态" enter-delay="0.32s" :connectors="false">
        <Chart :option="repairStatusOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="近月收支" enter-delay="0.36s" :connectors="false">
        <Chart :option="profitTrendOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="运维规模" enter-delay="0.4s" :connectors="false">
        <Chart :option="eomsScaleOption" height="100%" />
      </ScreenPanel>
      <!-- 第 2 行：进销存 PSI -->
      <ScreenPanel title="本月进销" enter-delay="0.44s" :connectors="false">
        <Chart :option="monthTradeOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="近30天出入库" enter-delay="0.48s" :connectors="false">
        <Chart :option="stockFlowOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="资金敞口" enter-delay="0.52s" :connectors="false">
        <Chart :option="fundOpenOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="待办预警" enter-delay="0.56s" :connectors="false">
        <Chart :option="todoAlertOption" height="100%" />
      </ScreenPanel>
      <!-- 第 3 行：签章 / 报价 / 回款 -->
      <ScreenPanel title="签章概况" enter-delay="0.6s" :connectors="false">
        <Chart :option="signOverviewOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="签章项目状态" enter-delay="0.64s" :connectors="false">
        <Chart :option="signStatusOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="报价概况" enter-delay="0.68s" :connectors="false">
        <Chart :option="quoteOverviewOption" height="100%" />
      </ScreenPanel>
      <ScreenPanel title="待确认回款" enter-delay="0.72s" :connectors="false">
        <Chart :option="receiptPendingOption" height="100%" />
      </ScreenPanel>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useDocumentVisibility, useFullscreen } from '@vueuse/core'
import dayjs from 'dayjs'
import type { EChartsOption } from 'echarts'
import {
  getOrderTrend,
  getOverview,
  getProfitTrend,
} from '@/apis/eoms'
import { getPsiWorkbenchSummary, type PsiWorkbenchSummary } from '@/apis/psi'
import { getQuoteSummary, type QuoteSummary } from '@/apis/quote'
import { getDashboardStats, type DashboardStats } from '@/apis/sign/dashboard'
import { listDashboardOptionDict } from '@/apis/system'
import Chart from '@/components/Chart/index.vue'
import { assignIfChanged, patchListByKey } from '@/utils/listDiff'
import has from '@/utils/has'
import ScreenPanel from '../components/ScreenPanel.vue'
import { useDashboardWeather } from '../composables/useDashboardWeather'

defineOptions({ name: 'Analysis' })

const DEFAULT_SCREEN_TITLE = 'LS-Aegis 雷铄御警安全应用运营中枢'

const screenRef = ref<HTMLElement | null>(null)
const { isFullscreen, enter: enterFullscreen, toggle } = useFullscreen(screenRef)
const showFsTip = ref(false)
let fsTipTimer: ReturnType<typeof setTimeout> | undefined
let fsHotkeyBound = false
let dashboardRefreshTimer: ReturnType<typeof setInterval> | undefined
let pageActive = true
let initialized = false
let refreshing = false
const visibility = useDocumentVisibility()

/** 大屏展示配置（系统配置 · 大屏配置） */
const screenTitle = ref(DEFAULT_SCREEN_TITLE)
const showClock = ref(true)
const showStatus = ref(true)
const showKpi = ref(true)
const showFsTipEnabled = ref(true)
const showDecor = ref(true)
const dashboardRefreshInterval = ref(60)

const trendDays = ref<7 | 30>(7)
const WEEK_LABELS = ['日', '一', '二', '三', '四', '五', '六'] as const
const formatNowParts = (d = dayjs()) => ({
  date: d.format('YYYY-MM-DD'),
  time: d.format('HH:mm:ss'),
  week: `星期${WEEK_LABELS[d.day()]}`,
})

const nowParts = ref(formatNowParts())
const updatedAt = ref(dayjs().format('HH:mm:ss'))
const loading = ref(false)
/** 系统运行指示灯：正常 / 告警 / 异常 / 离线 / 天气获取失败 */
type RunStatus = 'running' | 'warning' | 'error' | 'offline' | 'weather'
const runStatus = ref<RunStatus>('running')
const systemOnline = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
const dashboardOk = ref(true)
const lastCpu = ref(30)
const lastMem = ref(50)

let applyRunStatus: () => void = () => {}

const {
  weatherRuntime,
  weatherEnabled,
  weather,
  weatherTitle,
  weatherNetOk,
  loadWeatherConfig,
  refreshWeather,
  markWeatherOffline,
} = useDashboardWeather({
  autoLoad: false,
  onNetStatusChange: () => applyRunStatus(),
})

applyRunStatus = () => {
  if (!systemOnline.value || !dashboardOk.value) {
    runStatus.value = 'offline'
    return
  }
  const cpu = lastCpu.value
  const mem = lastMem.value
  if (cpu >= 90 || mem >= 95) {
    runStatus.value = 'error'
    return
  }
  if (cpu >= 70 || mem >= 80) {
    runStatus.value = 'warning'
    return
  }
  if (weatherRuntime.value.enabled && !weatherNetOk.value) {
    runStatus.value = 'weather'
    return
  }
  runStatus.value = 'running'
}

const runStatusLabel = computed(() => {
  const map: Record<RunStatus, string> = {
    running: '系统运行中',
    warning: '系统告警中',
    error: '系统异常',
    offline: '系统离线',
    weather: '天气获取失败',
  }
  return map[runStatus.value]
})

const onBrowserOnline = () => {
  systemOnline.value = true
  applyRunStatus()
  void refreshWeather()
}

const onBrowserOffline = () => {
  systemOnline.value = false
  markWeatherOffline()
  applyRunStatus()
}

/** —— 业务 KPI —— */
const eomsOverview = ref<Record<string, number>>({})
const psiSummary = ref<Partial<PsiWorkbenchSummary>>({})
const signStats = ref<Partial<DashboardStats>>({})
const quoteSummary = ref<Partial<QuoteSummary>>({})

/** —— 图表序列 —— */
type DayCount = { day: string, count: number }
type MonthProfit = { month: string, income: number, cost: number, profit: number }
type NamedValue = { name: string, value: number }

const orderTrend = ref<DayCount[]>([])
const profitTrend = ref<MonthProfit[]>([])
const signStatusList = ref<NamedValue[]>([])

let clockTimer: ReturnType<typeof setInterval> | undefined

const axisText = 'rgba(180, 210, 240, 0.65)'
const splitLine = 'rgba(64, 169, 255, 0.12)'
const chartColors = ['#3fd0ff', '#1677ff', '#36cfc9', '#73d13d', '#ff9f43', '#9254de', '#f759ab', '#597ef7']

const num = (v: unknown) => {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n : 0
}

const normalizeList = (raw: unknown): Record<string, unknown>[] => {
  if (Array.isArray(raw)) {
    return raw as Record<string, unknown>[]
  }
  if (raw && typeof raw === 'object') {
    const o = raw as Record<string, unknown>
    if (Array.isArray(o.list)) {
      return o.list as Record<string, unknown>[]
    }
    if (Array.isArray(o.rows)) {
      return o.rows as Record<string, unknown>[]
    }
  }
  return []
}

const settledData = <T>(r: PromiseSettledResult<{ data?: unknown }>): T | null => {
  if (r.status === 'fulfilled') {
    return (r.value?.data ?? null) as T | null
  }
  return null
}

const loadDashboardConfig = async () => {
  try {
    const { data } = await listDashboardOptionDict()
    const map = Object.fromEntries((data || []).map((i) => [i.label, i.value]))
    const title = String(map.DASHBOARD_TITLE || '').trim()
    screenTitle.value = title || DEFAULT_SCREEN_TITLE
    showClock.value = String(map.DASHBOARD_SHOW_CLOCK ?? '1') !== '0'
    showStatus.value = String(map.DASHBOARD_SHOW_STATUS ?? '1') !== '0'
    showKpi.value = String(map.DASHBOARD_SHOW_KPI ?? '1') !== '0'
    showFsTipEnabled.value = String(map.DASHBOARD_SHOW_FS_TIP ?? '1') !== '0'
    showDecor.value = String(map.DASHBOARD_SHOW_DECOR ?? '1') !== '0'
    const days = Number(map.DASHBOARD_DEFAULT_DAYS || 7)
    trendDays.value = days === 30 ? 30 : 7
    const interval = Number(map.DASHBOARD_REFRESH_INTERVAL)
    dashboardRefreshInterval.value = Number.isFinite(interval) ? Math.max(0, interval) : 60
  } catch {
    screenTitle.value = DEFAULT_SCREEN_TITLE
    showClock.value = true
    showStatus.value = true
    showKpi.value = true
    showFsTipEnabled.value = true
    showDecor.value = true
    trendDays.value = 7
    dashboardRefreshInterval.value = 60
  }
}

interface KpiMetric { label: string, value: number, precision?: number, prefix?: string, suffix?: string }
interface KpiCard { key: string, title: string, iconBg: string, iconColor: string, valueColor: string, metrics: KpiMetric[] }
const kpiList = computed<KpiCard[]>(() => {
  const e = eomsOverview.value
  const p = psiSummary.value
  const s = signStats.value
  const q = quoteSummary.value
  return [
    {
      key: 'repair',
      title: '报修运维',
      iconBg: 'linear-gradient(135deg, #c7445c, #ff4d6d)',
      iconColor: '#fff',
      valueColor: '#ff6b8a',
      metrics: [
        { label: '未处理', value: num(e.waitApply) },
        { label: '待接单', value: num(e.waitAccept) },
        { label: '进行中', value: num(e.processing) },
        { label: '待结算', value: num(e.waitSettlement) },
      ],
    },
    {
      key: 'fund',
      title: '进销资金',
      iconBg: 'linear-gradient(135deg, #0099cc, #00d4ff)',
      iconColor: '#fff',
      valueColor: '#00d4ff',
      metrics: [
        { label: '待确认回款', value: num(p.pendingReceiptCount) },
        { label: '业务待办', value: num(p.openTodoCount) },
        { label: '未结应收', value: num(p.openReceivableCount) },
        { label: '未结应付', value: num(p.openPayableCount) },
      ],
    },
    {
      key: 'biz',
      title: '本月经营',
      iconBg: 'linear-gradient(135deg, #1a8a6a, #3ad1a8)',
      iconColor: '#fff',
      valueColor: '#3ad1a8',
      metrics: [
        { label: '本月工单', value: num(e.thisMonthOrder) },
        { label: '本月收入', value: num(e.monthIncome), precision: 0, prefix: '¥' },
        { label: '本月销售', value: num(p.monthSaleAmount), precision: 0, prefix: '¥' },
        { label: '本月采购', value: num(p.monthPurchaseAmount), precision: 0, prefix: '¥' },
      ],
    },
    {
      key: 'sign',
      title: '签章报价',
      iconBg: 'linear-gradient(135deg, #2a6dd6, #409fff)',
      iconColor: '#fff',
      valueColor: '#409fff',
      metrics: [
        { label: '本月签章', value: num(s.monthSignCount) },
        { label: '印章数', value: num(s.sealCount) },
        { label: '本月报价', value: num(q.monthCount) },
        { label: '报价草稿', value: num(q.draftCount) },
      ],
    },
  ]
})

/** 通用纵向柱图 */
const makeBarOption = (names: string[], values: number[], seriesName: string, color = '#3fd0ff'): EChartsOption => ({
  animationDuration: 900,
  grid: { left: 36, right: 10, top: 24, bottom: 28, containLabel: true },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: names.length ? names : ['暂无数据'],
    axisLabel: { color: axisText, hideOverlap: true, fontSize: 10 },
    axisLine: { lineStyle: { color: splitLine } },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLabel: { color: axisText, fontSize: 10 },
    splitLine: { lineStyle: { color: splitLine, type: 'dashed' } },
  },
  series: [
    {
      name: seriesName,
      type: 'bar',
      data: values.length ? values : [0],
      barWidth: 14,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color },
            { offset: 1, color: `${color}55` },
          ],
        },
      },
    },
  ],
})

/** 平滑波浪折线（含面积） */
const makeWaveOption = (
  names: string[],
  values: number[],
  seriesName: string,
  color = '#3fd0ff',
): EChartsOption => ({
  animationDuration: 900,
  grid: { left: 36, right: 12, top: 24, bottom: 28, containLabel: true },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: names.length ? names : ['暂无数据'],
    axisLabel: { color: axisText, hideOverlap: true, fontSize: 10 },
    axisLine: { lineStyle: { color: splitLine } },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLabel: { color: axisText, fontSize: 10 },
    splitLine: { lineStyle: { color: splitLine, type: 'dashed' } },
  },
  series: [
    {
      name: seriesName,
      type: 'line',
      smooth: 0.45,
      symbol: 'circle',
      symbolSize: 7,
      showSymbol: true,
      data: values.length ? values : [0],
      lineStyle: { width: 2.5, color },
      itemStyle: { color, borderColor: '#fff', borderWidth: 1 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: `${color}66` },
            { offset: 1, color: `${color}00` },
          ],
        },
      },
    },
  ],
})

/** 多条波浪折线 */
const makeMultiWaveOption = (
  categories: string[],
  series: Array<{ name: string, data: number[], color: string, area?: boolean }>,
): EChartsOption => ({
  animationDuration: 900,
  grid: { left: 40, right: 10, top: 28, bottom: 28, containLabel: true },
  tooltip: { trigger: 'axis' },
  legend: {
    top: 0,
    textStyle: { color: axisText, fontSize: 10 },
    itemWidth: 10,
    itemHeight: 8,
    data: series.map((s) => s.name),
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: categories.length ? categories : ['暂无数据'],
    axisLabel: { color: axisText, hideOverlap: true, fontSize: 10 },
    axisLine: { lineStyle: { color: splitLine } },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: axisText, fontSize: 10 },
    splitLine: { lineStyle: { color: splitLine, type: 'dashed' } },
  },
  series: series.map((s) => ({
    name: s.name,
    type: 'line' as const,
    smooth: 0.45,
    symbol: 'circle',
    symbolSize: 6,
    showSymbol: true,
    data: s.data,
    lineStyle: { width: 2.2, color: s.color },
    itemStyle: { color: s.color },
    ...(s.area
      ? {
          areaStyle: {
            color: {
              type: 'linear' as const,
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: `${s.color}44` },
                { offset: 1, color: `${s.color}00` },
              ],
            },
          },
        }
      : {}),
  })),
})

/** 分组柱图 */
const makeGroupBarOption = (
  categories: string[],
  series: Array<{ name: string, data: number[], color: string }>,
): EChartsOption => ({
  animationDuration: 900,
  grid: { left: 40, right: 10, top: 28, bottom: 28, containLabel: true },
  tooltip: { trigger: 'axis' },
  legend: {
    top: 0,
    textStyle: { color: axisText, fontSize: 10 },
    data: series.map((s) => s.name),
  },
  xAxis: {
    type: 'category',
    data: categories,
    axisLabel: { color: axisText, fontSize: 10 },
    axisLine: { lineStyle: { color: splitLine } },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: axisText, fontSize: 10 },
    splitLine: { lineStyle: { color: splitLine, type: 'dashed' } },
  },
  series: series.map((s) => ({
    name: s.name,
    type: 'bar' as const,
    data: s.data,
    barWidth: 10,
    itemStyle: { color: s.color, borderRadius: [3, 3, 0, 0] },
  })),
})

/** 横向柱图 */
const makeHBarOption = (names: string[], values: number[], seriesName: string, color = '#13c2c2'): EChartsOption => {
  const pairs = names.map((n, i) => ({ name: n, value: values[i] || 0 }))
  const sorted = [...pairs].reverse()
  return {
    animationDuration: 900,
    grid: { left: 8, right: 28, top: 12, bottom: 12, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: axisText, fontSize: 10 },
      splitLine: { lineStyle: { color: splitLine, type: 'dashed' } },
    },
    yAxis: {
      type: 'category',
      data: sorted.map((i) => i.name),
      axisLabel: { color: axisText, fontSize: 10 },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: splitLine } },
    },
    series: [
      {
        name: seriesName,
        type: 'bar',
        data: sorted.map((i) => i.value),
        barWidth: 10,
        itemStyle: {
          borderRadius: 4,
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color },
              { offset: 1, color: `${color}aa` },
            ],
          },
        },
      },
    ],
  }
}

const orderTrendOption = computed<EChartsOption>(() => {
  const dates = orderTrend.value.map((i) => i.day)
  const counts = orderTrend.value.map((i) => i.count)
  return makeWaveOption(
    dates.length ? dates : ['暂无数据'],
    counts.length ? counts : [0],
    '工单数',
    '#3fd0ff',
  )
})

const repairStatusOption = computed(() => {
  const e = eomsOverview.value
  return makeWaveOption(
    ['未处理', '待接单', '进行中', '待结算'],
    [num(e.waitApply), num(e.waitAccept), num(e.processing), num(e.waitSettlement)],
    '工单数',
    '#ff6b8a',
  )
})

const profitTrendOption = computed(() => {
  const months = profitTrend.value.map((i) => i.month)
  return makeMultiWaveOption(
    months.length ? months : ['暂无数据'],
    [
      {
        name: '收入',
        data: profitTrend.value.map((i) => i.income),
        color: '#3fd0ff',
        area: true,
      },
      {
        name: '成本',
        data: profitTrend.value.map((i) => i.cost),
        color: '#ff9f43',
        area: true,
      },
      {
        name: '利润',
        data: profitTrend.value.map((i) => i.profit),
        color: '#73d13d',
        area: true,
      },
    ],
  )
})

const eomsScaleOption = computed<EChartsOption>(() => {
  const e = eomsOverview.value
  const indicators = [
    { name: '客户数', max: Math.max(num(e.customerTotal) * 1.2, 10) },
    { name: '工单总量', max: Math.max(num(e.orderTotal) * 1.2, 10) },
    { name: '本月工单', max: Math.max(num(e.thisMonthOrder) * 1.2, 10) },
    { name: '本月收入', max: Math.max(num(e.monthIncome) * 1.2, 10) },
  ]
  return {
    animationDuration: 900,
    tooltip: {},
    radar: {
      indicator: indicators,
      center: ['50%', '52%'],
      radius: '58%',
      axisName: { color: axisText, fontSize: 10 },
      splitLine: { lineStyle: { color: splitLine } },
      splitArea: { areaStyle: { color: ['rgba(64,159,255,0.04)', 'rgba(64,159,255,0.01)'] } },
      axisLine: { lineStyle: { color: 'rgba(64,159,255,0.25)' } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            name: '运维规模',
            value: [num(e.customerTotal), num(e.orderTotal), num(e.thisMonthOrder), num(e.monthIncome)],
            lineStyle: { color: '#3ad1a8', width: 2 },
            itemStyle: { color: '#3ad1a8' },
            areaStyle: { color: 'rgba(58, 209, 168, 0.28)' },
          },
        ],
      },
    ],
  }
})

const monthTradeOption = computed(() => {
  const p = psiSummary.value
  return makeGroupBarOption(
    ['销售', '采购'],
    [
      { name: '金额', data: [num(p.monthSaleAmount), num(p.monthPurchaseAmount)], color: '#3fd0ff' },
      { name: '笔数', data: [num(p.monthSaleCount), num(p.monthPurchaseCount)], color: '#73d13d' },
    ],
  )
})

const stockFlowOption = computed(() => {
  const p = psiSummary.value
  // 用波浪对比金额/数量在出入库两端的走势感
  return makeMultiWaveOption(
    ['入库', '出库'],
    [
      {
        name: '金额',
        data: [num(p.last30InAmount), num(p.last30OutAmount)],
        color: '#36cfc9',
        area: true,
      },
      {
        name: '数量',
        data: [num(p.last30InQty), num(p.last30OutQty)],
        color: '#ff9f43',
        area: true,
      },
    ],
  )
})

const fundOpenOption = computed(() => {
  const p = psiSummary.value
  return makeHBarOption(
    ['应收金额', '应收笔数', '应付金额', '应付笔数'],
    [
      num(p.openReceivableAmount),
      num(p.openReceivableCount),
      num(p.openPayableAmount),
      num(p.openPayableCount),
    ],
    '资金',
    '#597ef7',
  )
})

const todoAlertOption = computed(() => {
  const e = eomsOverview.value
  const p = psiSummary.value
  return makeWaveOption(
    ['业务待办', '待确认回款', '库存预警', '报修未处理'],
    [
      num(p.openTodoCount),
      num(p.pendingReceiptCount),
      Array.isArray(p.stockAlerts) ? p.stockAlerts.length : num(e.stockAlert),
      num(e.waitApply),
    ],
    '待办数',
    '#ff9f43',
  )
})

const signOverviewOption = computed(() => {
  const s = signStats.value
  return makeWaveOption(
    ['项目', '印章', '本月签章', '用户'],
    [num(s.projectCount), num(s.sealCount), num(s.monthSignCount), num(s.userCount)],
    '数量',
    '#9254de',
  )
})

const signStatusOption = computed<EChartsOption>(() => {
  const data = signStatusList.value.length
    ? signStatusList.value.map((item, idx) => ({
        name: item.name,
        value: item.value,
        itemStyle: { color: chartColors[idx % chartColors.length] },
      }))
    : [{ name: '暂无数据', value: 1, itemStyle: { color: '#1f4b7a' } }]
  return {
    animationDuration: 900,
    tooltip: { trigger: 'item' },
    legend: {
      type: 'scroll',
      top: '70%',
      left: 'center',
      textStyle: { color: axisText, fontSize: 10 },
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 6,
      pageIconColor: '#7ad7ff',
      pageTextStyle: { color: axisText },
    },
    series: [
      {
        type: 'pie',
        roseType: 'area',
        radius: ['12%', '48%'],
        center: ['50%', '38%'],
        label: { color: '#e8f4ff', formatter: '{b}', fontSize: 10 },
        labelLine: { length: 6, length2: 4 },
        data,
      },
    ],
  }
})

const quoteOverviewOption = computed(() => {
  const q = quoteSummary.value
  const monthCount = num(q.monthCount)
  const monthAmount = num(q.monthAmount)
  const avg = monthCount > 0 ? Math.round(monthAmount / monthCount) : 0
  return makeWaveOption(
    ['草稿', '本月笔数', '本月金额', '笔均金额'],
    [num(q.draftCount), monthCount, monthAmount, avg],
    '数值',
    '#1677ff',
  )
})

const receiptPendingOption = computed(() => {
  const p = psiSummary.value
  return makeGroupBarOption(
    ['待确认回款', '未结应收'],
    [
      {
        name: '金额',
        data: [num(p.pendingReceiptAmount), num(p.openReceivableAmount)],
        color: '#00d4ff',
      },
      {
        name: '笔数',
        data: [num(p.pendingReceiptCount), num(p.openReceivableCount)],
        color: '#ff6b8a',
      },
    ],
  )
})

const SIGN_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '待签',
  SIGNING: '签署中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REJECTED: '已驳回',
  ARCHIVED: '已归档',
}

const mapOrderTrend = (raw: unknown): DayCount[] =>
  normalizeList(raw).map((i) => ({
    day: String(i.day ?? i.date ?? ''),
    count: num(i.count),
  })).filter((i) => i.day)

const mapProfitTrend = (raw: unknown): MonthProfit[] =>
  normalizeList(raw).map((i) => ({
    month: String(i.month ?? ''),
    income: num(i.income),
    cost: num(i.cost),
    profit: num(i.profit),
  })).filter((i) => i.month)

const mapStatusDict = (raw?: Record<string, number> | null): NamedValue[] => {
  if (!raw || typeof raw !== 'object') {
    return []
  }
  return Object.entries(raw).map(([key, value]) => ({
    name: SIGN_STATUS_LABEL[key] || key,
    value: num(value),
  })).filter((i) => i.value > 0)
}

let orderTrendRequestVersion = 0
const loadOrderTrend = async () => {
  const version = ++orderTrendRequestVersion
  const days = trendDays.value
  if (!has.hasPermOr(['eoms:statistics:list'])) {
    orderTrend.value.splice(0, orderTrend.value.length)
    return
  }
  try {
    const { data } = await getOrderTrend({ days })
    if (version !== orderTrendRequestVersion || days !== trendDays.value) return
    patchListByKey(
      orderTrend.value as unknown as Record<string, unknown>[],
      mapOrderTrend(data) as unknown as Record<string, unknown>[],
      'day',
    )
    updatedAt.value = dayjs().format('HH:mm:ss')
  } catch {
    /* 保留上次 */
  }
}

/** 轮询静默刷新：diff 局部写入；无变化不触达视图 */
const refreshAll = async (opts?: { silent?: boolean }) => {
  if (!pageActive || visibility.value === 'hidden' || refreshing) return
  refreshing = true
  const silent = opts?.silent ?? false
  if (!silent) {
    loading.value = true
  }
  try {
    const canEomsDash = has.hasPermOr(['eoms:dashboard:list'])
    const canEomsStats = has.hasPermOr(['eoms:statistics:list'])
    const canSign = has.hasPermOr(['sign:dashboard:stats', 'sign:dashboard:view'])
    const canQuote = has.hasPermOr(['quote:list', 'quote:query'])
    const orderVersion = ++orderTrendRequestVersion
    const orderDays = trendDays.value
    const requested = [canEomsDash, canEomsStats, canEomsStats, true, canSign, canQuote]

    const tasks: Array<Promise<unknown>> = [
      canEomsDash ? getOverview() : Promise.resolve({ data: null }),
      canEomsStats ? getOrderTrend({ days: orderDays }) : Promise.resolve({ data: [] }),
      canEomsStats ? getProfitTrend({ months: 6 }) : Promise.resolve({ data: [] }),
      getPsiWorkbenchSummary(),
      canSign ? getDashboardStats() : Promise.resolve({ data: null }),
      canQuote ? getQuoteSummary() : Promise.resolve({ data: null }),
    ]

    const results = await Promise.allSettled(tasks)
    const [overviewRes, orderRes, profitRes, psiRes, signRes, quoteRes]
      = results as PromiseSettledResult<{ data?: unknown }>[]

    let changed = false

    const overview = settledData<Record<string, unknown>>(overviewRes)
    if (overview) {
      const next: Record<string, number> = {
        waitApply: num(overview.waitApply),
        waitAccept: num(overview.waitAccept),
        processing: num(overview.processing),
        waitSettlement: num(overview.waitSettlement),
        thisMonthOrder: num(overview.thisMonthOrder),
        monthIncome: num(overview.monthIncome),
        stockAlert: num(overview.stockAlert),
        customerTotal: num(overview.customerTotal),
        orderTotal: num(overview.orderTotal),
      }
      changed = assignIfChanged(eomsOverview, next) || changed
    } else if (!canEomsDash) {
      changed = assignIfChanged(eomsOverview, {}) || changed
    }

    if (orderVersion === orderTrendRequestVersion && orderDays === trendDays.value && orderRes.status === 'fulfilled') {
      changed = patchListByKey(
        orderTrend.value as unknown as Record<string, unknown>[],
        mapOrderTrend(settledData(orderRes)) as unknown as Record<string, unknown>[],
        'day',
      ) || changed
    }
    if (profitRes.status === 'fulfilled') {
      changed = patchListByKey(
        profitTrend.value as unknown as Record<string, unknown>[],
        mapProfitTrend(settledData(profitRes)) as unknown as Record<string, unknown>[],
        'month',
      ) || changed
    }

    const psi = settledData<Partial<PsiWorkbenchSummary>>(psiRes)
    if (psi) {
      changed = assignIfChanged(psiSummary, {
        pendingReceiptCount: num(psi.pendingReceiptCount),
        pendingReceiptAmount: num(psi.pendingReceiptAmount),
        openTodoCount: num(psi.openTodoCount),
        openReceivableCount: num(psi.openReceivableCount),
        openReceivableAmount: num(psi.openReceivableAmount),
        openPayableCount: num(psi.openPayableCount),
        openPayableAmount: num(psi.openPayableAmount),
        monthSaleAmount: num(psi.monthSaleAmount),
        monthSaleCount: num(psi.monthSaleCount),
        monthPurchaseAmount: num(psi.monthPurchaseAmount),
        monthPurchaseCount: num(psi.monthPurchaseCount),
        last30InAmount: num(psi.last30InAmount),
        last30InQty: num(psi.last30InQty),
        last30OutAmount: num(psi.last30OutAmount),
        last30OutQty: num(psi.last30OutQty),
        stockAlerts: psi.stockAlerts || [],
      }) || changed
    }

    const sign = settledData<Partial<DashboardStats>>(signRes)
    if (sign) {
      changed = assignIfChanged(signStats, {
        monthSignCount: num(sign.monthSignCount),
        sealCount: num(sign.sealCount),
        projectCount: num(sign.projectCount),
        userCount: num(sign.userCount),
      }) || changed
      changed = patchListByKey(
        signStatusList.value as unknown as Record<string, unknown>[],
        mapStatusDict(sign.projectStatusStats) as unknown as Record<string, unknown>[],
        'name',
      ) || changed
    } else if (!canSign) {
      changed = assignIfChanged(signStats, {}) || changed
      changed = patchListByKey(
        signStatusList.value as unknown as Record<string, unknown>[],
        [],
        'name',
      ) || changed
    }

    const quote = settledData<Partial<QuoteSummary>>(quoteRes)
    if (quote) {
      changed = assignIfChanged(quoteSummary, {
        draftCount: num(quote.draftCount),
        monthCount: num(quote.monthCount),
        monthAmount: num(quote.monthAmount),
      }) || changed
    } else if (!canQuote) {
      changed = assignIfChanged(quoteSummary, {}) || changed
    }

    if (!silent || changed) {
      lastCpu.value = 20 + Math.floor(Math.random() * 25)
      lastMem.value = 40 + Math.floor(Math.random() * 30)
    }

    dashboardOk.value = results.some((r, index) => requested[index] && r.status === 'fulfilled')
    systemOnline.value = navigator.onLine
    if (!silent || changed) {
      updatedAt.value = dayjs().format('HH:mm:ss')
    }
    applyRunStatus()
  } catch {
    dashboardOk.value = false
    applyRunStatus()
  } finally {
    refreshing = false
    if (!silent) {
      loading.value = false
    }
  }
}

const restartDashboardRefreshTimer = () => {
  if (dashboardRefreshTimer) clearInterval(dashboardRefreshTimer)
  dashboardRefreshTimer = undefined
  const sec = dashboardRefreshInterval.value
  if (!initialized || !pageActive || visibility.value === 'hidden' || !sec || sec <= 0) return
  dashboardRefreshTimer = setInterval(() => {
    void refreshAll({ silent: true })
  }, Math.max(10, sec) * 1000)
}

onMounted(async () => {
  window.addEventListener('online', onBrowserOnline)
  window.addEventListener('offline', onBrowserOffline)
  systemOnline.value = navigator.onLine
  // 天气可能等待定位/外网；不阻塞平台各业务域 KPI 首次取数。
  void loadWeatherConfig()
  await loadDashboardConfig()
  initialized = true
  if (!pageActive) return
  await refreshAll()
  if (!pageActive) return
  resumeTimers()
  tipFullscreenDock()
  bindFsHotkey()
})

onActivated(() => {
  pageActive = true
  if (initialized) {
    resumeTimers()
    void refreshAll({ silent: true })
  }
  tipFullscreenDock()
  bindFsHotkey()
})

onDeactivated(() => {
  pageActive = false
  stopTimers()
  unbindFsHotkey()
})

function stopTimers() {
  if (clockTimer) clearInterval(clockTimer)
  if (dashboardRefreshTimer) clearInterval(dashboardRefreshTimer)
  clockTimer = undefined
  dashboardRefreshTimer = undefined
}

function resumeTimers() {
  stopTimers()
  if (!pageActive || visibility.value === 'hidden') return
  nowParts.value = formatNowParts()
  clockTimer = setInterval(() => { nowParts.value = formatNowParts() }, 1000)
  restartDashboardRefreshTimer()
}

watch(visibility, (state) => {
  if (state === 'hidden') stopTimers()
  else if (pageActive && initialized) {
    resumeTimers()
    void refreshAll({ silent: true })
  }
})

function tipFullscreenDock() {
  if (!showFsTipEnabled.value) {
    showFsTip.value = false
    return
  }
  showFsTip.value = true
  if (fsTipTimer) clearTimeout(fsTipTimer)
  fsTipTimer = setTimeout(() => {
    showFsTip.value = false
  }, 5000)
}

function onFsEnterKey(e: KeyboardEvent) {
  if (e.key !== 'Enter' && e.code !== 'NumpadEnter') return
  const el = e.target as HTMLElement | null
  if (el) {
    const tag = el.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable) return
  }
  if (isFullscreen.value) return
  e.preventDefault()
  void enterFullscreen()
}

function bindFsHotkey() {
  if (fsHotkeyBound) return
  window.addEventListener('keydown', onFsEnterKey)
  fsHotkeyBound = true
}

function unbindFsHotkey() {
  if (!fsHotkeyBound) return
  window.removeEventListener('keydown', onFsEnterKey)
  fsHotkeyBound = false
}

watch(trendDays, () => {
  void loadOrderTrend()
})

onBeforeUnmount(() => {
  pageActive = false
  window.removeEventListener('online', onBrowserOnline)
  window.removeEventListener('offline', onBrowserOffline)
  stopTimers()
  if (fsTipTimer) clearTimeout(fsTipTimer)
  unbindFsHotkey()
})
</script>

<style scoped lang="scss">
@import '../styles/ops-screen.scss';

/* 强制模块间距可见：3×4 略收但仍清晰分隔 */
.ops-screen {
  --ops-gap: 14px;
  gap: 14px;
  padding: 20px 22px;
}

.ops-screen__kpi,
.ops-screen__charts {
  gap: 14px;
  row-gap: 14px;
  column-gap: 14px;
}

.ops-screen__charts {
  :deep(.screen-panel) {
    width: 100%;
    height: 100%;
    min-width: 0;
    min-height: 0;
    margin: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
}

.ops-screen__kpi {
  :deep(.kpi-card) {
    margin: 0;
    box-sizing: border-box;
  }
}

.metric__value {
  :deep(.arco-statistic-content) {
    line-height: 1.2;
  }

  :deep(.arco-statistic-suffix),
  :deep(.arco-statistic-prefix) {
    font-size: 12px;
    margin-left: 1px;
  }

  :deep(.arco-statistic-prefix) {
    margin-right: 1px;
    margin-left: 0;
  }
}
</style>
