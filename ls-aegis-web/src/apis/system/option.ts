import type * as T from './type'
import http from '@/utils/http'

export type * from './type'

const BASE_URL = '/system/option'

/** @desc 查询参数列表 */
export function listOption(query: T.OptionQuery) {
  return http.get<T.OptionResp[]>(`${BASE_URL}`, query)
}

/** @desc 修改参数 */
export function updateOption(data: any) {
  return http.put(`${BASE_URL}`, data)
}

/** @desc 重置参数 */
export function resetOptionValue(query: T.OptionQuery) {
  return http.patch(`${BASE_URL}/value`, query)
}

/** @desc 发送测试邮件 */
export function sendTestMail(to: string) {
  return http.post(`${BASE_URL}/mail/test`, { to })
}

/** @desc 立即拉取天气（不等待刷新间隔，不清 JWT） */
export function refreshWeatherNow() {
  return http.post<T.WeatherNowResp>(`${BASE_URL}/weather/refresh`)
}

/** @desc 测试天气连接 */
export function testWeatherConnection() {
  return http.post<T.WeatherNowResp>(`${BASE_URL}/weather/test`)
}

/** @desc 查看敏感参数明文（配置页点击「显示」时使用） */
export function getSensitiveOptionValue(code: string) {
  return http.get<string>(`${BASE_URL}/sensitive/${code}`)
}
