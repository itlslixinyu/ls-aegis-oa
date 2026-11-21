import { ref } from 'vue'
import { listRoleDict } from '@/apis/system'
import type { LabelValueState } from '@/types/global'
import { toSelectOpts } from '@/utils/biz-id'

/** 角色模块 */
export function useRole(options?: { onSuccess?: () => void }) {
  const loading = ref(false)
  const roleList = ref<LabelValueState[]>([])

  const getRoleList = async () => {
    try {
      loading.value = true
      const res = await listRoleDict()
      // 雪花 ID 一律字符串，避免 Select 只显示裸 ID
      roleList.value = toSelectOpts(res.data) as LabelValueState[]
      options?.onSuccess && options.onSuccess()
    } finally {
      loading.value = false
    }
  }
  return { roleList, getRoleList, loading }
}
