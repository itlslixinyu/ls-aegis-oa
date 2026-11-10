import axios from 'axios'
import qs from 'query-string'
import type { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { useTenantStore } from '@/stores/modules/tenant'
import { useUserStore } from '@/stores'
import { getToken } from '@/utils/auth'
import modalErrorWrapper from '@/utils/modal-error-wrapper'
import messageErrorWrapper from '@/utils/message-error-wrapper'
import notificationErrorWrapper from '@/utils/notification-error-wrapper'
import router from '@/router'
import { createSessionExpiryHandler } from '@/utils/login-redirect'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 为 true 时不弹出全局错误提示（可选接口降级用） */
    skipErrorMessage?: boolean
  }
}

interface ICodeMessage {
  [propName: number]: string
}

const StatusCodeMessage: ICodeMessage = {
  200: '服务器成功返回请求的数据',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）',
  204: '删除数据成功',
  400: '请求错误(400)',
  401: '未授权，请重新登录(401)',
  403: '拒绝访问(403)',
  404: '请求出错(404)',
  408: '请求超时(408)',
  500: '服务器错误(500)',
  501: '服务未实现(501)',
  502: '网络错误(502)',
  503: '服务不可用(503)',
  504: '网络超时(504)',
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_PREFIX ?? import.meta.env.VITE_API_BASE_URL,
  timeout: 30 * 1000,
})

const handleError = (msg: string) => {
  if (msg.length >= 15) {
    return notificationErrorWrapper({
      content: msg || '服务器端错误',
      duration: 5 * 1000,
    })
  }
  return messageErrorWrapper({
    content: msg || '服务器端错误',
    duration: 5 * 1000,
  })
}

const handleSessionExpiry = createSessionExpiryHandler({
  getToken,
  route: () => router.currentRoute.value,
  logout: () => useUserStore().logoutCallBack(),
  replace: (location) => router.replace(location),
  prompt: (message, onOk) => modalErrorWrapper({
    title: '提示', content: message, maskClosable: false, escToClose: false, okText: '重新登录', onOk,
  }),
})

/** 与后端 SaTokenConfiguration 强制改密文案一致 */
const PWD_EXPIRED_MSG = '密码已过期，请修改密码'

/** 防止多个业务接口同时失败时重复跳转 / 弹窗 */
let pwdExpiredRedirecting = false

const redirectToPwdExpired = (msg: string) => {
  const userStore = useUserStore()
  userStore.userInfo.pwdExpired = true
  if (pwdExpiredRedirecting || router.currentRoute.value.path === '/pwdExpired') {
    return
  }
  pwdExpiredRedirecting = true
  handleError(msg)
  router.replace('/pwdExpired').finally(() => {
    pwdExpiredRedirecting = false
  })
}

// 请求拦截器
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const tenantStore = useTenantStore()
    if (tenantStore.tenantEnabled && tenantStore.tenantId) {
      config.headers['X-Tenant-Id'] = tenantStore.tenantId
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response

    if (response.request.responseType === 'blob') {
      const contentType = String(data?.type || '')
      // 业务失败时后端可能仍返回 JSON Blob
      if (contentType.includes('application/json')) {
        return (data as Blob).text().then((text: string) => {
          let parsed: { success?: boolean; code?: string | number; msg?: string } | null = null
          try {
            parsed = JSON.parse(text)
          } catch {
            // 原始文件也可能标记为 JSON；文件内容不是平台响应时原样下载。
            return response
          }
          if (parsed?.success === false && (typeof parsed.code === 'string' || typeof parsed.code === 'number')) {
            const msg = parsed.msg || '导出失败'
            handleError(msg)
            return Promise.reject(new Error(msg))
          }
          return response
        })
      }
      return response
    }

    const { success, code, msg } = data

    if (success) {
      return response
    }

    // Token 失效
    if (code === '401' && response.config.url !== '/auth/user/info') {
      handleSessionExpiry(response.config.headers.get('Authorization'), msg)
    } else if (msg === PWD_EXPIRED_MSG) {
      // 初始口令 / 口令过期：后端拒绝业务接口时踢回强制改密页
      redirectToPwdExpired(msg)
    } else if (!response.config.skipErrorMessage) {
      handleError(msg)
    }
    return Promise.reject(new Error(msg || '服务器端错误'))
  },
  async (error: AxiosError) => {
    const skipErrorMessage = Boolean(error.config?.skipErrorMessage)
    if (!error.response) {
      if (!skipErrorMessage) {
        handleError('网络连接失败，请检查您的网络')
      }
      return Promise.reject(error)
    }
    if (!skipErrorMessage) {
      const data = error.response.data
      // 下载类接口失败时，错误体常为 Blob(JSON)
      if (data instanceof Blob) {
        try {
          const text = await data.text()
          const parsed = JSON.parse(text) as { msg?: string }
          if (parsed?.msg) {
            handleError(parsed.msg)
            return Promise.reject(new Error(parsed.msg))
          }
        } catch {
          // 非 JSON，走状态码提示
        }
      } else if (data && typeof data === 'object' && 'msg' in (data as object)) {
        const msg = String((data as { msg?: string }).msg || '')
        if (msg) {
          handleError(msg)
          return Promise.reject(new Error(msg))
        }
      }
      const status = error.response?.status
      const errorMsg = StatusCodeMessage[status as number] || '服务器暂时未响应，请刷新页面并重试。若无法解决，请联系管理员'
      handleError(errorMsg)
    }
    return Promise.reject(error)
  },
)

const request = async <T = unknown>(config: AxiosRequestConfig): Promise<ApiRes<T>> => {
  return http.request<T>(config)
    .then((res: AxiosResponse) => res.data)
    .catch((err: { msg: string }) => Promise.reject(err))
}

const requestNative = async <T = unknown>(config: AxiosRequestConfig): Promise<AxiosResponse> => {
  return http.request<T>(config)
    .then((res: AxiosResponse) => res)
    .catch((err: { msg: string }) => Promise.reject(err))
}

const createRequest = (method: string) => {
  return <T = any>(url: string, params?: unknown, config?: AxiosRequestConfig): Promise<ApiRes<T>> => {
    return request({
      method,
      url,
      [method === 'get' ? 'params' : 'data']: params,
      ...(method === 'get'
        ? {
            paramsSerializer: (obj) => qs.stringify(obj),
          }
        : {}),
      ...config,
    })
  }
}

const download = (url: string, params?: object, config?: AxiosRequestConfig): Promise<AxiosResponse> => {
  return requestNative({
    method: 'get',
    url,
    responseType: 'blob',
    params,
    paramsSerializer: (obj) => qs.stringify(obj),
    ...config,
  })
}

export default {
  get: createRequest('get'),
  post: createRequest('post'),
  put: createRequest('put'),
  patch: createRequest('patch'),
  del: createRequest('delete'),
  request,
  requestNative,
  download,
}
