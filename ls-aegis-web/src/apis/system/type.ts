/** 用户类型 */
export interface UserResp {
  id: string
  username: string
  nickname: string
  avatar: string
  gender: number
  email: string
  phone: string
  description: string
  status: 1 | 2
  isSystem?: boolean
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
  deptId: string
  deptName: string
  roleIds: Array<number>
  roleNames: Array<string>
  disabled: boolean
}
export type UserDetailResp = UserResp & {
  pwdResetTime?: string
}
/** 用户创建响应（含一次性初始密码） */
export interface UserCreateResp {
  id: string
  initialPassword?: string
  generated?: boolean
}
export interface UserImportResp {
  importKey: string
  totalRows: number
  validRows: number
  duplicateUserRows: number
  duplicateEmailRows: number
  duplicatePhoneRows: number
}
export interface UserQuery {
  description?: string
  status?: number
  createTime?: Array<string>
  deptId?: string
  sort: Array<string>
  userIds?: Array<string>
  roleId?: string
}
export interface UserPageQuery extends UserQuery, PageQuery {}

/** 角色类型 */
export interface RoleResp {
  id: string
  name: string
  code: string
  sort: number
  description: string
  dataScope: number
  isSystem: boolean
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
  disabled: boolean
}
export type RoleDetailResp = RoleResp & {
  menuIds: Array<number>
  deptIds: Array<number>
  menuCheckStrictly: boolean
  deptCheckStrictly: boolean
}
export interface RolePermissionResp {
  id: string
  title: string
  parentId: string
  /** 类型（1：目录；2：菜单；3：按钮） */
  type?: 1 | 2 | 3
  permission?: string
  children?: RolePermissionResp[]
  permissions?: RolePermissionResp[]
  isChecked?: boolean
}
export interface RoleUserResp {
  id: string
  username: string
  nickname: string
  gender: number
  description: string
  status: 1 | 2
  isSystem?: boolean
  deptId: string
  deptName: string
  roleIds: Array<number>
  roleNames: Array<string>
  disabled: boolean
}
export interface RoleQuery {
  description?: string
  sort: Array<string>
}
export interface RoleUserQuery {
  description?: string
  sort: Array<string>
}
export interface RoleUserPageQuery extends RoleUserQuery, PageQuery {}

/** 菜单类型 */
export interface MenuResp {
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
  isCache: boolean
  isHidden: boolean
  permission: string
  sort: number
  status: 1 | 2
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
  children: MenuResp[]
}
export interface MenuQuery {
  title?: string
  status?: number
}

/** 部门类型 */
export interface DeptResp {
  id: string
  name: string
  sort: number
  status: 1 | 2
  isSystem: boolean
  description: string
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
  parentId: string
  children: DeptResp[]
}
export interface DeptQuery {
  description?: string
  status?: number
}

/** 字典类型 */
export interface DictResp {
  id: string
  name: string
  code: string
  isSystem: boolean
  description: string
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
}
export interface DictQuery {
  description?: string
  sort: Array<string>
}
export interface DictItemResp {
  id: string
  label: string
  value: string
  color: string
  sort: number
  description: string
  status: 1 | 2
  dictId: string
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
}
export interface DictItemQuery {
  description?: string
  status?: number
  sort: Array<string>
  dictId: string
}
export interface DictItemPageQuery extends DictItemQuery, PageQuery {
}

/** 公告类型 */
export interface NoticeResp {
  id: string
  createUserString?: string
  updateTime?: string
  content?: string
  title?: string
  type: string
  noticeScope: number
  noticeMethods?: Array<number>
  isTiming: boolean
  publishTime?: string
  isTop: boolean
  status?: number
}
export type NoticeDetailResp = NoticeResp & {
  content: string
  noticeUsers?: string[]
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
}
export type NoticePreviewResp = NoticeDetailResp & {
  content: string
}
export interface NoticeQuery {
  title?: string
  type?: string
  sort: Array<string>
}
export interface NoticePageQuery extends NoticeQuery, PageQuery {
}

/** 文件类型 */
export interface FileItem {
  id: string
  name: string
  originalName: string
  size: number
  url: string
  parentPath: string
  path: string
  /** 文件指纹（SM3） */
  fileDigest?: string
  contentType: string
  metadata: string
  thumbnailSize: number
  thumbnailName: string
  thumbnailMetadata: string
  thumbnailUrl: string
  extension: string
  type: number
  storageId: string
  storageName: string
  createUserString: string
  createTime: string
  updateUserString?: string
  updateTime?: string
}
/** 文件资源统计信息 */
export interface FileStatisticsResp {
  type: string
  /** 后端 Long 在平台 JSON 中序列化为字符串。 */
  size: number | string
  number: number | string
  unit: string
  data: Array<FileStatisticsResp>
}
/** 文件夹计算大小信息 */
export interface FileDirCalcSizeResp {
  size: number
}
export interface FileQuery {
  originalName?: string
  type?: string
  parentPath?: string
  sort: Array<string>
}
export interface FilePageQuery extends FileQuery, PageQuery {
}

/** 存储类型 */
export interface StorageResp {
  id: string
  name: string
  code: string
  type: number
  accessKey: string
  secretKey: string
  endpoint: string
  bucketName: string
  domain: string
  recycleBinEnabled: boolean
  recycleBinPath: string
  description: string
  isDefault: boolean
  sort: number
  status: number
  createUserString: string
  createTime: string
  updateUserString: string
  updateTime: string
}
export interface StorageQuery {
  description?: string
  type?: number
  sort: Array<string>
}

/** 客户端类型 */
export interface ClientResp {
  id: string
  clientId: string
  clientType: string
  authType: string
  activeTimeout: string
  timeout: string
  status: 1 | 2
  createUser: string
  createTime: string
  updateUser: string
  updateTime: string
  createUserString: string
  updateUserString: string
}
export interface ClientDetailResp {
  id: string
  clientId: string
  clientType: string
  authType: string
  activeTimeout: string
  timeout: string
  status: 1 | 2
  createUser: string
  createTime: string
  updateUser: string
  updateTime: string
  createUserString: string
  updateUserString: string
}
export interface ClientQuery {
  clientType: string
  authType: string[]
  status: string
  sort: Array<string>
}
export interface ClientPageQuery extends ClientQuery, PageQuery {}

/** 系统参数类型 */
export interface OptionResp<T = string> {
  id: string
  name: string
  code: string
  value: T
  defaultValue?: string
  description: string
}

export interface OptionQuery {
  code?: Array<string>
  category?: string
}

/** 基础配置类型 */
export interface BasicConfig {
  SITE_FAVICON: string
  SITE_LOGO: string
  SITE_TITLE: string
  SITE_TITLE_SHORT: string
  SITE_COMPANY: string
  SITE_COMPANY_SHORT: string
  SITE_COPYRIGHT: string
  SITE_BEIAN: string
  SITE_BEIAN_GONGAN: string
  SITE_BEIAN_GONGAN_ICON: string
}

/** 基础配置类型 */
export interface SiteConfig {
  SITE_FAVICON: OptionResp
  SITE_LOGO: OptionResp
  SITE_TITLE: OptionResp
  SITE_TITLE_SHORT: OptionResp
  SITE_COMPANY: OptionResp
  SITE_COMPANY_SHORT: OptionResp
  SITE_DESCRIPTION: OptionResp
  SITE_COPYRIGHT: OptionResp
  SITE_BEIAN: OptionResp
  SITE_BEIAN_GONGAN: OptionResp
  SITE_BEIAN_GONGAN_ICON: OptionResp
}

/** 安全配置类型 */
export interface SecurityConfig {
  PASSWORD_ERROR_LOCK_COUNT: OptionResp<number>
  PASSWORD_ERROR_LOCK_MINUTES: OptionResp<number>
  PASSWORD_EXPIRATION_DAYS: OptionResp<number>
  PASSWORD_EXPIRATION_WARNING_DAYS: OptionResp<number>
  PASSWORD_REPETITION_TIMES: OptionResp<number>
  PASSWORD_MIN_LENGTH: OptionResp<number>
  PASSWORD_ALLOW_CONTAIN_USERNAME: OptionResp<number>
  PASSWORD_REQUIRE_SYMBOLS: OptionResp<number>
}

/** 邮箱配置类型 */
export interface MailConfig {
  MAIL_PROTOCOL: OptionResp
  MAIL_HOST: OptionResp
  MAIL_PORT: OptionResp<number>
  MAIL_USERNAME: OptionResp
  MAIL_PASSWORD: OptionResp
  MAIL_SSL_ENABLED: OptionResp<number>
  MAIL_SSL_PORT: OptionResp<number>
}

/** 登录配置类型 */
export interface LoginConfig {
  LOGIN_CAPTCHA_ENABLED: OptionResp<number>
}

/** 天气配置类型 */
export interface WeatherConfig {
  WEATHER_ENABLED: OptionResp
  WEATHER_CITY_MODE: OptionResp
  WEATHER_CITY: OptionResp
  WEATHER_REFRESH_INTERVAL: OptionResp
  WEATHER_PROVIDER: OptionResp
  WEATHER_API_KEY: OptionResp
  WEATHER_JWT_KID: OptionResp
  WEATHER_JWT_PROJECT_ID: OptionResp
  WEATHER_JWT_PRIVATE_KEY: OptionResp
  WEATHER_API_HOST: OptionResp
  WEATHER_GEO_HOST: OptionResp
}

/** 大屏配置类型（运营中枢） */
export interface DashboardConfig {
  DASHBOARD_TITLE: OptionResp
  DASHBOARD_SHOW_CLOCK: OptionResp
  DASHBOARD_SHOW_STATUS: OptionResp
  DASHBOARD_SHOW_KPI: OptionResp
  DASHBOARD_SHOW_NOTICE: OptionResp
  DASHBOARD_SHOW_FS_TIP: OptionResp
  DASHBOARD_SHOW_DECOR: OptionResp
  DASHBOARD_DEFAULT_DAYS: OptionResp
  DASHBOARD_REFRESH_INTERVAL: OptionResp
}

/** 功能模块总开关 */
export interface ModuleConfig {
  MODULE_TENANT_ENABLED: OptionResp
  MODULE_SCHEDULE_ENABLED: OptionResp
}

/** 运维报修（编号前缀 / 地理 / 水印） */
export interface EomsConfig {
  CUSTOMER_NO_PREFIX: OptionResp
  REPAIR_NO_PREFIX: OptionResp
  ORDER_NO_PREFIX: OptionResp
  QUOTE_NO_PREFIX: OptionResp
  CONTRACT_NO_PREFIX: OptionResp
  PURCHASE_NO_PREFIX: OptionResp
  INCOME_NO_PREFIX: OptionResp
  PAYMENT_NO_PREFIX: OptionResp
  RECEIVABLE_NO_PREFIX: OptionResp
  MATERIAL_NO_PREFIX: OptionResp
  CATEGORY_NO_PREFIX: OptionResp
  SETTLEMENT_NO_PREFIX: OptionResp
  WAREHOUSE_NO_PREFIX: OptionResp
  SUPPLIER_NO_PREFIX: OptionResp
  INBOUND_NO_PREFIX: OptionResp
  GEO_BAIDU_AK: OptionResp
  GEO_BAIDU_REGEO_URL: OptionResp
  WATERMARK_MAX_SIDE: OptionResp
  H5_TOKEN_EXPIRE_MINUTES: OptionResp
  H5_TOKEN_AUTO_REFRESH: OptionResp
}

/** 运营中枢实时天气 */
export interface WeatherNowResp {
  city: string
  kind: string
  label: string
  temp: number
  feelsLike?: number | null
  aqi?: number | null
  aqiCategory?: string
  windLevel: number
  windDir?: string | null
  humidity?: number | null
  visibility?: number | null
  pressure?: number | null
  precip?: number | null
  provider: string
}

/** 百度地图 AK 测试结果 */
export interface BaiduAkTestResp {
  success: boolean
  message: string
  sampleAddress?: string
}

/** 系统消息类型 */
export interface MessageResp {
  id: string
  title: string
  content: string
  type: number
  path: string
  isRead: boolean
  readTime?: string
  createUserString?: string
  createTime: string
}

export interface MessageQuery {
  title?: string
  type?: number
  isRead?: boolean
  sort: Array<string>
}

export interface MessagePageQuery extends MessageQuery, PageQuery {
}

/** 分片上传 - 初始化参数 */
export interface MultiPartUploadInitReq {
  fileName: string
  fileSize: number
  /** 文件指纹（SM3） */
  fileDigest: string
  parentPath: string
  metaData: Record<string, string>
}

/** 分片上传 - 初始化响应 */
export interface MultiPartUploadInitResp {
  uploadId: string
  partSize: number
  path: string
  uploadedPartNumbers: number[]
}

/** 分片上传 - 上传分片参数 */
export interface UploadPartReq {
  uploadId: string
  partNumber: number
  file: Blob
  path: string
}

/** 分片上传 - 上传分片响应 */
export interface UploadPartResp {
  /** 分片编号 */
  partNumber: number
  /** 分片ETag */
  partETag: string
  /** 分片大小 */
  partSize: number
  /** 是否成功 */
  success: boolean
  /** 错误信息 */
  errorMessage?: string
}

/** 分片上传 - 完成上传参数（后端按 uploadId 从缓存合并分片，无需客户端传 ETag） */
export interface CompleteMultipartUploadReq {
  uploadId: string
}

/** 分片上传 - 取消上传参数 */
export interface CancelUploadParams {
  uploadId: string
}
