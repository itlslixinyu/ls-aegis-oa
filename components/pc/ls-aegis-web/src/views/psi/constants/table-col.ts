/**
 * PSI 列表列宽档位（px）
 * - 短列 / 编号 / 名称：均用 width 档位（normalize 会把「未写 width」的名称锁成默认 240）
 * - 编号：业务写 minWidth + ellipsis:false → 工具锁成 width
 */
export const PsiCol = {
  /** 短编码（如 DEMO-SP-0004、DEMO-WH-01），须单行完整 */
  codeMinSm: 152,
  /** 长单据 / 连续编号（如 DEMO202608140006、DEMO-RK-20260814-001） */
  codeMin: 184,
  /** UUID / 超长编码 */
  codeMinLg: 220,
  type: 100,
  typeSm: 88,
  status: 96,
  /** 完整日期时间（见 @/constant/datetime DATETIME_FORMAT） */
  date: 178,
  /** 仅日期 YYYY-MM-DD */
  dateSm: 120,
  amount: 120,
  qty: 88,
  unit: 72,
  spec: 120,
  /** 名称类舒适宽（也可业务直接 width: PsiCol.name） */
  name: 240,
  nameLg: 320,
  actionS: 140,
  actionM: 220,
  actionL: 280,
  actionXl: 360,
} as const
