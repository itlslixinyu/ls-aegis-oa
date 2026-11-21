<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="logo-section">
        <SystemLogo :src="systemStore.logoUrl" class="logo-img" />
        <SingleLineHeading v-if="systemStore.h5Title" class="title" :text="systemStore.h5Title" />
        <p class="subtitle">{{ mode === 'login' ? '客户服务端' : '客户账号激活' }}</p>
      </div>

      <van-form ref="formRef" @submit="onSubmit" class="form-section">
        <h3 class="form-heading">{{ mode === 'login' ? '账号登录' : '设置客户账号' }}</h3>
        <van-cell-group class="input-box-wrap">
          <van-field
            v-model.trim="username"
            name="username"
            autocomplete="username"
            placeholder="请输入用户名"
            clearable
            :rules="[
              { required: true, message: '请输入用户名', trigger: 'onBlur' }
            ]"
          />
        </van-cell-group>
        <van-cell-group v-if="mode === 'activate'" class="input-box-wrap">
          <van-field
            v-model.trim="invitationCode"
            name="invitationCode"
            type="text"
            inputmode="numeric"
            maxlength="8"
            placeholder="8位数字客户激活码"
            autocomplete="off"
            clearable
            :rules="[{ required: true, message: '请输入一次性邀请码', trigger: 'onBlur' }]"
          />
        </van-cell-group>
        <van-cell-group class="input-box-wrap">
          <van-field
            v-model="password"
            name="password"
            type="password"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            :placeholder="mode === 'login' ? '请输入密码' : '设置密码（8-32位英文字符，含字母和数字）'"
            clearable
            :rules="[{ required: true, message: '请输入密码', trigger: 'onBlur' }]"
          />
        </van-cell-group>
        <van-cell-group v-if="mode === 'activate'" class="input-box-wrap">
          <van-field
            v-model="confirmPassword"
            name="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入密码"
            clearable
            :rules="[{ required: true, message: '请再次输入密码', trigger: 'onBlur' }]"
          />
        </van-cell-group>
        <van-cell-group class="input-box-wrap captcha-wrap">
          <van-field
            v-model="captchaCode"
            name="captcha"
            placeholder="请输入验证码"
            clearable
            autocomplete="off"
            :rules="[{ required: true, message: '请输入验证码', trigger: 'onBlur' }]"
          >
            <template #button>
              <img
                v-if="captchaImg"
                :src="captchaImg"
                class="captcha-img"
                alt="验证码"
                @click="loadCaptcha"
              />
            </template>
          </van-field>
        </van-cell-group>
        <div class="submit-btn">
          <van-button round block plain type="primary" native-type="submit" :loading="loading" class="auth-submit-btn">
            {{ mode === 'login' ? '登录客户门户' : '激活客户账号' }}
          </van-button>
        </div>
        <div v-if="mode === 'login'" class="auth-links">
          <button type="button" class="auth-link" @click="setMode('activate')">首次使用</button>
          <button type="button" class="auth-link" @click="showPasswordHelp">忘记密码</button>
        </div>
        <div v-else class="auth-links auth-links--single">
          <button type="button" class="auth-link" @click="setMode('login')">返回登录</button>
        </div>
      </van-form>
    </div>

    <!-- 版权及备案信息 -->
    <div class="beian-footer">
      <div class="copyright" v-if="systemStore.footerContent">{{ systemStore.footerContent }}</div>
      <div v-if="systemStore.icpNumber" class="beian-row">
        <a href="https://beian.miit.gov.cn" target="_blank" rel="noopener" class="beian-link">{{ systemStore.icpNumber }}</a>
      </div>
      <div v-if="systemStore.publicSecurityNumber" class="beian-row">
        <a href="https://beian.mps.gov.cn" target="_blank" rel="noopener" class="beian-link">
          <img :src="policeBadge" class="police-badge" alt="公安备案" />{{ systemStore.publicSecurityNumber }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showFailToast, showSuccessToast } from 'vant'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'
import policeBadge from '@/assets/images/police-badge.png'
import { activateAccount, getImageCaptcha, passwordLogin } from '@/api/customer'
import { toBizId } from '@/utils/biz-id'
import { encryptTransport } from '@/utils/encrypt'
import SystemLogo from '@/components/SystemLogo.vue'
import SingleLineHeading from '@/components/SingleLineHeading.vue'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

const formRef = ref(null)
const mode = ref('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const invitationCode = ref('')
const captchaCode = ref('')
const captchaUuid = ref('')
const captchaImg = ref('')
const loading = ref(false)

async function loadCaptcha() {
  try {
    const res = await getImageCaptcha()
    const data = res?.data
    if (data?.uuid && data?.img) {
      captchaUuid.value = data.uuid
      captchaImg.value = data.img
      captchaCode.value = ''
    }
  } catch (e) {
    captchaImg.value = ''
  }
}

onMounted(() => {
  if (userStore.h5Token) {
    window.location.replace('/customer/workbench')
    return
  }
  loadCaptcha()
})

function setMode(nextMode) {
  mode.value = nextMode
  password.value = ''
  confirmPassword.value = ''
  invitationCode.value = ''
  loadCaptcha()
}

function showPasswordHelp() {
  showDialog({
    title: '忘记密码',
    message: '请联系平台管理员重新签发客户激活码，再用原用户名设置新密码。'
  })
}

async function onSubmit() {
  username.value = username.value ? username.value.trim() : ''
  invitationCode.value = invitationCode.value ? invitationCode.value.trim() : ''
  if (mode.value === 'activate' && password.value !== confirmPassword.value) {
    showFailToast('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const encryptedPassword = await encryptTransport(password.value)
    const payload = mode.value === 'activate'
      ? { invitationCode: invitationCode.value, username: username.value, password: encryptedPassword, captcha: captchaCode.value, uuid: captchaUuid.value }
      : { username: username.value, password: encryptedPassword, captcha: captchaCode.value, uuid: captchaUuid.value }
    const res = mode.value === 'activate'
      ? await activateAccount(payload)
      : await passwordLogin(payload)
    const data = res?.data
    if (data?.token) {
      userStore.setH5Token(data.token, {
        customerId: toBizId(data.customerId),
        tenantId: toBizId(data.tenantId),
        customerNo: data.customerNo,
        customerName: data.customerName
      })
      invitationCode.value = ''
      password.value = ''
      confirmPassword.value = ''
      showSuccessToast(mode.value === 'activate' ? `账号 ${username.value} 已激活` : '登录成功')
      // 登录后先进入客户工作台，再由按钮进入报修列表。
      window.location.replace('/customer/workbench')
    }
  } catch (e) {
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; flex-direction: column; justify-content: space-between; overflow: hidden; position: relative; background: radial-gradient(ellipse at 90% 0%, #e4edff 0, transparent 34%), linear-gradient(160deg, #f5f7fb 0%, #eef2f8 100%); }
.auth-page::before { content: ''; position: absolute; width: 250px; height: 250px; border-radius: 50%; right: -115px; top: -120px; background: rgba(39, 92, 190, .08); }
.auth-container { width: min(100%, 480px); margin: auto; padding: max(26px, env(safe-area-inset-top)) 24px 16px; position: relative; z-index: 1; }
.logo-section { text-align: center; padding: 6px 4px 20px; }
.logo-section .logo-img { width: 104px; height: 104px; object-fit: contain; display: block; margin: 0 auto 12px; }
.logo-section .title { font-size: 23px; line-height: 1.25; letter-spacing: -.3px; font-weight: 750; color: #18243a; margin: 0 0 5px; text-align: center; }
.logo-section .subtitle { color: #65738b; font-size: 14px; font-weight: 600; margin: 0; text-align: center; }
.form-section { background: rgba(255,255,255,.96); border: 1px solid rgba(227,232,241,.9); border-radius: 24px; padding: 20px 18px 14px; box-shadow: 0 18px 48px rgba(35,54,86,.10); }
.form-heading { color: #24324c; text-align: center; font-size: 18px; font-weight: 700; margin: 0 0 16px; }
.form-section .input-box-wrap { margin-bottom: 12px; border: 1px solid #e9edf4; border-radius: 14px; background: #f8f9fc; overflow: hidden; }
.form-section .input-box-wrap:focus-within { border-color: #4e76d9; box-shadow: 0 0 0 3px rgba(78,118,217,.1); }
.form-section :deep(.van-cell) { padding: 12px 14px; background: transparent; min-height: 48px; }
.form-section :deep(.van-cell::after) { display: none; }
.form-section :deep(.van-field__control) { color: #25324a; font-size: 15px; }
.form-section :deep(.van-field__control::placeholder) { color: #9aa4b4; }
.form-section :deep(.van-field__clear-icon) { color: #9aa4b4; }
.captcha-wrap :deep(.van-field__button) { padding-left: 8px; }
.captcha-img { width: 90px; height: 32px; display: block; border-radius: 8px; cursor: pointer; }
.submit-btn { margin-top: 18px; }
.form-section :deep(.auth-submit-btn) { height: 50px; border: 0; color: #fff; background: linear-gradient(100deg,#3262ce,#597fe0); font-size: 16px; font-weight: 650; box-shadow: 0 8px 18px rgba(54,95,197,.23); }
.form-section :deep(.auth-submit-btn::before) { display: none; }
.auth-links { display: flex; align-items: center; justify-content: space-between; margin: 10px 2px 0; }
.auth-links--single { justify-content: center; }
.auth-link { border: 0; background: transparent; color: #456ac2; font: inherit; font-size: 13px; font-weight: 600; padding: 8px 4px; cursor: pointer; }
.beian-footer { flex-shrink: 0; text-align: center; padding: 8px 18px max(12px, env(safe-area-inset-bottom)); color: #9aa4b4; font-size: 10px; line-height: 1.55; position: relative; z-index: 1; }
.beian-footer .copyright { margin-bottom: 2px; }
.beian-row { min-height: 16px; }
.beian-footer .beian-link { color: #8792a5; display: inline-flex; align-items: center; }
.beian-footer .police-badge { width: 13px; height: 13px; margin-right: 3px; }
@media (max-height: 700px) { .auth-container { padding-top: 14px; } .logo-section { padding-top: 0; padding-bottom: 12px; } .logo-section .logo-img { width: 78px; height: 78px; margin-bottom: 8px; } }
</style>
