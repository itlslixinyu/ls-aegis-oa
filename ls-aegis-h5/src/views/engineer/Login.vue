<template>
  <div class="page">

    <!-- 品牌与工程师端标题 -->
    <div class="auth-container">
      <div class="logo-section">
        <SystemLogo :src="systemStore.logoUrl" class="logo-img" />
        <SingleLineHeading v-if="systemStore.h5Title" class="title" :text="systemStore.h5Title" />
        <p class="subtitle">工程师端</p>
      </div>

      <van-form ref="formRef" @submit="onSubmit" @failed="onFailed" class="form-section">
        <h3 class="form-heading">工程师登录</h3>

        <van-cell-group class="input-box-wrap">
          <van-field
            v-model="form.username"
            name="username"
            placeholder="请输入工程师账号"
            clearable
            autocomplete="username"
            :rules="[
              { required: true, message: '请输入工程师账号', trigger: 'onBlur' },
              { validator: (v) => v && v.trim() && v.trim().length >= 2, message: '账号至少2位', trigger: 'onBlur' }
            ]"
          />
        </van-cell-group>

        <van-cell-group class="input-box-wrap">
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            placeholder="请输入工程师密码"
            show-password
            autocomplete="current-password"
            :rules="[{ required: true, message: '请输入密码', trigger: 'onBlur' }]"
          />
        </van-cell-group>

        <van-cell-group v-if="captchaEnabled" class="input-box-wrap captcha-box-wrap">
          <van-field
            v-model="form.code"
            name="code"
            placeholder="请输入验证码"
            maxlength="5"
          >
            <template #button>
              <img
                v-if="codeUrl"
                :src="codeUrl"
                alt="验证码"
                class="code-img"
                @click="refreshCode"
                @error="onCodeError"
              />
              <van-button
                v-else
                size="small"
                type="primary"
                plain
                :loading="codeLoading"
                class="code-refresh-btn"
                @click="refreshCode"
              >
                刷新
              </van-button>
            </template>
          </van-field>
        </van-cell-group>

        <div class="submit-btn">
          <van-button round block plain type="primary" native-type="submit" :loading="loading" class="engineer-login-submit-btn">
            登 录
          </van-button>
        </div>
      </van-form>

    </div>

    <!-- 版权及备案信息（与客户Auth.vue一致位置：最底部） -->
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { login, getInfo, getCodeImg } from '@/api/engineer'
import { encryptTransport, loadGmPublicKey } from '@/utils/encrypt'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'
import policeBadge from '@/assets/images/police-badge.png'
import SystemLogo from '@/components/SystemLogo.vue'
import SingleLineHeading from '@/components/SingleLineHeading.vue'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

const formRef = ref(null)
const codeUrl = ref('')
const codeLoading = ref(false)
const loading = ref(false)
// 验证码开关（后端 sys_config.sys.account.captchaEnabled 控制）
// 初始 false，等 refreshCode 请求返回后根据后端配置更新，避免请求期间显示空验证码字段
const captchaEnabled = ref(false)

const form = reactive({
  username: '',
  password: '',
  code: '',
  uuid: ''
})

/** 刷新验证码：平台 GET /captcha/image → R.data */
function refreshCode() {
  codeLoading.value = true
  codeUrl.value = ''
  getCodeImg()
    .then(res => {
      const data = res?.data ?? res
      // 平台字段 isEnabled；兼容旧 captchaEnabled
      captchaEnabled.value = data.isEnabled === undefined
        ? (data.captchaEnabled === undefined ? true : data.captchaEnabled)
        : data.isEnabled
      if (captchaEnabled.value && data.img) {
        const img = data.img.startsWith('data:') ? data.img : data.img
        codeUrl.value = img.startsWith('data:') ? img : data.img
        form.uuid = data.uuid
      } else {
        form.code = ''
        form.uuid = ''
      }
    })
    .catch(() => {
      captchaEnabled.value = false
      codeUrl.value = ''
      form.code = ''
      form.uuid = ''
    })
    .finally(() => {
      codeLoading.value = false
    })
}

/** 验证码图片加载失败兜底（base64 损坏或解码异常） */
function onCodeError() {
  codeUrl.value = ''
}

/** 表单校验失败处理（Vant van-form @failed 事件） */
function onFailed(errorInfo) {
  // 校验失败时显示第一个错误信息
  const firstError = errorInfo && errorInfo.errors && errorInfo.errors[0]
  if (firstError && firstError.message) {
    showFailToast(firstError.message)
  }
}

/** 登录：平台 /auth/login + SM2 传输加密 */
async function onSubmit() {
  if (loading.value) return
  loading.value = true
  let acquiredToken = ''
  let acquiredVersion = -1
  try {
    form.username = form.username ? form.username.trim() : ''
    form.password = form.password ? form.password.trim() : ''
    const payload = {
      username: form.username,
      password: await encryptTransport(form.password),
      captcha: captchaEnabled.value ? form.code : '',
      uuid: captchaEnabled.value ? form.uuid : ''
    }
    const res = await login(payload)
    const loginData = res?.data ?? res
    const token = loginData?.token
    if (!token) {
      showFailToast('登录返回异常')
      return
    }
    userStore.setEngineerToken(token, null)
    acquiredToken = token
    acquiredVersion = userStore.engineerSessionVersion
    const infoRes = await getInfo()
    if (userStore.engineerToken !== token || userStore.engineerSessionVersion !== acquiredVersion) return
    const info = infoRes?.data ?? infoRes
    if (info && (info.username || info.id)) {
      // 兼容 Profile/Tasks 旧字段 user.nickName / user.userName
      userStore.setEngineerToken(token, {
        user: {
          userName: info.username,
          nickName: info.nickname || info.username,
          avatar: info.avatar,
          dept: info.deptName ? { deptName: info.deptName } : undefined
        },
        roles: info.roles || [],
        permissions: info.permissions || []
      })
      showSuccessToast('登录成功')
      // 登录后先进入维修工作台，再由状态按钮进入任务列表。
      window.location.replace('/engineer/workbench')
    } else {
      userStore.clearEngineerToken()
      showFailToast('获取用户信息失败')
    }
  } catch (e) {
    if (acquiredToken && userStore.engineerToken === acquiredToken && userStore.engineerSessionVersion === acquiredVersion) {
      userStore.clearEngineerToken()
    }
    if (captchaEnabled.value) {
      refreshCode()
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (userStore.engineerToken) {
    window.location.replace('/engineer/workbench')
    return
  }
  await loadGmPublicKey()
  refreshCode()
})
</script>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; position: relative; overflow: hidden; background: radial-gradient(ellipse at 90% 0%, #e4edff 0, transparent 34%), linear-gradient(160deg,#f5f7fb,#eef2f8); }
.page::before { content: ''; position: absolute; width: 250px; height: 250px; border-radius: 50%; right: -115px; top: -120px; background: rgba(39,92,190,.08); }
.auth-container { width: min(100%,480px); margin: auto; padding: max(26px,env(safe-area-inset-top)) 24px 16px; position: relative; z-index: 1; }
.logo-section { text-align: center; padding: 6px 4px 20px; }
.logo-section .logo-img { width: 104px; height: 104px; object-fit: contain; display: block; margin: 0 auto 12px; }
.logo-section .title { font-size: 23px; line-height: 1.25; letter-spacing: -.3px; font-weight: 750; color: #18243a; margin: 0 0 5px; text-align: center; }
.logo-section .subtitle { color: #65738b; font-size: 14px; font-weight: 600; margin: 0; text-align: center; }
.form-section { background: rgba(255,255,255,.96); border: 1px solid rgba(227,232,241,.9); border-radius: 24px; padding: 20px 18px 16px; box-shadow: 0 18px 48px rgba(35,54,86,.10); }
.form-heading { color: #24324c; text-align: center; font-size: 18px; font-weight: 700; margin: 0 0 16px; }
.form-section .input-box-wrap { margin-bottom: 12px; border: 1px solid #e9edf4; border-radius: 14px; background: #f8f9fc; overflow: hidden; }
.form-section .input-box-wrap:focus-within { border-color: #4e76d9; box-shadow: 0 0 0 3px rgba(78,118,217,.1); }
.form-section :deep(.van-cell) { padding: 12px 14px; background: transparent; min-height: 48px; }
.form-section :deep(.van-cell::after) { display: none; }
.form-section :deep(.van-field__control) { color: #25324a; font-size: 15px; }
.form-section :deep(.van-field__control::placeholder) { color: #9aa4b4; }
.form-section :deep(.van-field__right-icon) { color: #77849b; }
.captcha-box-wrap :deep(.van-field__button) { margin-left: 8px; flex-shrink: 0; }
.code-img { width: 90px; height: 32px; display: block; border-radius: 8px; background: #fff; cursor: pointer; }
.code-refresh-btn { border-radius: 8px; }
.submit-btn { margin: 18px 0 8px; }
.form-section :deep(.engineer-login-submit-btn) { height: 50px; border: 0; color: #fff; background: linear-gradient(100deg,#3262ce,#597fe0); font-size: 16px; font-weight: 650; box-shadow: 0 8px 18px rgba(54,95,197,.23); }
.form-section :deep(.engineer-login-submit-btn::before) { display: none; }
.beian-footer { text-align: center; padding: 8px 16px max(12px,env(safe-area-inset-bottom)); margin-top: auto; position: relative; z-index: 1; color: #9aa4b4; font-size: 10px; line-height: 1.55; }
.beian-footer .copyright { font-size: 10px; color: #8792a5; line-height: 1.55; margin-bottom: 2px; }
.beian-row { min-height: 16px; }
.beian-footer .beian-link { color: #8792a5; display: inline-flex; align-items: center; }
.beian-footer .beian-link:active { color: #456ac2; }
.beian-footer .police-badge { width: 13px; height: 13px; margin-right: 3px; }
@media (max-height:700px) { .auth-container { padding-top: 14px; } .logo-section { padding-top: 0; padding-bottom: 12px; } .logo-section .logo-img { width: 78px; height: 78px; margin-bottom: 8px; } }
</style>
