import http from '@/utils/http'

export interface DashboardStats {
  projectCount?: number
  sealCount?: number
  monthSignCount?: number
  userCount?: number
  projectStatusStats?: Record<string, number>
  sealCategoryStats?: Record<string, number>
  recentSigns?: Array<{
    recordId?: string
    projectName?: string
    sealName?: string
    operatorName?: string
    signTime?: string
  }>
}

export function getDashboardStats() {
  return http.get<DashboardStats>('/sign/dashboard/stats')
}
