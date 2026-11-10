import type { TableData, TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import type { Options as paginationOptions } from './usePagination'
import { useBreakpoint, usePagination } from '@/hooks'
import { patchListByKey } from '@/utils/listDiff'

interface Options<T, U> {
  formatResult?: (data: T[]) => U[]
  onSuccess?: () => void
  immediate?: boolean
  rowKey?: keyof T
  paginationOption?: paginationOptions
}

interface PaginationParams { page: number, size: number }
type Api<T> = (params: PaginationParams) => Promise<ApiRes<PageRes<T[]>>> | Promise<ApiRes<T[]>>

interface GetTableDataOptions {
  /** 静默刷新：不闪 loading、diff 局部更新、保留滚动/选中/分页 */
  silent?: boolean
}

export function useTable<T extends U, U = T>(api: Api<T>, options?: Options<T, U>) {
  const { formatResult, onSuccess, immediate, rowKey } = options || {}
  const { pagination, setTotal } = usePagination(() => getTableData(), options?.paginationOption)
  const loading = ref(false)
  const tableData: Ref<U[]> = ref([])
  let requestVersion = 0

  async function getTableData(opts?: GetTableDataOptions) {
    const version = ++requestVersion
    const silent = opts?.silent ?? false
    try {
      // 静默刷新不打 loading，避免遮罩导致表格高度跳动/闪烁
      if (!silent) {
        loading.value = true
      }
      const res = await api({ page: pagination.current, size: pagination.pageSize })
      // 快速筛选/翻页时，旧请求即使最后返回，也不能覆盖最新页面。
      if (version !== requestVersion) return
      const raw = res?.data
      const data = Array.isArray(raw)
        ? raw
        : Array.isArray(raw?.list)
          ? raw.list
          : Array.isArray(raw?.records)
            ? raw.records
            : Array.isArray(raw?.rows)
              ? raw.rows
              : []
      const next = (formatResult ? formatResult(data) : data) as U[]
      const total = Array.isArray(raw)
        ? data.length
        : Number(raw?.total ?? data.length) || 0
      const key = (rowKey as string) || 'id'

      // 已有数据或静默刷新：diff 局部更新，无变化不整体替换数组
      if (silent || tableData.value.length > 0) {
        const listChanged = patchListByKey(
          tableData.value as Record<string, unknown>[],
          next as Record<string, unknown>[],
          key,
        )
        const totalChanged = pagination.total !== total
        if (totalChanged) {
          setTotal(total)
        }
        if (listChanged || totalChanged) {
          onSuccess && onSuccess()
        }
      } else {
        tableData.value = next
        setTotal(total)
        onSuccess && onSuccess()
      }
    } catch {
      if (version === requestVersion && !silent) {
        tableData.value = []
        setTotal(0)
      }
    } finally {
      if (version === requestVersion) {
        loading.value = false
      }
    }
  }

  // 是否立即触发
  const isImmediate = immediate ?? true
  isImmediate && getTableData()

  // 多选
  const selectedKeys = ref<(string | number)[]>([])
  const select: TableInstance['onSelect'] = (rowKeys) => {
    if (Array.isArray(rowKeys)) {
      selectedKeys.value = rowKeys
    }
  }

  // 全选
  const selectAll: TableInstance['onSelectAll'] = (checked) => {
    const key = rowKey ?? 'id'
    const arr = (tableData.value as TableData[]).filter((i) => !(i?.disabled ?? false))
    selectedKeys.value = checked ? arr.map((i) => i[key as string]) : []
  }

  // 查询（重置页码/清空选中；筛选条件由业务侧 queryForm 自行保留）
  const search = () => {
    selectedKeys.value = []
    pagination.onChange(1)
  }

  // 刷新：静默 diff，保留滚动条、选中行、分页、筛选
  const refresh = () => {
    getTableData({ silent: true })
  }

  // 删除
  const handleDelete = async <T>(
    deleteApi: () => Promise<ApiRes<T>>,
    options?: { title?: string, content?: string, successTip?: string, showModal?: boolean, multiple?: boolean },
  ): Promise<boolean | undefined> => {
    const onDelete = async () => {
      try {
        const res = await deleteApi()
        if (res.success) {
          // 计算新总页数
          const deleteNum = options?.multiple ? selectedKeys.value.length : 1
          const totalPage = Math.ceil((pagination.total - deleteNum) / pagination.pageSize)
          // 修正当前页码
          if (pagination.current > totalPage) {
            pagination.current = totalPage > 0 ? totalPage : 1
          }
          options?.multiple && (selectedKeys.value = [])
          Message.success(options?.successTip || '删除成功')
          await getTableData({ silent: true })
        }
        return res.success
      } catch (error) {
        return false
      }
    }
    const flag = options?.showModal ?? true // 是否显示对话框
    if (!flag) {
      return onDelete()
    }
    Modal.warning({
      title: options?.title || '提示',
      content: options?.content || '是否确定删除该条数据？',
      okButtonProps: { status: 'danger' },
      hideCancel: false,
      maskClosable: false,
      onBeforeOk: onDelete,
    })
  }

  const { breakpoint } = useBreakpoint()
  // 表格操作列在小屏下不固定在右侧
  const fixed = computed(() => !['xs', 'sm'].includes(breakpoint.value) ? 'right' : undefined)

  return {
    /** 表格加载状态 */
    loading,
    /** 表格数据 */
    tableData,
    /** 获取表格数据 */
    getTableData,
    /** 搜索，页码会重置为1 */
    search,
    /** 分页的传参 */
    pagination,
    /** 选择的行keys */
    selectedKeys,
    /** 选择行 */
    select,
    /** 全选行 */
    selectAll,
    /** 处理删除、批量删除 */
    handleDelete,
    /** 刷新表格数据：diff 局部更新，页码/选中/筛选不重置 */
    refresh,
    /** 操作列在小屏场景下不固定在右侧 */
    fixed,
  }
}
