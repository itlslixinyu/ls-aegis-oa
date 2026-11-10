import http from '@/utils/http'

const BASE_URL = '/eoms/admin/h5/auth'

/** H5 在线客户令牌 */
export interface H5OnlineTokenResp {
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

export function issueH5Invitation(customerId: string) {
  return http.post<H5InvitationResp>(`${BASE_URL}/invitations`, { customerId })
}
