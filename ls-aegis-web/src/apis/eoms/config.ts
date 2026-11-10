import http from '@/utils/http'

/** 百度地图 AK 测试结果 */
export interface BaiduAkTestResp {
  success: boolean
  message: string
  sampleAddress?: string
}

/** 使用浏览器位置与已保存 AK 测试百度逆地理编码是否可用 */
export function testBaiduMapAk(params: { longitude: string, latitude: string }) {
  return http.post<BaiduAkTestResp>('/eoms/config/baidu-ak/test', params)
}

/** 查看已保存的百度地图 AK 明文（配置页点击「显示」时使用） */
export function getBaiduAkPlain() {
  return http.get<string>('/eoms/config/baidu-ak/plain')
}
