<template>
  <div class="gi_page">
    <a-spin :loading="loading">
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        auto-label-width
        label-align="left"
        :layout="width >= 500 ? 'horizontal' : 'vertical'"
        :disabled="!isUpdate"
        scroll-to-first-error
      >
        <a-divider orientation="left">基础信息</a-divider>
        <a-form-item field="DASHBOARD_TITLE" :label="dashboardConfig.DASHBOARD_TITLE?.name || '大屏名称'">
          <a-input
            v-model="form.DASHBOARD_TITLE"
            class="input-width-lg"
            placeholder="运营中枢顶栏主标题"
            allow-clear
            :max-length="64"
            show-word-limit
          />
          <template #extra>
            显示在运营中枢（大屏）顶栏中央，建议简洁有辨识度。
          </template>
        </a-form-item>
        <a-form-item
          field="DASHBOARD_DEFAULT_DAYS"
          :label="dashboardConfig.DASHBOARD_DEFAULT_DAYS?.name || '默认统计周期'"
        >
          <a-radio-group v-model="form.DASHBOARD_DEFAULT_DAYS" type="button">
            <a-radio :value="7">近 7 天</a-radio>
            <a-radio :value="30">近 30 天</a-radio>
          </a-radio-group>
          <template #extra>
            进入大屏时「访问量每日趋势」的默认统计周期，用户仍可在大屏内切换。
          </template>
        </a-form-item>
        <a-form-item
          field="DASHBOARD_REFRESH_INTERVAL"
          :label="dashboardConfig.DASHBOARD_REFRESH_INTERVAL?.name || '数据刷新间隔（秒）'"
        >
          <a-input-number
            v-model="form.DASHBOARD_REFRESH_INTERVAL"
            class="input-width"
            :min="0"
            :max="3600"
            :precision="0"
          />
          <template #extra>
            仪表盘数据自动刷新周期，单位秒；填 0 表示不自动刷新（需手动切换周期或重新进入）。建议 30～300。
          </template>
        </a-form-item>

        <a-divider orientation="left">展示开关</a-divider>
        <a-form-item field="DASHBOARD_SHOW_CLOCK" :label="dashboardConfig.DASHBOARD_SHOW_CLOCK?.name || '显示时钟'">
          <a-switch
            v-model="form.DASHBOARD_SHOW_CLOCK"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            顶栏右侧日期、时间与星期。
          </template>
        </a-form-item>
        <a-form-item field="DASHBOARD_SHOW_STATUS" :label="dashboardConfig.DASHBOARD_SHOW_STATUS?.name || '显示运行状态'">
          <a-switch
            v-model="form.DASHBOARD_SHOW_STATUS"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            工具栏运行状态指示灯（正常 / 告警 / 异常等）。
          </template>
        </a-form-item>
        <a-form-item field="DASHBOARD_SHOW_KPI" :label="dashboardConfig.DASHBOARD_SHOW_KPI?.name || '显示指标概览'">
          <a-switch
            v-model="form.DASHBOARD_SHOW_KPI"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            顶部 KPI 卡片：访问量、独立访客、模块访问、终端环境。
          </template>
        </a-form-item>
        <a-form-item field="DASHBOARD_SHOW_NOTICE" :label="dashboardConfig.DASHBOARD_SHOW_NOTICE?.name || '显示公告'">
          <a-switch
            v-model="form.DASHBOARD_SHOW_NOTICE"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            底部「最新公告」滚动面板。
          </template>
        </a-form-item>
        <a-form-item field="DASHBOARD_SHOW_FS_TIP" :label="dashboardConfig.DASHBOARD_SHOW_FS_TIP?.name || '全屏入口提示'">
          <a-switch
            v-model="form.DASHBOARD_SHOW_FS_TIP"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            进入大屏时是否短暂提示右下角全屏按钮位置。
          </template>
        </a-form-item>
        <a-form-item field="DASHBOARD_SHOW_DECOR" :label="dashboardConfig.DASHBOARD_SHOW_DECOR?.name || '显示装饰动效'">
          <a-switch
            v-model="form.DASHBOARD_SHOW_DECOR"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            边框角标与扫描线等视觉装饰；关闭后画面更简洁，适合长时间投屏。
          </template>
        </a-form-item>

        <a-space style="margin-bottom: 16px">
          <a-button v-if="!isUpdate" v-permission="['system:dashboardConfig:update']" type="primary" @click="onUpdate">
            <template #icon><icon-edit /></template>修改
          </a-button>
          <a-button v-if="!isUpdate" v-permission="['system:dashboardConfig:update']" @click="onResetValue">
            <template #icon><icon-undo /></template>恢复默认
          </a-button>
          <a-button v-if="isUpdate" type="primary" @click="handleSave">
            <template #icon><icon-save /></template>保存
          </a-button>
          <a-button v-if="isUpdate" @click="reset">
            <template #icon><icon-refresh /></template>重置
          </a-button>
          <a-button v-if="isUpdate" @click="handleCancel">
            <template #icon><icon-undo /></template>取消
          </a-button>
        </a-space>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { useWindowSize } from '@vueuse/core'
import { type FormInstance, Message, Modal } from '@arco-design/web-vue'
import { type DashboardConfig, type OptionResp, listOption, resetOptionValue, updateOption } from '@/apis/system'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'SystemDashboardConfig' })
const { width } = useWindowSize()

const loading = ref(false)
const formRef = ref<FormInstance>()
const [form] = useResetReactive({
  DASHBOARD_TITLE: 'LS-Aegis 雷铄御警安全应用运营中枢',
  DASHBOARD_SHOW_CLOCK: 1,
  DASHBOARD_SHOW_STATUS: 1,
  DASHBOARD_SHOW_KPI: 1,
  DASHBOARD_SHOW_NOTICE: 1,
  DASHBOARD_SHOW_FS_TIP: 1,
  DASHBOARD_SHOW_DECOR: 1,
  DASHBOARD_DEFAULT_DAYS: 7 as 7 | 30,
  DASHBOARD_REFRESH_INTERVAL: 60,
})

const rules: FormInstance['rules'] = {
  DASHBOARD_TITLE: [{ required: true, message: '请输入大屏名称' }],
  DASHBOARD_DEFAULT_DAYS: [{ required: true, message: '请选择默认统计周期' }],
  DASHBOARD_REFRESH_INTERVAL: [{ required: true, message: '请输入数据刷新间隔' }],
  DASHBOARD_SHOW_CLOCK: [{ required: true, message: '请选择' }],
  DASHBOARD_SHOW_STATUS: [{ required: true, message: '请选择' }],
  DASHBOARD_SHOW_KPI: [{ required: true, message: '请选择' }],
  DASHBOARD_SHOW_NOTICE: [{ required: true, message: '请选择' }],
  DASHBOARD_SHOW_FS_TIP: [{ required: true, message: '请选择' }],
  DASHBOARD_SHOW_DECOR: [{ required: true, message: '请选择' }],
}

const dashboardConfig = ref<Partial<DashboardConfig>>({})

const BOOL_CODES = new Set([
  'DASHBOARD_SHOW_CLOCK',
  'DASHBOARD_SHOW_STATUS',
  'DASHBOARD_SHOW_KPI',
  'DASHBOARD_SHOW_NOTICE',
  'DASHBOARD_SHOW_FS_TIP',
  'DASHBOARD_SHOW_DECOR',
])

const NUMBER_CODES = new Set(['DASHBOARD_DEFAULT_DAYS', 'DASHBOARD_REFRESH_INTERVAL'])

const applyFormFromConfig = () => {
  form.DASHBOARD_TITLE = String(
    dashboardConfig.value.DASHBOARD_TITLE?.value || 'LS-Aegis 雷铄御警安全应用运营中枢',
  )
  form.DASHBOARD_SHOW_CLOCK = Number(dashboardConfig.value.DASHBOARD_SHOW_CLOCK?.value ?? 1)
  form.DASHBOARD_SHOW_STATUS = Number(dashboardConfig.value.DASHBOARD_SHOW_STATUS?.value ?? 1)
  form.DASHBOARD_SHOW_KPI = Number(dashboardConfig.value.DASHBOARD_SHOW_KPI?.value ?? 1)
  form.DASHBOARD_SHOW_NOTICE = Number(dashboardConfig.value.DASHBOARD_SHOW_NOTICE?.value ?? 1)
  form.DASHBOARD_SHOW_FS_TIP = Number(dashboardConfig.value.DASHBOARD_SHOW_FS_TIP?.value ?? 1)
  form.DASHBOARD_SHOW_DECOR = Number(dashboardConfig.value.DASHBOARD_SHOW_DECOR?.value ?? 1)
  const days = Number(dashboardConfig.value.DASHBOARD_DEFAULT_DAYS?.value || 7)
  form.DASHBOARD_DEFAULT_DAYS = days === 30 ? 30 : 7
  form.DASHBOARD_REFRESH_INTERVAL = Number(dashboardConfig.value.DASHBOARD_REFRESH_INTERVAL?.value ?? 60)
}

const reset = () => {
  formRef.value?.resetFields()
  applyFormFromConfig()
}

const isUpdate = ref(false)
const onUpdate = () => {
  isUpdate.value = true
}

const handleCancel = () => {
  reset()
  isUpdate.value = false
}

const queryForm = { category: 'DASHBOARD' }

const getDataList = async () => {
  try {
    loading.value = true
    const { data } = await listOption(queryForm)
    dashboardConfig.value = data.reduce((obj: Partial<DashboardConfig>, option: OptionResp) => {
      const raw = option.value ?? option.defaultValue
      let parsed: string | number
      if (BOOL_CODES.has(option.code) || NUMBER_CODES.has(option.code)) {
        parsed = Number.parseInt(String(raw ?? '0'), 10)
      } else {
        parsed = String(raw ?? '')
      }
      obj[option.code as keyof DashboardConfig] = { ...option, value: parsed as any }
      return obj
    }, {})
    handleCancel()
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  const isInvalid = await formRef.value?.validate()
  if (isInvalid) return false
  await updateOption(
    Object.entries(form).map(([key, value]) => ({
      id: dashboardConfig.value[key as keyof DashboardConfig]?.id,
      code: key,
      value,
    })),
  )
  await getDataList()
  Message.success('保存成功')
}

const handleResetValue = async () => {
  await resetOptionValue(queryForm)
  Message.success('恢复成功')
  await getDataList()
}

const onResetValue = () => {
  Modal.warning({
    title: '警告',
    content: '确认恢复大屏配置为默认值吗？',
    hideCancel: false,
    maskClosable: false,
    onOk: handleResetValue,
  })
}

onMounted(() => {
  getDataList()
})
</script>

<style scoped lang="scss">
:deep(.arco-form-item.arco-form-item-has-help) {
  margin-bottom: 5px;
}

.input-width {
  width: 240px;
}

.input-width-lg {
  width: 420px;
  max-width: 100%;
}
</style>
