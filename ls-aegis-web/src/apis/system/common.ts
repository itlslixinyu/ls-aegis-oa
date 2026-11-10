import http from '@/utils/http'
import type { LabelValueState } from '@/types/global'
import type { WeatherNowResp } from './type'

const BASE_URL = '/system/common'

/** @desc 查询字典列表 */
export function listCommonDict(code: string) {
  return http.get<LabelValueState[]>(`${BASE_URL}/dict/${code}`)
}

/** @desc 查询系统配置参数 */
export function listSiteOptionDict() {
  return http.get<LabelValueState[]>(`${BASE_URL}/dict/option/site`)
}

/** @desc 查询天气配置参数（运营中枢顶栏；失败静默降级，避免打扰） */
export function listWeatherOptionDict() {
  return http.get<LabelValueState[]>(`${BASE_URL}/dict/option/weather`, undefined, {
    skipErrorMessage: true,
  })
}

/** @desc 查询大屏配置参数（运营中枢标题与展示开关；失败静默降级） */
export function listDashboardOptionDict() {
  return http.get<LabelValueState[]>(`${BASE_URL}/dict/option/dashboard`, undefined, {
    skipErrorMessage: true,
  })
}

/** @desc 查询实时天气（后端代理；失败静默降级） */
export function getWeatherNow(params?: { lat?: number, lon?: number }) {
  return http.get<WeatherNowResp>(`${BASE_URL}/weather/now`, params, {
    skipErrorMessage: true,
  })
}

/** @desc 上传文件 */
export function upload(data: FormData) {
  return http.post(`${BASE_URL}/file`, data)
}

/** @desc 查询文件元数据（需登录） */
export function getFileInfo(id: string) {
  return http.get<{ id: string, originalName?: string, name?: string, extension?: string, url?: string }>(`${BASE_URL}/file/${id}/info`)
}

/** @desc 鉴权下载/预览文件（blob，需登录） */
export function downloadFile(id: string) {
  return http.download(`${BASE_URL}/file/${id}`)
}

/** @desc 查询租户开启状态 */
export function getTenantStatus() {
  return http.get<boolean>(`${BASE_URL}/dict/option/tenant`)
}
