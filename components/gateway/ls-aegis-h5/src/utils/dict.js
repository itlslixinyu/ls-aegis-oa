/**
 * 状态文案：优先平台 /system/common/dict/{code}，未加载时用兜底。
 * 改字典即可改 H5 展示，不必发前端补丁。
 */
import { reactive } from 'vue'
import { listCommonDict } from '@/api/system'

const EXTRA_COLOR = {
  primary: '#1989fa',
  success: '#07c160',
  warning: '#ff976a',
  error: '#ee0a24',
  default: '#969799'
}

const EXTRA_TAG = {
  primary: 'primary',
  success: 'success',
  warning: 'warning',
  error: 'danger',
  default: 'default'
}

export const applyStatusMap = reactive({
  WAIT: { text: '待处理', color: '#ff976a', tagType: 'warning' },
  CONFIRM: { text: '已确认', color: '#1989fa', tagType: 'primary' },
  ORDER: { text: '已转工单', color: '#07c160', tagType: 'success' },
  CANCEL: { text: '已取消', color: '#969799', tagType: 'default' }
})

export const orderStatusMap = reactive({
  WAIT_ACCEPT: { text: '待接单', color: '#ff976a', tagType: 'warning' },
  ACCEPTED: { text: '已接单', color: '#1989fa', tagType: 'primary' },
  PROCESSING: { text: '维修中', color: '#07c160', tagType: 'success' },
  WAIT_SETTLEMENT: { text: '待结算', color: '#ed6a0c', tagType: 'warning' },
  FINISHED: { text: '已完成', color: '#07c160', tagType: 'success' },
  CLOSED: { text: '已关闭', color: '#969799', tagType: 'default' }
})

export const urgentLevelMap = reactive({
  NORMAL: { text: '一般', color: '#07c160' },
  URGENT: { text: '紧急', color: '#ff976a' },
  VERY_URGENT: { text: '特急', color: '#ee0a24' }
})

export const sourceMap = reactive({
  H5: '客户H5',
  ADMIN: '后台登记'
})

export const faultTypeOptions = reactive([
  { text: '电脑设备故障', value: '电脑设备故障' },
  { text: '监控设备故障', value: '监控设备故障' },
  { text: '办公设备故障', value: '办公设备故障' },
  { text: '通用基础故障', value: '通用基础故障' }
])

function patchStatusMap(target, items) {
  if (!Array.isArray(items) || !items.length) {
    return
  }
  items.forEach((item) => {
    const extra = item.extra || 'default'
    target[item.value] = {
      text: item.label,
      color: EXTRA_COLOR[extra] || EXTRA_COLOR.default,
      tagType: EXTRA_TAG[extra] || 'default'
    }
  })
}

function replaceReactiveArray(target, next) {
  target.splice(0, target.length, ...next)
}

/** 拉取 H5 用到的业务字典并覆盖兜底映射 */
export async function loadBizDicts() {
  const [apply, order, urgent, fault, source] = await Promise.all([
    listCommonDict('eoms_apply_status'),
    listCommonDict('eoms_order_status'),
    listCommonDict('eoms_urgent_level'),
    listCommonDict('eoms_fault_type'),
    listCommonDict('eoms_apply_source')
  ])
  patchStatusMap(applyStatusMap, apply)
  patchStatusMap(orderStatusMap, order)
  if (Array.isArray(urgent) && urgent.length) {
    urgent.forEach((item) => {
      const extra = item.extra || 'default'
      urgentLevelMap[item.value] = {
        text: item.label,
        color: EXTRA_COLOR[extra] || EXTRA_COLOR.default
      }
    })
  }
  if (Array.isArray(fault) && fault.length) {
    replaceReactiveArray(
      faultTypeOptions,
      fault.map((item) => ({ text: item.label, value: item.value }))
    )
  }
  if (Array.isArray(source) && source.length) {
    Object.keys(sourceMap).forEach((k) => {
      delete sourceMap[k]
    })
    source.forEach((item) => {
      sourceMap[item.value] = item.label
    })
  }
}

export function getApplyStatusText(status) {
  if (status === 'WAIT') return '等待接单'
  return applyStatusMap[status]?.text || status
}

export function getApplyStatusColor(status) {
  return applyStatusMap[status]?.color || '#969799'
}

export function getOrderStatusText(status) {
  return orderStatusMap[status]?.text || status
}

export function getOrderStatusColor(status) {
  return orderStatusMap[status]?.color || '#969799'
}

export function getUrgentText(level) {
  return urgentLevelMap[level]?.text || level
}

export function getUrgentColor(level) {
  return urgentLevelMap[level]?.color || '#969799'
}

/** 格式化日期时间 */
export function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

/** 系统角色编码 -> 中文名称映射（角色名以接口 roleName 为准，此处仅兜底） */
export const ROLE_NAME_MAP = {
  admin: '超级管理员',
  common: '普通角色',
  visitor: '访客',
  eoms_engineer: '维修工程师',
  eoms_engineer_leader: '维修主管',
  eoms_foreman: '维修组长',
  eoms_dispatcher: '调度员',
  eoms_finance: '财务',
  eoms_manager: '运营经理',
  eoms_customer_service: '客服',
  eoms_salesman: '业务员',
  eoms_purchaser: '采购',
  eoms_warehouse: '仓管',
  eoms_quality: '质检',
  eoms_admin: '运维管理员',
  eoms_customer: '客户用户'
}

/**
 * 获取角色中文名称
 * @param {Object|String} role - 角色对象（含 roleKey/roleName/name）或角色编码字符串
 * @returns {String} 中文角色名称，未匹配则原样返回
 */
export function getRoleLabel(role) {
  if (!role) return ''
  if (typeof role === 'object') {
    if (role.roleName) return role.roleName
    if (role.name) return role.name
    if (role.roleKey) return ROLE_NAME_MAP[role.roleKey] || role.roleKey
    return JSON.stringify(role)
  }
  return ROLE_NAME_MAP[role] || role
}
