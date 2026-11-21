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
        <a-alert type="info" style="margin-bottom: 16px">
          任务调度默认关闭。开启后显示对应菜单；实际执行还需运维完成调度中心配置。
        </a-alert>

        <a-form-item field="MODULE_SCHEDULE_ENABLED" :label="moduleConfig.MODULE_SCHEDULE_ENABLED?.name || '任务调度'">
          <a-switch
            v-model="form.MODULE_SCHEDULE_ENABLED"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>开</template>
            <template #unchecked>关</template>
          </a-switch>
          <template #extra>
            {{ moduleConfig.MODULE_SCHEDULE_ENABLED?.description || '关闭后隐藏任务调度菜单并拦截相关接口。' }}
          </template>
        </a-form-item>

        <a-space style="margin-top: 20px">
          <a-button v-if="!isUpdate" v-permission="['system:moduleConfig:update']" type="primary" @click="onUpdate">
            <template #icon><icon-edit /></template>修改
          </a-button>
          <a-button v-if="isUpdate" v-permission="['system:moduleConfig:update']" type="primary" @click="handleSave">
            <template #icon><icon-save /></template>保存
          </a-button>
          <a-button v-if="isUpdate" @click="reset">
            <template #icon><icon-refresh /></template>重置
          </a-button>
          <a-button v-if="isUpdate" @click="handleCancel">
            <template #icon><icon-undo /></template>取消
          </a-button>
          <a-button v-permission="['system:moduleConfig:update']" status="warning" @click="onResetValue">
            <template #icon><icon-sync /></template>恢复默认
          </a-button>
        </a-space>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { useWindowSize } from '@vueuse/core'
import { type FormInstance, Message, Modal } from '@arco-design/web-vue'
import { type ModuleConfig, type OptionResp, listOption, resetOptionValue, updateOption } from '@/apis/system'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'SystemModuleConfig' })
const { width } = useWindowSize()

const loading = ref(false)
const formRef = ref<FormInstance>()
const [form] = useResetReactive({
  MODULE_SCHEDULE_ENABLED: 0,
})

const rules: FormInstance['rules'] = {
  MODULE_SCHEDULE_ENABLED: [{ required: true, message: '请选择' }],
}

const moduleConfig = ref<Partial<ModuleConfig>>({})

const BOOL_CODES = new Set([
  'MODULE_SCHEDULE_ENABLED',
])

const applyFormFromConfig = () => {
  form.MODULE_SCHEDULE_ENABLED = Number(moduleConfig.value.MODULE_SCHEDULE_ENABLED?.value ?? 0)
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

const queryForm = { code: ['MODULE_SCHEDULE_ENABLED'] }

const getDataList = async () => {
  try {
    loading.value = true
    const { data } = await listOption(queryForm)
    moduleConfig.value = data.filter((option) => BOOL_CODES.has(option.code)).reduce((obj: Partial<ModuleConfig>, option: OptionResp) => {
      const raw = option.value ?? option.defaultValue
      const parsed = BOOL_CODES.has(option.code) ? Number.parseInt(String(raw ?? '0'), 10) : String(raw ?? '')
      obj[option.code as keyof ModuleConfig] = { ...option, value: parsed as any }
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
      id: moduleConfig.value[key as keyof ModuleConfig]?.id,
      code: key,
      value,
    })),
  )
  await getDataList()
  Message.success('保存成功，刷新页面后菜单生效')
}

const handleResetValue = async () => {
  await resetOptionValue({ code: ['MODULE_SCHEDULE_ENABLED'] })
  Message.success('恢复成功，刷新页面后菜单生效')
  await getDataList()
}

const onResetValue = () => {
  Modal.warning({
    title: '警告',
    content: '确认恢复功能模块配置为默认值吗？',
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
.gi_page {
  padding: 16px 20px 24px;
}

code {
  padding: 0 4px;
  font-size: 12px;
  background: var(--color-fill-2);
  border-radius: 2px;
}
</style>
