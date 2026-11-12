/**
 * Axios 请求实例
 * 统一：平台 R（code===0）、Token 头、错误处理
 */
import axios from 'axios'
import { showToast, showFailToast } from 'vant'
import { useUserStore } from '@/store/user'
import { isEngineerPasswordExpired } from '@/utils/engineerPassword'

const pendingMap = new Map()
let passwordRedirectPending = false

function handleEngineerPasswordExpiry(config, body) {
  if (!isEngineerPasswordExpired(body)) return false
  const session = config?.authSession
  const store = useUserStore()
  if (session?.kind !== 'engineer' || !session.token || store.engineerToken !== session.token || store.engineerSessionVersion !== session.version) return false
  store.updateEngineerInfo({ ...(store.engineerInfo || {}), pwdExpired: true })
  if (!window.location.pathname.endsWith('/engineer/password') && !passwordRedirectPending) {
    passwordRedirectPending = true
    window.location.replace('/engineer/password')
  }
  return true
}

function generateCancelKey(config) {
  const method = (config.method || '').toLowerCase()
  const url = config.url || ''
  let paramsData = ''
  const data = config.data
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    // FormData 无法稳定序列化；每次上传独立，避免互相 abort
    paramsData = `formData:${Date.now()}:${Math.random()}`
  } else if (method === 'get') {
    paramsData = JSON.stringify(config.params || {})
  } else if (method === 'post' || method === 'put') {
    paramsData = JSON.stringify(data || {})
  }
  return `${method}&${url}&${paramsData}`
}

function removePendingKey(config) {
  if (config.cancelKey && pendingMap.has(config.cancelKey)) {
    pendingMap.delete(config.cancelKey)
  }
}

/** 平台成功：code===0 或 success===true */
function isBizSuccess(res) {
  if (!res || typeof res !== 'object') return false
  if (res.success === true) return true
  if (res.code === 0 || res.code === '0') return true
  return false
}

// Only the credential actually sent by this request may expire its own identity.
function expireRequestSession(config, customerOnly = false) {
  const session = config?.authSession
  if (!session || (customerOnly && session.kind !== 'customer')) return false
  const store = useUserStore()
  const customer = session.kind === 'customer'
  const tokenKey = customer ? 'h5Token' : 'engineerToken'
  const versionKey = customer ? 'h5SessionVersion' : 'engineerSessionVersion'
  if (!session.token || store[tokenKey] !== session.token || store[versionKey] !== session.version) return false
  if (customer) store.clearH5Token()
  else store.clearEngineerToken()
  const clearedVersion = store[versionKey]
  setTimeout(() => {
    if (!store[tokenKey] && store[versionKey] === clearedVersion) {
      window.location.href = customer ? '/customer/auth' : '/engineer/login'
    }
  }, 1500)
  return true
}

const service = axios.create({
  baseURL: '/',
  timeout: 30000
})

service.interceptors.request.use(
  config => {
    const userStore = useUserStore()

    const method = (config.method || '').toLowerCase()
    const isBlobGet = method === 'get' && config.responseType === 'blob'
    if ((method === 'get' || method === 'post' || method === 'put') && !isBlobGet) {
      const cancelKey = generateCancelKey(config)
      config.cancelKey = cancelKey
      if (pendingMap.has(cancelKey)) {
        const controller = pendingMap.get(cancelKey)
        controller.abort()
        pendingMap.delete(cancelKey)
      }
      const controller = new AbortController()
      config.signal = controller.signal
      pendingMap.set(cancelKey, controller)
    }

    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      if (config.headers) {
        delete config.headers['Content-Type']
        delete config.headers['content-type']
      }
      if (!config.timeout || config.timeout === 30000) {
        config.timeout = 60000
      }
    }

    const isTokenRequired = config.headers?.isToken !== false

    // 客户 H5：h5-token
    if (isTokenRequired && config.url && config.url.startsWith('/eoms/h5/') && !config.url.startsWith('/eoms/h5/auth/')) {
      if (userStore.h5Token) {
        config.headers['h5-token'] = userStore.h5Token
      }
    }

    // EOMS 文件预览：h5-token（客户）或 Bearer（工程师/PC 代理）
    if (isTokenRequired && config.url && config.url.startsWith('/eoms/file/')) {
      if (userStore.h5Token) {
        config.headers['h5-token'] = userStore.h5Token
      } else if (userStore.engineerToken) {
        config.headers['Authorization'] = 'Bearer ' + userStore.engineerToken
      }
    }

    // 工程师：Authorization Bearer（与 PC 一致）
    if (isTokenRequired && config.url) {
      const needAuth =
        config.url.startsWith('/eoms/engineer/') ||
        config.url.startsWith('/user/profile/') ||
        config.url.startsWith('/auth/')
      if (needAuth && userStore.engineerToken) {
        config.headers['Authorization'] = 'Bearer ' + userStore.engineerToken
      }
    }

    delete config.authSession
    if (isTokenRequired && userStore.h5Token && config.headers['h5-token'] === userStore.h5Token) {
      config.authSession = { kind: 'customer', token: userStore.h5Token, version: userStore.h5SessionVersion }
    } else if (isTokenRequired && userStore.engineerToken && config.headers.Authorization === 'Bearer ' + userStore.engineerToken) {
      config.authSession = { kind: 'engineer', token: userStore.engineerToken, version: userStore.engineerSessionVersion }
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    removePendingKey(response.config)
    const res = response.data

    if (response.config.responseType === 'blob') {
      return response
    }

    if (res && res.code !== undefined) {
      if (isBizSuccess(res)) {
        return res
      }
      if (handleEngineerPasswordExpiry(response.config, res)) return Promise.reject(new Error(res.msg || '请先修改密码'))
      if (response.config.skipErrorToast) return Promise.reject(new Error(res.msg || '操作失败'))
      // H5 客户令牌失效
      if (res.code === 10007 || res.code === '10007') {
        expireRequestSession(response.config, true)
        showFailToast(res.msg || '令牌失效，请重新登录')
        return Promise.reject(new Error(res.msg || '令牌失效'))
      }
      // 未授权
      if (res.code === 401 || res.code === '401') {
        expireRequestSession(response.config)
        showFailToast(res.msg || '登录已过期，请重新登录')
        return Promise.reject(new Error(res.msg || '未授权'))
      }
      if (res.code === 403 || res.code === '403') {
        showToast(res.msg || '无权访问')
        return Promise.reject(new Error(res.msg || '无权访问'))
      }
      showFailToast(res.msg || '操作失败')
      return Promise.reject(new Error(res.msg || '操作失败'))
    }

    return res
  },
  error => {
    if (error.config) {
      removePendingKey(error.config)
    }
    if (axios.isCancel(error)) return Promise.reject(error)
    if (handleEngineerPasswordExpiry(error.config, error.response?.data)) return Promise.reject(error)
    // 预览等静默请求：不要弹 toast，也不要因 401 清掉 H5 登录态
    if (error.config?.skipErrorToast) {
      return Promise.reject(error)
    }
    if (error.response) {
      if (error.response.status === 401) {
        expireRequestSession(error.config)
        showFailToast('登录已过期，请重新登录')
        return Promise.reject(error)
      } else if (error.response.status === 403) {
        showToast('无权访问')
        return Promise.reject(error)
      }
    }
    let msg = error.message || '网络异常'
    if (msg.includes('timeout')) {
      msg = '请求超时，请稍后重试'
    } else if (msg.includes('Network')) {
      msg = '网络异常，请检查网络连接'
    } else if (msg.includes('abort')) {
      if (error.config?.responseType === 'blob' || error.config?.skipErrorToast) {
        return Promise.reject(error)
      }
      msg = '请求已取消'
    }
    if (error.config?.skipErrorToast) {
      return Promise.reject(error)
    }
    showFailToast(msg)
    return Promise.reject(error)
  }
)

export default service
