/** 用户类型 */
export interface UserInfo {
  id: string
  username: string
  nickname: string
  gender: 0 | 1 | 2
  email: string
  phone: string
  avatar: string
  pwdResetTime: string
  pwdExpired: boolean
  registrationDate: string
  deptName: string
  roles: string[]
  roleNames: string[]
  permissions: string[]
}

/** 路由类型 */
export interface RouteItem {
  id: string
  title: string
  parentId: string
  type: 1 | 2 | 3
  path: string
  name: string
  component: string
  redirect: string
  icon: string
  isExternal: boolean
  isHidden: boolean
  isCache: boolean
  permission: string
  roles: string[]
  sort: number
  status: 0 | 1
  children: RouteItem[]
  activeMenu: string
  alwaysShow: boolean
  breadcrumb: boolean
  showInTabs: boolean
  affix: boolean
}

/** 认证类型（登录页仅开放账号/邮箱） */
export type AuthType = 'ACCOUNT' | 'EMAIL'

export const AuthTypeConstants = {
  ACCOUNT: 'ACCOUNT',
  EMAIL: 'EMAIL',
} as const

/** 基础认证请求接口 */
export interface AuthReq {
  clientId?: string
  authType?: AuthType
}

/** 账号登录请求参数 */
export interface AccountLoginReq extends AuthReq {
  username: string
  password: string
  captcha: string
  uuid: string
}

/** 邮箱登录请求参数 */
export interface EmailLoginReq extends AuthReq {
  email: string
  captcha: string
}

/** 登录响应类型 */
export interface LoginResp {
  token: string
  tenantId: string
}

/** 国密公钥（仅公钥） */
export interface GmPublicKeyResp {
  enable: boolean
  algorithm: string
  publicKeyHex: string
  publicKeyBase64: string
}

/** 邮箱验证码自助找回密码 */
export interface ForgotPasswordReq {
  email: string
  captcha: string
  newPassword: string
}
