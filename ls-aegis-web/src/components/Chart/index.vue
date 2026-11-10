<template>
  <VCharts ref="chart" :option="option" :autoresize="autoResize" :style="{ width, height }" />
</template>

<script setup lang="ts">
import { use } from 'echarts/core'
import { BarChart, LineChart, MapChart, PieChart } from 'echarts/charts'
import {
  DataZoomComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VCharts from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  MapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
  ToolboxComponent,
  VisualMapComponent,
  GraphicComponent,
])

defineProps({
  option: {
    type: Object,
    default() {
      return {}
    },
  },
  autoResize: {
    type: Boolean,
    default: true,
  },
  width: {
    type: String,
    default: '100%',
  },
  height: {
    type: String,
    default: '100%',
  },
})

const chart = ref(null)
defineExpose({
  chart,
})
</script>

<style scoped lang="less"></style>
