/** 签章模块表格列宽约定（对齐 EomsCol / PsiCol） */
export const SignCol = {
  codeMin: 120,
  codeMinSm: 100,
  name: 160,
  nameLg: 220,
  type: 100,
  typeSm: 80,
  status: 90,
  date: 170,
  actionS: 120,
  actionM: 180,
  actionL: 240,
  actionXL: 320,
} as const

/**
 * 印章提取工具直径（毫米）。物理规格，不是展示文案；文案走字典 sign_seal_extract_category。
 */
export const SEAL_EXTRACT_SIZE_MM: Record<string, number> = {
  公章: 42,
  合同章: 42,
  财务章: 38,
  法人章: 20,
}
