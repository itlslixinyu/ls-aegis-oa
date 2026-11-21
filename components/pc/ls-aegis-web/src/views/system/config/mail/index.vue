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
        class="form"
      >
        <a-form-item
          field="MAIL_PROTOCOL"
          :label="mailConfig.MAIL_PROTOCOL.name"
          :help="mailConfig.MAIL_PROTOCOL.description"
          hide-asterisk
        >
          <a-select v-model="form.MAIL_PROTOCOL">
            <a-option label="SMTP" value="smtp" />
          </a-select>
        </a-form-item>
        <a-form-item
          field="MAIL_HOST"
          :label="mailConfig.MAIL_HOST.name"
          :help="mailConfig.MAIL_HOST.description"
          hide-asterisk
        >
          <a-input v-model="form.MAIL_HOST" />
        </a-form-item>
        <a-form-item
          field="MAIL_PORT"
          :label="mailConfig.MAIL_PORT.name"
          :help="mailConfig.MAIL_PORT.description"
          hide-asterisk
        >
          <a-input-number v-model="form.MAIL_PORT" :min="0" />
        </a-form-item>
        <a-form-item
          field="MAIL_USERNAME"
          :label="mailConfig.MAIL_USERNAME.name"
          :help="mailConfig.MAIL_USERNAME.description"
          hide-asterisk
        >
          <!-- Arco：visibility=true 才为 password 脱敏；false 反而是明文 -->
          <a-input-password
            v-model="form.MAIL_USERNAME"
            :default-visibility="true"
            :invisible-button="isUpdate"
            autocomplete="off"
          />
        </a-form-item>
        <a-form-item
          field="MAIL_PASSWORD"
          :label="mailConfig.MAIL_PASSWORD?.name"
          :help="passwordConfigured ? '已配置密码；不修改请留空。保存后不会回显明文。' : mailConfig.MAIL_PASSWORD.description"
          hide-asterisk
        >
          <a-input-password
            v-model="form.MAIL_PASSWORD"
            :default-visibility="true"
            :invisible-button="isUpdate"
            :placeholder="passwordConfigured ? '已保存，不修改请留空' : '请输入密码或三方客户端安全密码'"
            autocomplete="new-password"
          />
        </a-form-item>
        <a-form-item
          field="MAIL_SSL_ENABLED"
          :label="mailConfig.MAIL_SSL_ENABLED?.name"
          :help="mailConfig.MAIL_SSL_ENABLED.description"
          hide-asterisk
        >
          <a-switch
            v-model="form.MAIL_SSL_ENABLED"
            type="round"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </a-switch>
        </a-form-item>
        <a-form-item
          v-if="form.MAIL_SSL_ENABLED === 1"
          field="MAIL_SSL_PORT"
          :label="mailConfig.MAIL_SSL_PORT.name"
          :help="mailConfig.MAIL_SSL_PORT.description"
          hide-asterisk
        >
          <a-input-number v-model="form.MAIL_SSL_PORT" :min="0" />
        </a-form-item>
        <a-space style="margin-bottom: 16px">
          <a-button v-if="!isUpdate" v-permission="['system:mailConfig:update']" type="primary" @click="onUpdate">
            <template #icon><icon-edit /></template>修改
          </a-button>
          <a-button
            v-if="!isUpdate"
            v-permission="['system:mailConfig:update']"
            :loading="testLoading"
            @click="onTestMail"
          >
            <template #icon><icon-email /></template>发送测试邮件
          </a-button>
          <a-button v-if="!isUpdate" v-permission="['system:mailConfig:update']" @click="onResetValue">
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

    <a-modal
      v-model:visible="testVisible"
      title="发送测试邮件"
      :ok-loading="testLoading"
      unmount-on-close
      @before-ok="handleSendTest"
    >
      <a-form :model="testForm" layout="vertical">
        <a-form-item label="收件邮箱" required>
          <a-input v-model="testForm.to" placeholder="请输入用于接收测试邮件的邮箱" allow-clear />
        </a-form-item>
        <a-alert type="info">
          将使用当前已保存的 SMTP 配置发信。若开启了阿里邮箱「三方客户端安全密码」，请填写安全密码而非网页登录密码。
        </a-alert>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { useWindowSize } from '@vueuse/core'
import { type FormInstance, Message, Modal } from '@arco-design/web-vue'
import {
  type MailConfig,
  type OptionResp,
  listOption,
  resetOptionValue,
  sendTestMail,
  updateOption,
} from '@/apis/system'
import { useResetReactive } from '@/hooks'
import * as Regexp from '@/utils/regexp'

defineOptions({ name: 'SystemMailConfig' })

/** 与后端 OptionServiceImpl 脱敏占位一致 */
const PASSWORD_MASK = '********'

const { width } = useWindowSize()
const loading = ref<boolean>(false)
const testLoading = ref(false)
const testVisible = ref(false)
const testForm = reactive({ to: '' })
/** 库中已有密码（接口回显为脱敏占位） */
const passwordConfigured = ref(false)
const formRef = ref<FormInstance>()
const [form] = useResetReactive({
  MAIL_PROTOCOL: '',
  MAIL_HOST: '',
  MAIL_PORT: 0,
  MAIL_USERNAME: '',
  MAIL_PASSWORD: '',
  MAIL_SSL_ENABLED: 0,
  MAIL_SSL_PORT: 0,
})
const rules = computed<FormInstance['rules']>(() => ({
  MAIL_HOST: [{ required: true, message: '请输入值' }],
  MAIL_PORT: [{ required: true, message: '请输入值' }],
  MAIL_USERNAME: [{ required: true, message: '请输入值' }],
  // 已配置过密码时允许留空（表示不修改）
  MAIL_PASSWORD: passwordConfigured.value
    ? []
    : [{ required: true, message: '请输入邮箱密码' }],
  MAIL_SSL_PORT: [{ required: true, message: '请输入值' }],
}))

const mailConfig = ref<MailConfig>({
  MAIL_PROTOCOL: { id: '', name: '', code: 'MAIL_PROTOCOL', description: '', value: '' },
  MAIL_HOST: { id: '', name: '', code: 'MAIL_HOST', description: '', value: '' },
  MAIL_PORT: { id: '', name: '', code: 'MAIL_PORT', description: '', value: 0 },
  MAIL_USERNAME: { id: '', name: '', code: 'MAIL_USERNAME', description: '', value: '' },
  MAIL_PASSWORD: { id: '', name: '', code: 'MAIL_PASSWORD', description: '', value: '' },
  MAIL_SSL_ENABLED: { id: '', name: '', code: 'MAIL_SSL_ENABLED', description: '', value: 0 },
  MAIL_SSL_PORT: { id: '', name: '', code: 'MAIL_SSL_PORT', description: '', value: 0 },
})

// 重置
const reset = () => {
  formRef.value?.resetFields()
  form.MAIL_PROTOCOL = mailConfig.value.MAIL_PROTOCOL.value || ''
  form.MAIL_HOST = mailConfig.value.MAIL_HOST.value || ''
  form.MAIL_PORT = mailConfig.value.MAIL_PORT.value || 0
  form.MAIL_USERNAME = mailConfig.value.MAIL_USERNAME.value || ''
  const pwd = mailConfig.value.MAIL_PASSWORD?.value || ''
  passwordConfigured.value = !!pwd && pwd !== ''
  // 脱敏占位不回填到输入框，避免误当成真实密码保存
  form.MAIL_PASSWORD = pwd === PASSWORD_MASK ? '' : pwd
  form.MAIL_SSL_ENABLED = mailConfig.value.MAIL_SSL_ENABLED.value || 0
  form.MAIL_SSL_PORT = mailConfig.value.MAIL_SSL_PORT.value || 0
}

const isUpdate = ref(false)
// 修改
const onUpdate = () => {
  isUpdate.value = true
}

// 取消
const handleCancel = () => {
  reset()
  isUpdate.value = false
}

const queryForm = {
  category: 'MAIL',
}
// 查询列表数据
const getDataList = async () => {
  loading.value = true
  const { data } = await listOption(queryForm)
  mailConfig.value = data.reduce((obj: MailConfig, option: OptionResp) => {
    obj[option.code] = { ...option, value: ['MAIL_PORT', 'MAIL_SSL_PORT', 'MAIL_SSL_ENABLED'].includes(option.code) ? Number.parseInt(option.value) : option.value }
    return obj
  }, { ...mailConfig.value })
  handleCancel()
  loading.value = false
}

// 保存
const handleSave = async () => {
  const isInvalid = await formRef.value?.validate()
  if (isInvalid) return false
  await updateOption(
    Object.entries(form).map(([key, value]) => {
      let submitValue = value
      // 已配置密码且本次留空 → 回传脱敏占位，后端保留原密码
      if (key === 'MAIL_PASSWORD' && passwordConfigured.value && !String(value || '').trim()) {
        submitValue = PASSWORD_MASK
      }
      return { id: mailConfig.value[key].id, code: key, value: submitValue }
    }),
  )
  await getDataList()
  Message.success('保存成功')
}

// 恢复默认
const handleResetValue = async () => {
  await resetOptionValue(queryForm)
  Message.success('恢复成功')
  await getDataList()
}
const onResetValue = () => {
  Modal.warning({
    title: '警告',
    content: '确认恢复邮件配置为默认值吗？',
    hideCancel: false,
    maskClosable: false,
    onOk: handleResetValue,
  })
}

const onTestMail = () => {
  testForm.to = form.MAIL_USERNAME || ''
  testVisible.value = true
}

const handleSendTest = async () => {
  if (!testForm.to || !Regexp.Email.test(testForm.to)) {
    Message.warning('请输入正确的收件邮箱')
    return false
  }
  try {
    testLoading.value = true
    await sendTestMail(testForm.to)
    Message.success('测试邮件已发送，请查收收件箱（含垃圾箱）')
    return true
  } catch {
    return false
  } finally {
    testLoading.value = false
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

:deep(.form .arco-input-wrapper),
:deep(.arco-select-view-single) {
  width: 220px;
}
</style>
