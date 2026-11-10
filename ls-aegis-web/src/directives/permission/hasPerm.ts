import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores'
import { matchPermission } from '@/utils/permission-match'

/**
 * @desc v-permission 操作权限处理
 * @desc 使用 v-permission="['system:user:create']"
 */
function checkPermission(el: HTMLElement, binding: DirectiveBinding) {
  const userStore = useUserStore()
  const { value } = binding
  if (value && Array.isArray(value) && value.length) {
    const permissionValues: string[] = value
    const hasPermission = permissionValues.some((required) =>
      userStore.permissions.some((userPerm) => matchPermission(userPerm, required)),
    )
    if (!hasPermission) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  } else {
    throw new Error(`need permission! Like v-hasPerm="['home:btn:edit','home:btn:delete']"`)
  }
}

const directive: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkPermission(el, binding)
  },
}

export default directive
