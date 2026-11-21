import type * as T from './type'
import http from '@/utils/http'

export type * from './type'

const BASE_URL = '/auth'

const login = (req: T.AccountLoginReq | T.EmailLoginReq) => {
  return http.post<T.LoginResp>(`${BASE_URL}/login`, req)
}

/** @desc 账号登录 */
export function accountLogin(req: T.AccountLoginReq) {
  return login(req)
}

/** @desc 邮箱登录 */
export function emailLogin(req: T.EmailLoginReq) {
  return login(req)
}

/** @desc 退出登录 */
export function logout() {
  return http.post(`${BASE_URL}/logout`)
}

/** @desc 获取用户信息 */
export const getUserInfo = () => {
  return http.get<T.UserInfo>(`${BASE_URL}/user/info`)
}

/** @desc 获取路由信息 */
export const getUserRoute = () => {
  return http.get<T.RouteItem[]>(`${BASE_URL}/user/route`)
}

/** @desc 国密 SM2 公钥（仅公钥） */
export const getGmPublicKey = () => {
  return http.get<T.GmPublicKeyResp>(`${BASE_URL}/gm/public-key`)
}

/** @desc 邮箱验证码自助找回密码 */
export function forgotPassword(req: T.ForgotPasswordReq) {
  return http.post(`${BASE_URL}/forgot-password`, req)
}
