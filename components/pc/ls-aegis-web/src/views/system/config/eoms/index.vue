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
          单据编号为「前缀 + 年月日后两位年 + 两位日流水」（如 BX26081901）。改前缀后新单据立即生效，已生成编号不会回写。
        </a-alert>

        <h4 class="section-title">编号前缀</h4>
        <a-row :gutter="16">
          <a-col v-for="code in prefixCodes" :key="code" :xs="24" :md="12" :xl="8">
            <a-form-item :field="code" :label="labelOf(code)" :help="descOf(code)">
              <a-input v-model="form[code]" :max-length="4" placeholder="2～4 位大写字母" />
            </a-form-item>
          </a-col>
        </a-row>

        <h4 class="section-title">地理位置</h4>
        <a-alert type="info" style="margin-bottom: 16px">外部地图API已停用。Android使用系统地址解析，不支持或解析失败时保留坐标，不调用外部地图服务。</a-alert>
        <a-form-item
          field="GEO_BAIDU_AK"
          :label="labelOf('GEO_BAIDU_AK')"
          :help="akHelpText"
        >
          <a-input-password
            v-model="form.GEO_BAIDU_AK"
            disabled
            :default-visibility="true"
            :invisible-button="isUpdate"
            placeholder="外部地图API已停用，无需配置AK"
            autocomplete="off"
            @visibility-change="onAkVisibilityChange"
          />
        </a-form-item>
        <a-form-item label=" ">
          <a-button
            v-permission="['system:eomsConfig:get', 'system:eomsConfig:update']"
            type="outline"
            :loading="testingAk"
            :disabled="true"
            @click="onTestBaiduAk"
          >
            <template #icon><icon-thunderbolt /></template>
            测试地图 AK
          </a-button>
          <span class="ak-test-tip">外部地图测试已停用</span>
        </a-form-item>
        <a-form-item field="GEO_BAIDU_REGEO_URL" :label="labelOf('GEO_BAIDU_REGEO_URL')" :help="descOf('GEO_BAIDU_REGEO_URL')">
          <a-input v-model="form.GEO_BAIDU_REGEO_URL" disabled placeholder="外部地图API已停用" />
        </a-form-item>

        <h4 class="section-title">现场水印</h4>
        <a-form-item field="WATERMARK_MAX_SIDE" :label="labelOf('WATERMARK_MAX_SIDE')" :help="descOf('WATERMARK_MAX_SIDE')">
          <a-input-number v-model="form.WATERMARK_MAX_SIDE" :min="640" :max="4096" :precision="0" />
        </a-form-item>

        <a-space style="margin-top: 8px">
          <a-button v-if="!isUpdate" v-permission="['system:eomsConfig:update']" type="primary" @click="onUpdate">
            <template #icon><icon-edit /></template>修改
          </a-button>
          <a-button v-if="isUpdate" v-permission="['system:eomsConfig:update']" type="primary" @click="handleSave">
            <template #icon><icon-save /></template>保存
          </a-button>
          <a-button v-if="isUpdate" @click="reset">
            <template #icon><icon-refresh /></template>重置
          </a-button>
          <a-button v-if="isUpdate" @click="handleCancel">
            <template #icon><icon-undo /></template>取消
          </a-button>
          <a-button v-permission="['system:eomsConfig:update']" status="warning" @click="onResetValue">
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
import { testBaiduMapAk, getBaiduAkPlain } from '@/apis/eoms'
import { type EomsConfig, type OptionResp, listOption, resetOptionValue, updateOption } from '@/apis/system'

defineOptions({ name: 'SystemEomsConfig' })
const { width } = useWindowSize()

/** 与后端 OptionServiceImpl 脱敏占位一致 */
const SENSITIVE_MASK = '********'

const PREFIX_PATTERN = /^[A-Z]{2,4}$/
const prefixRule = [{
  validator: (value: string, callback: (error?: string) => void) => {
    if (!value || PREFIX_PATTERN.test(String(value).trim().toUpperCase())) {
      callback()
      return
    }
    callback('须为 2～4 位大写字母')
  },
}]

const loading = ref(false)
const testingAk = ref(false)
const revealingAk = ref(false)
/** 库中已有 AK（接口回显为脱敏占位） */
const akConfigured = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})
const optionMap = ref<Partial<EomsConfig>>({})
const prefixCodes = ref<string[]>([])
const isUpdate = ref(false)

const rules = computed<FormInstance['rules']>(() => {
  const next: FormInstance['rules'] = {}
  prefixCodes.value.forEach((code) => {
    next[code] = prefixRule
  })
  return next
})

const labelOf = (code: string) => optionMap.value[code as keyof EomsConfig]?.name || code
const descOf = (code: string) => optionMap.value[code as keyof EomsConfig]?.description || ''
const akHelpText = computed(() => {
  if (akConfigured.value) {
    return '已配置 AK；不修改请留空。保存后不回显明文，点击「修改」后可通过眼睛图标查看。'
  }
  return descOf('GEO_BAIDU_AK')
})

const applyFormFromConfig = () => {
  Object.entries(optionMap.value).forEach(([code, option]) => {
    if (!option) return
    const raw = option.value ?? option.defaultValue
    if (code === 'GEO_BAIDU_AK') {
      const ak = String(raw ?? '')
      akConfigured.value = !!ak
      // 脱敏占位不回填到输入框，避免误当成真实 AK 保存或展示
      form[code] = ak === SENSITIVE_MASK ? '' : ak
      return
    }
    form[code] = code === 'WATERMARK_MAX_SIDE' ? Number(raw ?? 1280) : String(raw ?? '')
  })
}

const reset = () => {
  formRef.value?.resetFields()
  applyFormFromConfig()
}

const onUpdate = () => {
  isUpdate.value = true
}

const handleCancel = () => {
  reset()
  isUpdate.value = false
}

const queryForm = { category: 'EOMS' }

const getDataList = async () => {
  try {
    loading.value = true
    const { data } = await listOption(queryForm)
    const next: Partial<EomsConfig> = {}
    const prefixes: string[] = []
    ;(data || []).forEach((option: OptionResp) => {
      next[option.code as keyof EomsConfig] = option
      if (option.code?.endsWith('_NO_PREFIX')) {
        prefixes.push(option.code)
      }
      if (!(option.code in form)) {
        form[option.code] = undefined
      }
    })
    optionMap.value = next
    prefixCodes.value = prefixes
    handleCancel()
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  const isInvalid = await formRef.value?.validate()
  if (isInvalid) return false
  const payload = Object.keys(optionMap.value).map((key) => {
    let value = form[key]
    if (key.endsWith('_NO_PREFIX') && typeof value === 'string') {
      value = value.trim().toUpperCase()
    }
    // 已配置 AK 且本次留空 → 回传脱敏占位，后端保留原值
    if (key === 'GEO_BAIDU_AK' && akConfigured.value && !String(value || '').trim()) {
      value = SENSITIVE_MASK
    }
    return {
      id: optionMap.value[key as keyof EomsConfig]?.id,
      code: key,
      value,
    }
  })
  await updateOption(payload)
  await getDataList()
  Message.success('保存成功')
}

/** 点击眼睛显示时，从服务端拉取已保存 AK 明文 */
const onAkVisibilityChange = async (visible: boolean) => {
  if (!visible || !akConfigured.value || revealingAk.value) return
  if (String(form.GEO_BAIDU_AK || '').trim()) return
  revealingAk.value = true
  try {
    const { data } = await getBaiduAkPlain()
    form.GEO_BAIDU_AK = data || ''
  } catch {
    // 错误已由 http 拦截器提示
  } finally {
    revealingAk.value = false
  }
}

/** 获取浏览器当前位置（WGS84） */
const getBrowserLocation = (): Promise<{ latitude: number, longitude: number }> => {
  return new Promise((resolve, reject) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    let settled = false
    const settle = (callback: () => void) => {
      if (settled) return
      settled = true
      clearTimeout(deadline)
      callback()
    }
    // Browser acquisition timeouts may not include time spent waiting for permission/provider callbacks.
    const deadline = setTimeout(() => settle(() => reject(new Error('定位超时，请检查位置权限后重试'))), 10000)
    try {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          settle(() => resolve({
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
          }))
        },
        (err) => {
          if (err.code === err.PERMISSION_DENIED) {
            settle(() => reject(new Error('请允许浏览器获取位置权限后再测试')))
            return
          }
          settle(() => reject(new Error('定位失败，请检查浏览器或系统定位设置')))
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
      )
    } catch (error) {
      settle(() => reject(error instanceof Error ? error : new Error('定位失败，请检查浏览器或系统定位设置')))
    }
  })
}

const onTestBaiduAk = async () => {
  if (testingAk.value) return
  if (!akConfigured.value) {
    Message.warning('请先配置并保存百度地图 AK')
    return
  }
  try {
    testingAk.value = true
    let coords: { latitude: number, longitude: number }
    try {
      coords = await getBrowserLocation()
    } catch (e) {
      Message.warning(e instanceof Error ? e.message : '定位失败')
      return
    }
    const { data } = await testBaiduMapAk({
      longitude: String(coords.longitude),
      latitude: String(coords.latitude),
    })
    if (data?.success) {
      Message.success(`AK 可用${data.sampleAddress ? `：${data.sampleAddress}` : ''}`)
      return
    }
    Message.error(data?.message || 'AK 不可用，请检查密钥类型与 IP 白名单')
  } catch {
    // 错误已由 http 拦截器提示
  } finally {
    testingAk.value = false
  }
}

const handleResetValue = async () => {
  await resetOptionValue(queryForm)
  Message.success('已恢复默认')
  await getDataList()
}

const onResetValue = () => {
  Modal.warning({
    title: '警告',
    content: '确认将运维报修参数恢复为默认值吗？自定义前缀与地图密钥会被清空。',
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

.section-title {
  margin: 8px 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.ak-test-tip {
  margin-left: 8px;
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
