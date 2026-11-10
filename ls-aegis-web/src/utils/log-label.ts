/** Legacy audit rows may contain framework setup hints instead of recorded labels. */
export function formatLogLabel(value: string | null | undefined, kind: 'description' | 'module'): string {
  const text = value?.trim() || ''
  if (!text || (text.startsWith('请在该接口') && text.includes('@top.continew.starter.log.annotation.Log('))) {
    return kind === 'module' ? '模块未记录' : '操作描述未记录'
  }
  return text
}
