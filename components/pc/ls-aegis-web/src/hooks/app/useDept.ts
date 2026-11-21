import { ref } from 'vue'
import type { TreeNodeData } from '@arco-design/web-vue'
import { listDeptDictTree } from '@/apis/system'
import { normalizeTreeBizIds } from '@/utils/biz-id'

/** 部门模块 */
export function useDept(options?: { onSuccess?: () => void }) {
  const loading = ref(false)
  const deptList = ref<TreeNodeData[]>([])

  const getDeptList = async (name?: string) => {
    try {
      loading.value = true
      const res = await listDeptDictTree({ description: name })
      // tree-select 的 key 与表单 deptId 统一为字符串
      deptList.value = normalizeTreeBizIds(res.data as TreeNodeData[])
      options?.onSuccess && options.onSuccess()
    } finally {
      loading.value = false
    }
  }
  return { deptList, getDeptList, loading }
}
