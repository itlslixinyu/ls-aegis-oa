const ALL_PERMISSION = '*:*:*'

/**
 * 用户权限模式是否覆盖所需权限（与后端 PatternMatchUtils.simpleMatch 语义对齐）
 * @param userPerm 用户持有的权限标识，可含 * 通配
 * @param required 页面/按钮要求的权限标识
 */
export function matchPermission(userPerm: string, required: string): boolean {
  if (!userPerm || !required) {
    return false
  }
  if (userPerm === ALL_PERMISSION || userPerm === '*') {
    return true
  }
  if (userPerm === required) {
    return true
  }
  const pattern = userPerm.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*')
  return new RegExp(`^${pattern}$`).test(required)
}
