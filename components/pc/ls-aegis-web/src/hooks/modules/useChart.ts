import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import { useAppStore } from '@/stores'

/**
 * 按应用明暗主题生成 ECharts option。
 * 未接入独立 echarts 主题包；各图表通过 isDark 自行适配颜色即可。
 */
interface OptionsFn {
  (isDark: boolean): EChartsOption
}

export function useChart(sourceOption: OptionsFn) {
  const appStore = useAppStore()
  const isDark = computed(() => appStore.theme === 'dark')

  const chartOption = computed<EChartsOption>(() => sourceOption(isDark.value))

  return { chartOption }
}
