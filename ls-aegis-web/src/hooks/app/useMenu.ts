import { ref } from 'vue'
import type { TreeNodeData } from '@arco-design/web-vue'
import { listMenuDictTree } from '@/apis/system'
import { listTenantPackageMenu } from '@/apis/tenant/package'
import { normalizeTreeBizIds } from '@/utils/biz-id'

/** 菜单模块 */
export function useMenu(options?: { onSuccess?: () => void }) {
  const loading = ref(false)
  const menuList = ref<TreeNodeData[]>([])

  const getMenuList = async (name?: string) => {
    try {
      loading.value = true
      const res = await listMenuDictTree({ description: name })
      // tree-select / 勾选 key 与 menuIds 统一为字符串，避免雪花 ID 回显失败
      menuList.value = normalizeTreeBizIds(res.data as TreeNodeData[])
      options?.onSuccess && options.onSuccess()
    } finally {
      loading.value = false
    }
  }

  // 获取租户套餐菜单
  const getTenantPackageMenuList = async () => {
    try {
      loading.value = true
      const res = await listTenantPackageMenu()
      menuList.value = normalizeTreeBizIds(res.data as TreeNodeData[])
      options?.onSuccess && options.onSuccess()
    } finally {
      loading.value = false
    }
  }

  return { menuList, getMenuList, loading, getTenantPackageMenuList }
}
