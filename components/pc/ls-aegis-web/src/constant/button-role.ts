/** 按钮业务角色（视觉与文案权威字典：`public/design/button-dictionary.html`） */
/** 全局皮肤：`styles/arco-ui/a-button.less`、`styles/arco-ui/a-link.less`（业务页勿再 import） */

/** 七类业务角色 */
export const ButtonRole = {
  /** 主操作：页内主 CTA（同工具栏仅 1 个实心 primary） */
  PRIMARY: 'primary',
  /** 次操作：同级辅助，不抢主 CTA */
  SECONDARY: 'secondary',
  /** 危险：不可逆破坏 */
  DANGER: 'danger',
  /** 警示：有风险但非删除 */
  WARNING: 'warning',
  /** 文字操作：表格行 / 轻量链接，统一 a-link */
  LINK: 'link',
  /** 图标工具：顶栏 / 表格工具条 */
  ICON: 'icon',
  /** 更多：Dropdown 溢出（触发可用 type=text size=mini） */
  MORE: 'more',
} as const

export type ButtonRoleType = (typeof ButtonRole)[keyof typeof ButtonRole]

/** 危险子形态 */
export const DangerVariant = {
  /** primary + status=danger，工具栏批量 */
  SOLID: 'solid',
  /** 默认按钮 + status=danger */
  SOFT: 'soft',
  /** outline + danger */
  OUTLINE: 'outline',
  /** a-link status=danger */
  LINK: 'link',
} as const

export type DangerVariantType = (typeof DangerVariant)[keyof typeof DangerVariant]

/** 文字操作子形态 */
export const LinkVariant = {
  DEFAULT: 'default',
  DANGER: 'danger',
  MUTED: 'muted',
} as const

export type LinkVariantType = (typeof LinkVariant)[keyof typeof LinkVariant]

/** 修饰（非角色） */
export const ButtonModifier = {
  SIZE_SM: 'sm',
  SIZE_LG: 'lg',
  LONG: 'long',
  LOADING: 'loading',
  DISABLED: 'disabled',
  /** 仅登录/营销等特例；业务工具栏默认直角 */
  ROUND: 'round',
} as const

/**
 * 角色 → 推荐 Arco 写法（落地时对照，勿按 type×status 穷举新皮肤）
 */
export const BUTTON_ROLE_ARCO: Record<ButtonRoleType, string> = {
  [ButtonRole.PRIMARY]: 'a-button type="primary"',
  [ButtonRole.SECONDARY]: 'a-button type="secondary" 或默认按钮',
  [ButtonRole.DANGER]:
    '实心 primary+danger / 轻量 status=danger / 描边 outline+danger / 行内 a-link status=danger',
  [ButtonRole.WARNING]: 'a-button type="outline" status="warning"',
  [ButtonRole.LINK]: 'a-link（禁止表格行再用 type="text"）',
  [ButtonRole.ICON]: '图标按钮 / size=mini',
  [ButtonRole.MORE]: 'a-dropdown + ⋯（次要图标触发）',
}

/** 规范外写法：存量可暂留，禁止新增 */
export const BUTTON_ANTI_PATTERNS = [
  'type="primary" status="success"',
  'type="primary" status="warning"',
  '业务工具栏随意 shape="round"',
  '表格操作列 a-button type="text"（应改为 a-link）',
  '「重试」使用 status="danger"',
  '按钮文案「搜索」（应使用「查询」；placeholder/顶栏全局搜除外）',
  '业务页自行 import 按钮样式 / 另造 .btn-* class',
] as const

/** 字典静态页路径（开发 / 生产均可直接打开） */
export const BUTTON_DICTIONARY_PATH = '/design/button-dictionary.html'
