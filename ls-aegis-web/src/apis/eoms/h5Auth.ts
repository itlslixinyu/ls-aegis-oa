import http from '@/utils/http'

const BASE_URL = '/eoms/admin/h5/auth'

/** H5 在线客户令牌 */
export interface H5OnlineTokenResp {
  loginName?: string
  displayName?: string
  customerId: string
  customerNo: string
  customerName: string
  loginTime: string
  expireSeconds: number
  /** SM3/MD5 摘要前缀（列表展示，非完整令牌） */
  tokenDigest: string
  /** 不具登录能力的管理句柄；完整会话令牌不返回管理列表 */
  sessionId: string
}

export interface H5OnlineTokenQuery extends PageQuery {
  sort?: string[]
  customerNo?: string
  customerName?: string
}

/** 查询在线 H5 客户令牌 */
export function listH5OnlineToken(query: H5OnlineTokenQuery) {
  return http.get<PageRes<H5OnlineTokenResp[]>>(`${BASE_URL}/online`, query)
}

/** 强退 H5 客户令牌 */
export function kickoutH5Token(sessionId: string) {
  return http.del(`${BASE_URL}/online/${encodeURIComponent(sessionId)}`)
}

export interface H5InvitationResp {
  invitationCode: string
  expiresInSeconds: number
  customerId: string
}

export function issueH5Invitation(customerId: string, options: { accountRole?: 'ADMIN' | 'MEMBER', resetAccountId?: string } = {}) {
  return http.post<H5InvitationResp>(`${BASE_URL}/invitations`, { customerId, ...options })
}

export interface H5CustomerMember {
  id: string
  customerId: string
  loginName: string
  displayName: string
  accountRole: 'ADMIN' | 'MEMBER'
  status: 0 | 1
}
export function listH5CustomerMembers(customerId: string) {
  return http.get<H5CustomerMember[]>(`${BASE_URL}/customers/${customerId}/members`)
}
export function updateH5CustomerMember(customerId: string, member: H5CustomerMember) {
  return http.put(`${BASE_URL}/customers/${customerId}/members/${member.id}`, {
    displayName: member.displayName, accountRole: member.accountRole, status: member.status,
  })
}
