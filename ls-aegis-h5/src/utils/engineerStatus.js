import { getOrderStatusText } from './dict'

// Engineer service completion does not imply back-office financial settlement.
export function getEngineerStatusText(status) {
  if (status === 'WAIT_SETTLEMENT' || status === 'FINISHED') return '维修已完成'
  if (status === 'ACCEPTED') return '待开工'
  return getOrderStatusText(status)
}
export function engineerStatusFilter(status) {
  return ['WAIT_SETTLEMENT', 'FINISHED', 'COMPLETED'].includes(status) ? 'COMPLETED' : status
}
