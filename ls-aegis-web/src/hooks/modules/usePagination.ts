import { reactive, toRefs, watch } from 'vue'
import { useBreakpoint } from '@/hooks'

type Callback = () => void

/** 全站分页默认配置（业务页统一走 usePagination / useTable，勿各自拼装） */
export const DEFAULT_PAGINATION_OPTIONS = {
  defaultPageSize: 10,
  defaultSizeOptions: [10, 20, 30, 40, 50],
} as const

export interface Options {
  defaultPageSize: number
  defaultSizeOptions: number[]
}

export function usePagination(
  callback: Callback,
  options: Options = { ...DEFAULT_PAGINATION_OPTIONS, defaultSizeOptions: [...DEFAULT_PAGINATION_OPTIONS.defaultSizeOptions] },
) {
  const { breakpoint } = useBreakpoint()

  const pagination = reactive({
    showPageSize: true,
    showTotal: true,
    current: 1,
    pageSize: options.defaultPageSize,
    pageSizeOptions: options.defaultSizeOptions,
    total: 0,
    simple: false,
    onChange: (size: number) => {
      pagination.current = size
      callback && callback()
    },
    onPageSizeChange: (size: number) => {
      pagination.current = 1
      pagination.pageSize = size
      callback && callback()
    },
  })

  watch(
    () => breakpoint.value,
    () => {
      pagination.simple = ['xs'].includes(breakpoint.value)
      pagination.showTotal = !['xs'].includes(breakpoint.value)
    },
    { immediate: true },
  )

  const changeCurrent = pagination.onChange
  const changePageSize = pagination.onPageSizeChange
  function setTotal(value: number) {
    pagination.total = value
  }

  const { current, pageSize, total } = toRefs(pagination)

  return {
    current,
    pageSize,
    total,
    pagination,
    changeCurrent,
    changePageSize,
    setTotal,
  }
}
