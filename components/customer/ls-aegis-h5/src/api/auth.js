/**
 * 平台鉴权 API（与 PC 对齐）
 */
import request from '@/utils/request'

/** 与 PC VITE_CLIENT_ID 一致的默认客户端 */
export const PLATFORM_CLIENT_ID = 'ef51c9a3e9046c4f2ea45142c8a8344a'

/** 账号登录 POST /auth/login */
export function accountLogin(data) {
  return request({
    url: '/auth/login',
    headers: { isToken: false, repeatSubmit: false },
    method: 'post',
    data: {
      clientId: PLATFORM_CLIENT_ID,
      authType: 'ACCOUNT',
      ...data
    }
  })
}

/** 退出登录 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/** 当前用户信息 */
export function getUserInfo() {
  return request({
    url: '/auth/user/info',
    method: 'get'
  })
}

/** 国密 SM2 公钥 */
export function getGmPublicKey() {
  return request({
    url: '/auth/gm/public-key',
    headers: { isToken: false },
    method: 'get'
  })
}

/** 图片验证码 */
export function getImageCaptcha() {
  return request({
    url: '/captcha/image',
    headers: { isToken: false },
    method: 'get',
    timeout: 20000
  })
}
