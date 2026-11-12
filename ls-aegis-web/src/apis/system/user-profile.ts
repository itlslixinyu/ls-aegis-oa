import type { AxiosRequestConfig } from 'axios'
import http from '@/utils/http'

const BASE_URL = '/user/profile'

/** @desc 上传头像 */
export function uploadAvatar(data: FormData, options: Pick<AxiosRequestConfig, 'signal'> = {}) {
  return http.patch(`${BASE_URL}/avatar`, data, options)
}

/** @desc 修改用户基本信息 */
export function updateUserBaseInfo(data: { nickname: string, gender: number }) {
  return http.patch(`${BASE_URL}/basic/info`, data)
}

/** @desc 修改密码 */
export function updateUserPassword(data: { oldPassword: string, newPassword: string }) {
  return http.patch(`${BASE_URL}/password`, data)
}

/** @desc 登记/修改手机号（仅登记，无需短信验证码） */
export function updateUserPhone(data: { phone: string, oldPassword: string }) {
  return http.patch(`${BASE_URL}/phone`, data)
}

/** @desc 修改邮箱 */
export function updateUserEmail(data: { email: string, captcha: string, oldPassword: string }) {
  return http.patch(`${BASE_URL}/email`, data)
}

/** @desc 查询账号绑定的界面配置 */
export function getUserUiSettings() {
  return http.get<App.AppSettings | null>(`${BASE_URL}/ui-settings`)
}

/** @desc 保存界面配置到当前账号 */
export function saveUserUiSettings(data: App.AppSettings) {
  return http.put(`${BASE_URL}/ui-settings`, data)
}
