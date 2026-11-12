<template>
  <div class="password-page">
    <van-nav-bar title="修改密码" :left-arrow="!requiredChange" @click-left="router.back()" />
    <section class="password-intro">
      <van-icon name="shield-o" size="32" color="#2563eb" />
      <h1>{{ requiredChange ? '请先更新登录密码' : '设置新的登录密码' }}</h1>
      <p>{{ requiredChange ? '当前密码为初始密码或已过期，修改后重新登录即可查看任务。' : '修改成功后需使用新密码重新登录。' }}</p>
    </section>
    <van-form @submit="submit" class="password-form">
      <van-cell-group inset>
        <van-field v-model="form.oldPassword" type="password" label="当前密码" placeholder="请输入当前密码" autocomplete="current-password" clearable :rules="[{ required: true, message: '请输入当前密码' }]" />
        <van-field v-model="form.newPassword" type="password" label="新密码" placeholder="请输入新密码" autocomplete="new-password" clearable :rules="[{ required: true, message: '请输入新密码' }, { validator: value => value !== form.oldPassword, message: '新密码不能与当前密码相同' }]" />
        <van-field v-model="form.confirmPassword" type="password" label="确认密码" placeholder="请再次输入新密码" autocomplete="new-password" clearable :rules="[{ required: true, message: '请再次输入新密码' }, { validator: value => value === form.newPassword, message: '两次输入的新密码不一致' }]" />
      </van-cell-group>
      <div class="password-actions">
        <van-button block round type="primary" native-type="submit" :loading="saving">确认修改</van-button>
        <van-button block round plain native-type="button" :disabled="saving" @click="leave">退出登录</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { updatePassword, logout } from '@/api/engineer'
import { encryptTransport } from '@/utils/encrypt'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)
const requiredChange = computed(() => Boolean(userStore.engineerInfo?.pwdExpired))
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
let active = true
function clearForm() { form.oldPassword = ''; form.newPassword = ''; form.confirmPassword = '' }
onBeforeUnmount(() => { active = false; clearForm() })

async function submit() {
  if (saving.value) return
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) { showFailToast('请完整填写密码'); return }
  if (form.oldPassword === form.newPassword) { showFailToast('新密码不能与当前密码相同'); return }
  if (form.newPassword !== form.confirmPassword) { showFailToast('两次输入的新密码不一致'); return }
  const token = userStore.engineerToken, generation = userStore.engineerSessionVersion
  saving.value = true
  try {
    const currentValue = form.oldPassword, nextValue = form.newPassword
    const oldPassword = await encryptTransport(currentValue)
    const newPassword = await encryptTransport(nextValue)
    if (!active || userStore.engineerToken !== token || userStore.engineerSessionVersion !== generation) return
    await updatePassword({ oldPassword, newPassword })
    if (!active || userStore.engineerToken !== token || userStore.engineerSessionVersion !== generation) return
    clearForm()
    userStore.clearEngineerToken()
    showSuccessToast('密码已修改，请重新登录')
    router.replace('/engineer/login')
  } catch {
    // The platform interceptor displays the specific old-password or policy error.
  } finally { saving.value = false }
}

async function leave() {
  if (saving.value) return
  saving.value = true
  const token = userStore.engineerToken, generation = userStore.engineerSessionVersion
  try { await logout() } catch { /* Local logout remains available when offline. */ }
  finally {
    if (userStore.engineerToken === token && userStore.engineerSessionVersion === generation) {
      clearForm(); userStore.clearEngineerToken(); router.replace('/engineer/login')
    }
    saving.value = false
  }
}
</script>

<style scoped>
.password-page { min-height: 100vh; background: #f5f7fb; }
.password-intro { padding: 28px 24px 22px; text-align: center; }
.password-intro h1 { margin: 12px 0 8px; color: #172b4d; font-size: 20px; }
.password-intro p { margin: 0 auto; max-width: 360px; color: #6b778c; font-size: 14px; line-height: 1.7; }
.password-form { max-width: 480px; margin: 0 auto; }
.password-actions { display: grid; gap: 12px; margin: 24px 16px; }
</style>
