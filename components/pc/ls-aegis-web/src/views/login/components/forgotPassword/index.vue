<template>
  <a-form
    ref="formRef"
    :model="form"
    :rules="rules"
    :label-col-style="{ display: 'none' }"
    :wrapper-col-style="{ flex: 1 }"
    size="large"
    @submit="onSubmit"
  >
    <a-form-item field="email" hide-label>
      <a-input v-model="form.email" placeholder="请输入绑定邮箱" allow-clear />
    </a-form-item>
    <a-form-item field="captcha" hide-label>
      <a-input v-model="form.captcha" placeholder="请输入邮箱验证码" :max-length="6" allow-clear style="flex: 1 1" />
      <a-button
        class="captcha-btn"
        :loading="captchaLoading"
        :disabled="captchaDisable"
        size="large"
        @click="onCaptcha"
      >
        {{ captchaBtnName }}
      </a-button>
    </a-form-item>
    <a-form-item field="newPassword" hide-label>
      <a-input-password
        v-model="form.newPassword"
        placeholder="请输入新密码"
        allow-clear
        autocomplete="new-password"
      />
    </a-form-item>
    <a-form-item field="confirmPassword" hide-label>
      <a-input-password
        v-model="form.confirmPassword"
        placeholder="请再次输入新密码"
        allow-clear
        autocomplete="new-password"
      />
    </a-form-item>
    <a-form-item>
      <a-space direction="vertical" fill class="w-full">
        <a-button class="btn" type="primary" :loading="loading" html-type="submit" size="large" long>
          重置密码
        </a-button>
        <a-link class="back-login" @click="router.push('/login')">返回登录</a-link>
      </a-space>
    </a-form-item>
    <Verify
      ref="VerifyRef"
      :captcha-type="captchaType"
      :mode="captchaMode"
      :img-size="{ width: '330px', height: '155px' }"
      @success="getCaptcha"
    />
  </a-form>
</template>

<script setup lang="ts">
import { type FormInstance, Message } from '@arco-design/web-vue'
import { type BehaviorCaptchaReq, forgotPassword, getEmailCaptcha } from '@/apis'
import { encryptTransport } from '@/utils/encrypt'
import * as Regexp from '@/utils/regexp'

defineOptions({ name: 'ForgotPasswordForm' })

const formRef = ref<FormInstance>()
const form = reactive({
  email: '',
  captcha: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormInstance['rules'] = {
  email: [
    { required: true, message: '请输入邮箱' },
    { match: Regexp.Email, message: '请输入正确的邮箱' },
  ],
  captcha: [{ required: true, message: '请输入验证码' }],
  newPassword: [{ required: true, message: '请输入新密码' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (value, cb) => {
        return new Promise((resolve) => {
          if (value !== form.newPassword) {
            cb('两次密码不一致')
          }
          resolve(true)
        })
      },
    },
  ],
}

const router = useRouter()
const loading = ref(false)

const VerifyRef = ref<InstanceType<any>>()
const captchaType = ref('blockPuzzle')
const captchaMode = ref('pop')
const captchaLoading = ref(false)
const captchaTimer = ref()
const captchaTime = ref(60)
const captchaBtnName = ref('获取验证码')
const captchaDisable = ref(false)

const resetCaptcha = () => {
  window.clearInterval(captchaTimer.value)
  captchaTime.value = 60
  captchaBtnName.value = '获取验证码'
  captchaDisable.value = false
}

const onCaptcha = async () => {
  if (captchaLoading.value) return
  const isInvalid = await formRef.value?.validateField('email')
  if (isInvalid) return
  VerifyRef.value.instance.refresh()
  VerifyRef.value.show()
}

const getCaptcha = async (captchaReq: BehaviorCaptchaReq) => {
  try {
    captchaLoading.value = true
    captchaBtnName.value = '发送中...'
    await getEmailCaptcha(form.email, captchaReq, 'forgot')
    captchaDisable.value = true
    captchaBtnName.value = `获取验证码(${(captchaTime.value -= 1)}s)`
    Message.success('验证码已发送，请查收邮箱')
    captchaTimer.value = window.setInterval(() => {
      captchaTime.value -= 1
      captchaBtnName.value = `获取验证码(${captchaTime.value}s)`
      if (captchaTime.value <= 0) {
        resetCaptcha()
      }
    }, 1000)
  } catch {
    resetCaptcha()
  } finally {
    captchaLoading.value = false
  }
}

const onSubmit = async () => {
  const isInvalid = await formRef.value?.validate()
  if (isInvalid) return
  try {
    loading.value = true
    await forgotPassword({
      email: form.email,
      captcha: form.captcha,
      newPassword: (await encryptTransport(form.newPassword)) || '',
    })
    Message.success('密码重置成功，请使用新密码登录')
    await router.replace({ path: '/login' })
  } catch {
    form.captcha = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.arco-input-wrapper,
:deep(.arco-select-view-single) {
  height: 40px;
  border-radius: 4px;
  font-size: 13px;
}

.arco-input-wrapper.arco-input-error {
  background-color: rgb(var(--danger-1));
  border-color: rgb(var(--danger-3));
}

.arco-input-wrapper.arco-input-error:hover {
  background-color: rgb(var(--danger-1));
  border-color: rgb(var(--danger-6));
}

.arco-input-wrapper :deep(.arco-input) {
  font-size: 13px;
  color: var(--color-text-1);
}

.arco-input-wrapper:hover {
  border-color: rgb(var(--arcoblue-6));
}

.captcha-btn {
  height: 40px;
  margin-left: 12px;
  min-width: 98px;
  border-radius: 4px;
}

.btn {
  height: 40px;
}

.back-login {
  justify-content: center;
  margin-top: 4px;
}
</style>
