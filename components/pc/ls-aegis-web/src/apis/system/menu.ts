import type { TreeNodeData } from '@arco-design/web-vue'
import type * as T from './type'
import http from '@/utils/http'
import { activeNavigation } from '@/utils/retired-navigation'

export type * from './type'

const BASE_URL = '/system/menu'

/** @desc 查询菜单列表 */
export async function listMenu(query?: T.MenuQuery) {
  const res = await http.get<T.MenuResp[]>(`${BASE_URL}/tree`, query)
  return { ...res, data: activeNavigation(res.data) }
}

/** @desc 查询菜单详情 */
export function getMenu(id: string) {
  return http.get<T.MenuResp>(`${BASE_URL}/${id}`)
}

/** @desc 新增菜单 */
export function addMenu(data: any) {
  return http.post<boolean>(`${BASE_URL}`, data)
}

/** @desc 修改菜单 */
export function updateMenu(data: any, id: string) {
  return http.put(`${BASE_URL}/${id}`, data)
}

/** @desc 删除菜单 */
export function deleteMenu(id: string) {
  return http.del(`${BASE_URL}`, { ids: [id] })
}

/** @desc 清除菜单缓存 */
export function clearMenuCache() {
  return http.del(`${BASE_URL}/cache`)
}

/** @desc 查询菜单字典树 */
export async function listMenuDictTree(query: { description?: string }) {
  const res = await http.get<TreeNodeData[]>(`${BASE_URL}/dict/tree`, query)
  return { ...res, data: activeNavigation(res.data) }
}
