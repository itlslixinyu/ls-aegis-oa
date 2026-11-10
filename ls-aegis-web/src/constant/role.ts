/** 与后端 RoleCodeEnum 对齐 */
export const SUPER_ADMIN_ROLE = 'super_admin'
export const TENANT_ADMIN_ROLE = 'admin'
export const SUPER_ROLE_CODES = [SUPER_ADMIN_ROLE, TENANT_ADMIN_ROLE] as const

/** 过渡期兼容旧角色码（localStorage 缓存），后续版本移除 */
const LEGACY_SUPER_ADMIN_ROLE = 'role_admin'

/** 是否为超级角色（超管 / 租户管理员，与后端 getSuperRoleCodes 语义对齐） */
export function isSuperRole(role: string): boolean {
  if (role === LEGACY_SUPER_ADMIN_ROLE) {
    return true
  }
  return (SUPER_ROLE_CODES as readonly string[]).includes(role)
}
