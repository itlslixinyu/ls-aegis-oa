<template>
  <div class="login-page" :class="{ desktop: isDesktop, mobile: !isDesktop }">
    <header class="login-header">
      <div class="brand">
        <div class="brand-logo">
          <img :src="logo || '/brand-logo.png'" alt="logo" />
        </div>
        <div class="brand-text">
          <span v-if="company" class="company">{{ company }}</span>
          <span v-if="company && title" class="divider" />
          <span v-if="title" class="product">{{ title }}</span>
        </div>
      </div>
      <div v-if="isDesktop" class="clock-pill">
        <icon-clock-circle />
        <span>{{ nowText }}</span>
      </div>
    </header>

    <main class="login-main">
      <div class="login-card">
        <aside v-if="isDesktop" class="login-card__left">
          <div class="hero-illust" aria-hidden="true">
            <svg viewBox="0 0 200 200" class="shield">
              <defs>
                <linearGradient id="pwdShieldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95" />
                  <stop offset="100%" stop-color="#d6e8ff" stop-opacity="0.85" />
                </linearGradient>
              </defs>
              <circle cx="40" cy="50" r="4" fill="#fff" opacity="0.55" />
              <circle cx="170" cy="70" r="3" fill="#fff" opacity="0.45" />
              <circle cx="155" cy="150" r="5" fill="#fff" opacity="0.35" />
              <path
                d="M100 28 L148 48 V98 C148 132 128 158 100 172 C72 158 52 132 52 98 V48 Z"
                fill="url(#pwdShieldGrad)"
              />
              <path
                d="M78 102 L94 118 L128 80"
                fill="none"
                stroke="#1e6fff"
                stroke-width="10"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <h2 class="hero-title">账号安全加固</h2>
          <p class="hero-sub">请修改初始或过期密码后继续使用</p>
          <ul class="hero-points">
            <li><icon-safe /> 定期更换强口令</li>
            <li><icon-lock /> 国密传输保护</li>
            <li><icon-user /> 禁止复用旧密码</li>
          </ul>
        </aside>

        <section class="login-card__right">
          <div class="login-form-wrap">
            <h3 class="form-title">修改密码</h3>
            <p class="form-tip">密码已过期或为初始密码，请先修改后再使用系统</p>
            <div class="login-form-body">
              <ModifyPassword />
            </div>
          </div>
        </section>
      </div>
    </main>

    <footer class="login-footer">
      <div v-if="copyright" class="copyright">{{ copyright }}</div>
      <div class="beian-row">
        <a v-if="beianIcp" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ beianIcp }}</a>
        <a
          v-if="beianGongan"
          class="gongan"
          href="https://www.beian.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
        >
          <img :src="beianGonganIcon" alt="公安备案" />
          <span>{{ beianGongan }}</span>
        </a>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import ModifyPassword from '../components/modifyPassword/index.vue'
import { useAppStore } from '@/stores'
import { useDevice } from '@/hooks'

defineOptions({ name: 'PwdExpired' })

const { isDesktop } = useDevice()
const appStore = useAppStore()

const logo = computed(() => appStore.getLogo())
const title = computed(() => appStore.getTitle())
const company = computed(() => appStore.getCompany())
const copyright = computed(() => appStore.getCopyright())
const beianIcp = computed(() => appStore.getForRecord())
const beianGongan = computed(() => appStore.getBeianGongan())
const beianGonganIcon = computed(() => appStore.getBeianGonganIcon())

const nowText = ref('')
let clockTimer: ReturnType<typeof setInterval> | undefined
const weekMap = ['日', '一', '二', '三', '四', '五', '六']

const formatNow = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 星期${weekMap[d.getDay()]} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(() => {
  nowText.value = formatNow()
  clockTimer = setInterval(() => {
    nowText.value = formatNow()
  }, 1000)
})

onUnmounted(() => {
  if (clockTimer) {
    clearInterval(clockTimer)
  }
})
</script>

<style scoped lang="scss">
.login-page {
  --brand-blue: #1e6fff;
  --brand-blue-deep: #0d4fd6;
  --brand-blue-soft: #3b8bff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #fff;
  background:
    radial-gradient(1200px 600px at 20% -10%, rgba(255, 255, 255, 0.18), transparent 55%),
    radial-gradient(900px 500px at 90% 10%, rgba(90, 170, 255, 0.35), transparent 50%),
    linear-gradient(160deg, #1a5fff 0%, #1e6fff 45%, #0f4fd4 100%);
}

.login-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 100px 100px 0;
  z-index: 2;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-logo {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  img {
    width: 48px;
    height: 48px;
    object-fit: contain;
  }
}

.brand-text {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.company {
  white-space: nowrap;
}

.divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.55);
  flex-shrink: 0;
}

.product {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clock-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px 16px;
  z-index: 2;
}

.login-card {
  width: min(1000px, 100%);
  min-height: 520px;
  display: grid;
  grid-template-columns: 6fr 4fr;
  border-radius: 18px;
  overflow: hidden;
  background: #1a62ef;
  box-shadow: 0 24px 60px rgba(8, 40, 110, 0.28);
  isolation: isolate;
}

.login-card__left {
  padding: 48px 40px;
  background:
    radial-gradient(420px 280px at 50% 18%, rgba(255, 255, 255, 0.16), transparent 60%),
    linear-gradient(165deg, #1f73ff 0%, #1a62ef 55%, #1554d8 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #fff;
  margin-right: -1px;
}

.hero-illust {
  width: 200px;
  height: 200px;
  margin-bottom: 28px;

  .shield {
    width: 100%;
    height: 100%;
    filter: drop-shadow(0 12px 24px rgba(0, 30, 90, 0.28));
  }
}

.hero-title {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1.35;
}

.hero-sub {
  margin: 14px 0 36px;
  opacity: 0.92;
  font-size: 18px;
  letter-spacing: 0.02em;
}

.hero-points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  font-size: 18px;

  li {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }
}

.login-card__right {
  padding: 40px 42px 28px;
  background: #fff;
  color: #1d2129;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.login-form-wrap {
  width: 100%;
  max-width: 360px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.form-title {
  margin: 0 0 12px;
  font-size: 26px;
  font-weight: 700;
  color: #163a8a;
  text-align: center;
}

.form-tip {
  margin: 0 0 24px;
  text-align: center;
  font-size: 13px;
  line-height: 1.6;
  color: #f53f3f;
}

.login-form-body {
  width: 100%;

  :deep(.arco-btn-primary) {
    background: var(--brand-blue);
    border-color: var(--brand-blue);
  }

  :deep(.arco-btn-primary:hover) {
    background: var(--brand-blue-soft);
    border-color: var(--brand-blue-soft);
  }
}

.login-footer {
  z-index: 2;
  padding: 8px 20px 20px;
  text-align: center;
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  line-height: 1.7;
}

.beian-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;

  a {
    color: rgba(255, 255, 255, 0.88);
    text-decoration: none;

    &:hover {
      color: #fff;
      text-decoration: underline;
    }
  }

  .gongan {
    display: inline-flex;
    align-items: center;
    gap: 6px;

    img {
      width: 14px;
      height: 14px;
      object-fit: contain;
    }
  }
}

.login-page.mobile {
  .login-header {
    padding: 24px 24px 0;
  }

  .brand-text {
    font-size: 16px;
  }

  .clock-pill {
    font-size: 14px;
  }

  .login-card {
    grid-template-columns: 1fr;
    min-height: auto;
    width: min(440px, 100%);
    background: #fff;
  }

  .login-card__left {
    margin-right: 0;
  }

  .login-card__right {
    padding: 28px 22px 20px;
  }

  .form-title {
    font-size: 22px;
  }
}
</style>
