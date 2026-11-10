import browse from 'xe-utils/browse'
import mapTree from 'xe-utils/mapTree'
import { camelCase, upperFirst } from 'lodash-es'
import { Message } from '@arco-design/web-vue'
import { isExternal } from '@/utils/validate'

/** 菜单/路由是否隐藏（兼容后端省略 false、数字 0/1） */
export function isRouteHidden(hidden: unknown): boolean {
  return hidden === true || hidden === 1 || hidden === '1'
}

/**
 * @description 动态路由 path 转 name
 * @demo /system => System
 * @demo /system/menu => SystemMenu
 * @demo /data-manage/detail => DataManageDetail
 */
export const transformPathToName = (path: string) => {
  if (!path) return ''
  if (isExternal(path)) return ''
  return upperFirst(camelCase(path))
}

/**
 * @desc 过滤树
 * @param { values } 数组
 */
type FilterTree = <T extends { children?: T[] }>(
  array: T[],
  iterate: (item: T, index?: number, items?: T[]) => boolean
) => T[]
export const filterTree: FilterTree = (values, fn) => {
  const arr = values.filter(fn)
  const data = mapTree(arr, (item) => {
    if (item.children && item.children.length) {
      item.children = item.children.filter(fn)
    }
    return item
  })
  return data
}

/** @desc 是否是h5环境 */
export const isMobile = () => {
  return browse().isMobile
}

/** @desc 问候 */
export function goodTimeText() {
  const time = new Date()
  const hour = time.getHours()
  return hour < 9 ? '早上好' : hour <= 11 ? '上午好' : hour <= 13 ? '中午好' : hour <= 18 ? '下午好' : '晚上好'
}

/** @desc 格式化文件大小 */
export const formatFileSize = (fileSize: number) => {
  if (fileSize == null || fileSize === 0) {
    return '0 Bytes'
  }
  const unitArr = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  let index = 0
  const srcSize = Number.parseFloat(fileSize.toString())
  index = Math.floor(Math.log(srcSize) / Math.log(1024))
  const size = srcSize / 1024 ** index
  return `${size.toFixed(2)} ${unitArr[index]}`
}

/** @desc 复制文本 */
export const copyText = (text: any) => {
  const textarea = document.createElement('textarea')
  textarea.value = text
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
  Message.success('复制成功')
}
