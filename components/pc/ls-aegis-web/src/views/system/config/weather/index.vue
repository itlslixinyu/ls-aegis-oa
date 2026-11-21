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
        <a-form-item field="WEATHER_ENABLED" :label="weatherConfig.WEATHER_ENABLED?.name || '是否启用'">
          <a-switch
            v-model="form.WEATHER_ENABLED"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>是</template>
            <template #unchecked>否</template>
          </a-switch>
          <template #extra>
            控制运营中枢顶栏是否展示天气信息。
          </template>
        </a-form-item>
        <a-form-item field="WEATHER_CITY" :label="weatherConfig.WEATHER_CITY?.name || '默认城市'">
          <a-input
            v-model="form.WEATHER_CITY"
            class="input-width"
            placeholder="如：北京"
            allow-clear
            :max-length="20"
          />
          <template #extra>
            固定城市模式下使用；定位未授权或解析失败时作为展示城市。
          </template>
        </a-form-item>
        <a-form-item
          field="WEATHER_REFRESH_INTERVAL"
          :label="weatherConfig.WEATHER_REFRESH_INTERVAL?.name || '刷新时间间隔（秒）'"
        >
          <a-input-number
            v-model="form.WEATHER_REFRESH_INTERVAL"
            class="input-width"
            :min="60"
            :max="86400"
            :precision="0"
          />
          <template #extra>
            天气信息自动刷新周期，单位秒，建议 60～86400。
          </template>
        </a-form-item>
        <a-form-item field="WEATHER_PROVIDER" :label="weatherConfig.WEATHER_PROVIDER?.name || '数据来源'">
          <a-select v-model="form.WEATHER_PROVIDER" class="input-width">
            <a-option value="mock">本地模拟</a-option>
            <a-option value="qweather">和风天气</a-option>
          </a-select>
          <template #extra>
            本地模拟仅用于联调；和风须自行申请凭据并遵守其协议与配额，异常时回退本地模拟。
          </template>
        </a-form-item>

        <template v-if="form.WEATHER_PROVIDER === 'qweather'">
          <a-form-item field="WEATHER_CITY_MODE" :label="weatherConfig.WEATHER_CITY_MODE?.name || '城市模式'">
            <a-select v-model="form.WEATHER_CITY_MODE" class="input-width">
              <a-option value="auto">和风 GeoAPI 定位</a-option>
              <a-option value="fixed">固定城市</a-option>
            </a-select>
            <template #extra>
              「和风 GeoAPI 定位」在用户授权后使用浏览器位置解析城市；「固定城市」仅使用上方默认城市。
            </template>
          </a-form-item>
          <a-form-item
            field="WEATHER_API_HOST"
            :label="weatherConfig.WEATHER_API_HOST?.name || '和风 API Host'"
          >
            <a-input
              v-model="form.WEATHER_API_HOST"
              class="input-width-lg"
              placeholder="控制台「设置」中的专属 API Host"
              allow-clear
              :max-length="200"
            />
            <template #extra>
              填写控制台「设置」中的专属 API 访问地址（如 https://xxx.re.qweatherapi.com）。
            </template>
          </a-form-item>
          <a-form-item
            field="WEATHER_GEO_HOST"
            :label="weatherConfig.WEATHER_GEO_HOST?.name || '和风 GeoAPI 地址'"
          >
            <a-input
              v-model="form.WEATHER_GEO_HOST"
              class="input-width-lg"
              placeholder="可留空，默认与 API Host 相同"
              allow-clear
              :max-length="200"
            />
            <template #extra>
              可选。留空则用上方 API Host 做城市查询。
            </template>
          </a-form-item>

          <a-divider orientation="left">鉴权（API Key 与 JWT 二选一，都保留）</a-divider>
          <a-form-item
            field="WEATHER_API_KEY"
            :label="weatherConfig.WEATHER_API_KEY?.name || '和风 API Key'"
          >
            <a-input-password
              v-model="form.WEATHER_API_KEY"
              class="input-width-lg"
              placeholder="控制台「API KEY」类型凭据的 Key"
              allow-clear
              :max-length="128"
            />
            <template #extra>
              控制台创建「API KEY」凭据后填此处。与下方 JWT 二选一即可；两者都配时服务端优先用 JWT。脱敏为 ******** 时未改直接保存不会覆盖。
            </template>
          </a-form-item>

          <a-collapse :bordered="false" class="jwt-collapse">
            <a-collapse-item key="jwt" header="JWT 鉴权（凭据 ID + 项目 ID + 私钥）">
              <a-form-item
                field="WEATHER_JWT_PROJECT_ID"
                :label="weatherConfig.WEATHER_JWT_PROJECT_ID?.name || '项目 ID（sub）'"
              >
                <a-input
                  v-model="form.WEATHER_JWT_PROJECT_ID"
                  class="input-width-lg"
                  placeholder="仅 JWT 时填写"
                  allow-clear
                  :max-length="64"
                />
              </a-form-item>
              <a-form-item
                field="WEATHER_JWT_KID"
                :label="weatherConfig.WEATHER_JWT_KID?.name || '凭据 ID（kid）'"
              >
                <a-input
                  v-model="form.WEATHER_JWT_KID"
                  class="input-width-lg"
                  placeholder="仅 JWT 时填写"
                  allow-clear
                  :max-length="64"
                />
                <template #extra>
                  JWT 凭据的 ID；仅有凭据 ID、没有 Ed25519 私钥时无法走 JWT，请改用上方 API Key。
                </template>
              </a-form-item>
              <a-form-item label="JWT 密钥">
                <a-space wrap>
                  <a-button
                    type="outline"
                    :loading="generatingKey"
                    :disabled="!isUpdate"
                    @click="handleGenerateKeyPair"
                  >
                    <template #icon><icon-safe /></template>
                    一键生成
                  </a-button>
                  <a-button
                    type="secondary"
                    :disabled="!generatedPublicKey"
                    @click="copyText(generatedPublicKey, '公钥已复制')"
                  >
                    复制公钥
                  </a-button>
                </a-space>
                <template #extra>
                  公钥上传至和风控制台创建 JWT 凭据；私钥仅保存在本系统。
                </template>
              </a-form-item>
              <a-form-item v-if="generatedPublicKey" label="公钥">
                <pre class="jwt-pem">{{ generatedPublicKey }}</pre>
              </a-form-item>
              <a-form-item
                field="WEATHER_JWT_PRIVATE_KEY"
                :label="weatherConfig.WEATHER_JWT_PRIVATE_KEY?.name || 'JWT 私钥'"
              >
                <a-textarea
                  v-model="form.WEATHER_JWT_PRIVATE_KEY"
                  class="input-width-lg"
                  placeholder="仅 JWT 时粘贴 PEM；用 API Key 可留空"
                  :auto-size="{ minRows: 1, maxRows: 6 }"
                  allow-clear
                />
              </a-form-item>
            </a-collapse-item>
          </a-collapse>
        </template>

        <a-space style="margin-bottom: 16px; margin-top: 12px">
          <a-button v-if="!isUpdate" v-permission="['system:weatherConfig:update']" type="primary" @click="onUpdate">
            <template #icon><icon-edit /></template>修改
          </a-button>
          <a-button
            v-if="!isUpdate"
            v-permission="['system:weatherConfig:get']"
            :loading="refreshing"
            @click="onRefreshCache"
          >
            <template #icon><icon-refresh /></template>一键刷新
          </a-button>
          <a-button
            v-if="!isUpdate"
            v-permission="['system:weatherConfig:get']"
            type="outline"
            :loading="testing"
            @click="onTestConnection"
          >
            <template #icon><icon-thunderbolt /></template>测试连接
          </a-button>
          <a-button v-if="!isUpdate" v-permission="['system:weatherConfig:update']" @click="onResetValue">
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
import { type OptionResp, type WeatherConfig, listOption, refreshWeatherNow, resetOptionValue, testWeatherConnection, updateOption } from '@/apis/system'
import { useResetReactive } from '@/hooks'

defineOptions({ name: 'SystemWeatherConfig' })
const { width } = useWindowSize()

const loading = ref(false)
const refreshing = ref(false)
const testing = ref(false)
const generatingKey = ref(false)
const generatedPublicKey = ref('')
const formRef = ref<FormInstance>()
const [form] = useResetReactive({
  WEATHER_ENABLED: 1,
  WEATHER_CITY_MODE: 'auto',
  WEATHER_CITY: '北京',
  WEATHER_REFRESH_INTERVAL: 600,
  WEATHER_PROVIDER: 'mock',
  WEATHER_API_KEY: '',
  WEATHER_JWT_KID: '',
  WEATHER_JWT_PROJECT_ID: '',
  WEATHER_JWT_PRIVATE_KEY: '',
  WEATHER_API_HOST: '',
  WEATHER_GEO_HOST: '',
})
const isQweather = computed(() => form.WEATHER_PROVIDER === 'qweather')

const isBlankOrMask = (v: unknown) => {
  const s = String(v || '').trim()
  return !s || s === '********'
}

const hasApiKey = () => !isBlankOrMask(form.WEATHER_API_KEY)

const hasJwt = () => {
  const pk = String(form.WEATHER_JWT_PRIVATE_KEY || '').trim()
  return !!String(form.WEATHER_JWT_KID || '').trim()
    && !!String(form.WEATHER_JWT_PROJECT_ID || '').trim()
    && !!pk
    && (pk === '********' || pk.includes('BEGIN PRIVATE KEY'))
}

const rules = computed<FormInstance['rules']>(() => {
  const qweatherRequired = isQweather.value
  return {
    WEATHER_ENABLED: [{ required: true, message: '请选择是否启用' }],
    WEATHER_CITY: [{ required: true, message: '请输入默认城市' }],
    WEATHER_REFRESH_INTERVAL: [{ required: true, message: '请输入刷新时间间隔' }],
    WEATHER_PROVIDER: [{ required: true, message: '请选择数据来源' }],
    WEATHER_CITY_MODE: qweatherRequired ? [{ required: true, message: '请选择城市模式' }] : [],
    WEATHER_API_HOST: qweatherRequired ? [{ required: true, message: '请填写和风 API Host' }] : [],
    WEATHER_API_KEY: qweatherRequired
      ? [
          {
            validator: (_value, callback) => {
              if (hasApiKey() || hasJwt()) {
                callback()
                return
              }
              callback('请填写 API Key（推荐）；或配置完整 JWT（含私钥）')
            },
          },
        ]
      : [],
    WEATHER_JWT_PRIVATE_KEY: qweatherRequired
      ? [
          {
            validator: (value, callback) => {
              if (hasApiKey()) {
                callback()
                return
              }
              const v = String(value || '').trim()
              if (!v) {
                callback('未填 API Key 时须配置 JWT 私钥')
                return
              }
              if (v === '********') {
                callback()
                return
              }
              if (!v.includes('BEGIN PRIVATE KEY')) {
                callback('请粘贴完整 PEM 私钥（含 BEGIN/END）')
                return
              }
              callback()
            },
          },
        ]
      : [],
  }
})

const weatherConfig = ref<Partial<WeatherConfig>>({})

const arrayBufferToBase64 = (buffer: ArrayBuffer) => {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  bytes.forEach((b) => {
    binary += String.fromCharCode(b)
  })
  return btoa(binary)
}

const toPem = (type: 'PRIVATE KEY' | 'PUBLIC KEY', buffer: ArrayBuffer) => {
  const base64 = arrayBufferToBase64(buffer)
  const lines = base64.match(/.{1,64}/g) || []
  return `-----BEGIN ${type}-----\n${lines.join('\n')}\n-----END ${type}-----`
}

const copyText = async (text: string, successMsg: string) => {
  try {
    await navigator.clipboard.writeText(text)
    Message.success(successMsg)
  } catch {
    Message.error('复制失败，请手动选择文本复制')
  }
}

const handleGenerateKeyPair = async () => {
  if (!isUpdate.value) {
    Message.warning('请先点击「修改」')
    return
  }
  if (!window.crypto?.subtle) {
    Message.error('当前浏览器不支持 WebCrypto，请升级 Chrome/Edge/Firefox 后重试')
    return
  }
  generatingKey.value = true
  try {
    const keyPair = await window.crypto.subtle.generateKey({ name: 'Ed25519' }, true, ['sign', 'verify'])
    if (!('privateKey' in keyPair)) throw new Error('浏览器未生成有效密钥对')
    const privateDer = await window.crypto.subtle.exportKey('pkcs8', keyPair.privateKey)
    const publicDer = await window.crypto.subtle.exportKey('spki', keyPair.publicKey)
    form.WEATHER_JWT_PRIVATE_KEY = toPem('PRIVATE KEY', privateDer)
    generatedPublicKey.value = toPem('PUBLIC KEY', publicDer)
    Message.success('密钥已生成：私钥已填入，请复制公钥到和风控制台')
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    Message.error(`生成失败：${msg || '浏览器可能不支持 Ed25519'}`)
  } finally {
    generatingKey.value = false
  }
}

const applyFormFromConfig = () => {
  form.WEATHER_ENABLED = Number(weatherConfig.value.WEATHER_ENABLED?.value ?? 1)
  form.WEATHER_CITY_MODE = String(weatherConfig.value.WEATHER_CITY_MODE?.value || 'auto')
  form.WEATHER_CITY = String(weatherConfig.value.WEATHER_CITY?.value || '北京')
  form.WEATHER_REFRESH_INTERVAL = Number(weatherConfig.value.WEATHER_REFRESH_INTERVAL?.value || 600)
  const provider = String(weatherConfig.value.WEATHER_PROVIDER?.value || 'mock')
  form.WEATHER_PROVIDER = provider === 'open-meteo' ? 'qweather' : provider
  form.WEATHER_API_KEY = String(weatherConfig.value.WEATHER_API_KEY?.value || '')
  form.WEATHER_JWT_KID = String(weatherConfig.value.WEATHER_JWT_KID?.value || '')
  form.WEATHER_JWT_PROJECT_ID = String(weatherConfig.value.WEATHER_JWT_PROJECT_ID?.value || '')
  form.WEATHER_JWT_PRIVATE_KEY = String(weatherConfig.value.WEATHER_JWT_PRIVATE_KEY?.value || '')
  form.WEATHER_API_HOST = String(weatherConfig.value.WEATHER_API_HOST?.value || '')
  form.WEATHER_GEO_HOST = String(weatherConfig.value.WEATHER_GEO_HOST?.value || '')
  generatedPublicKey.value = ''
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

const queryForm = { category: 'WEATHER' }

const getDataList = async () => {
  try {
    loading.value = true
    const { data } = await listOption(queryForm)
    weatherConfig.value = data.reduce((obj: Partial<WeatherConfig>, option: OptionResp) => {
      const raw = option.value ?? option.defaultValue
      const parsed =
        option.code === 'WEATHER_ENABLED' || option.code === 'WEATHER_REFRESH_INTERVAL'
          ? Number.parseInt(String(raw ?? '0'), 10)
          : String(raw ?? '')
      obj[option.code as keyof WeatherConfig] = { ...option, value: parsed as any }
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
  if (isQweather.value && !hasApiKey() && !hasJwt()) {
    Message.warning('请填写 API Key，或配置完整 JWT（含私钥）')
    return false
  }
  if (isQweather.value && !hasApiKey()) {
    const kid = String(form.WEATHER_JWT_KID || '').trim()
    const projectId = String(form.WEATHER_JWT_PROJECT_ID || '').trim()
    if (!kid || !projectId) {
      Message.warning('未填 API Key 时须同时填写项目 ID、凭据 ID 与私钥')
      return false
    }
  }
  await updateOption(
    Object.entries(form).map(([key, value]) => ({
      id: weatherConfig.value[key as keyof WeatherConfig]?.id,
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
    content: '确认恢复天气配置为默认值吗？',
    hideCancel: false,
    maskClosable: false,
    onOk: handleResetValue,
  })
}

const formatWeatherTip = (data?: { city?: string, label?: string, temp?: number, aqi?: number | null, aqiCategory?: string, windLevel?: number, provider?: string }) => {
  const city = data?.city || '-'
  const label = data?.label || '-'
  const temp = data?.temp ?? '-'
  const aqiText = data?.aqiCategory || (data?.aqi != null ? String(data.aqi) : '--')
  const wind = data?.windLevel ?? '-'
  const provider = data?.provider || '-'
  return `${city}｜${label}｜${temp}°C｜空气质量 ${aqiText}｜风力 ${wind}级｜来源 ${provider}`
}

async function onRefreshCache() {
  try {
    refreshing.value = true
    const { data } = await refreshWeatherNow()
    Message.success(`已刷新：${formatWeatherTip(data)}`)
  } catch {
    // 错误已由 http 拦截器提示
  } finally {
    refreshing.value = false
  }
}

async function onTestConnection() {
  try {
    testing.value = true
    const { data } = await testWeatherConnection()
    Message.success(`连接成功：${formatWeatherTip(data)}`)
  } catch {
    // 错误已由 http 拦截器提示
  } finally {
    testing.value = false
  }
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

.jwt-collapse {
  margin-bottom: 8px;
  background: transparent;

  :deep(.arco-collapse-item-header) {
    padding-left: 0;
    background: transparent;
  }

  :deep(.arco-collapse-item-content-box) {
    padding: 8px 0 0;
  }
}

.jwt-pem {
  margin: 0;
  padding: 10px 12px;
  width: 420px;
  max-width: 100%;
  max-height: 160px;
  overflow: auto;
  box-sizing: border-box;
  background: var(--color-fill-2);
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
