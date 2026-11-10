import type { DashboardNoticeResp } from '@/apis'
import dayjs from 'dayjs'

/** 公告等级：色块 + 分类名按等级着色（字典 notice_type） */
export type NoticeLevel = 'primary' | 'success' | 'warning' | 'error' | 'default'

export interface NoticeTickerItem {
  id: string | number
  level: NoticeLevel
  label: string
  title: string
  isTop: boolean
  /** 原始发布时间 */
  publishTime?: string
  /** 列表右侧展示用短日期 */
  timeText?: string
}

/** 与字典 notice_type 颜色分级对齐（低→高：资讯→常规→人事→关注→紧急） */
const TYPE_META: Record<string, { level: NoticeLevel, label: string }> = {
  '1': { level: 'primary', label: '通知公告' }, // 常规行政
  '2': { level: 'primary', label: '政策文件' }, // 常规行政
  '3': { level: 'default', label: '新闻动态' }, // 资讯阅览
  '4': { level: 'success', label: '人事公告' }, // 组织人事
  '5': { level: 'warning', label: '财务公示' }, // 需关注
  '6': { level: 'error', label: '安全通告' }, // 高优先
  '7': { level: 'warning', label: '运维公告' }, // 需关注
  '8': { level: 'default', label: '公示通告' }, // 资讯阅览
  '9': { level: 'error', label: '紧急通告' }, // 高优先
}

export const noticeTypeMeta = (type: string | number): { level: NoticeLevel, label: string } => {
  return TYPE_META[String(type)] || { level: 'default', label: '公告' }
}

/** 欢迎页公告行右侧短日期 */
export const formatNoticeTimeText = (time?: string): string => {
  if (!time) {
    return ''
  }
  const d = dayjs(time)
  return d.isValid() ? d.format('YYYY-MM-DD') : ''
}

export const mapDashboardNotice = (item: DashboardNoticeResp): NoticeTickerItem => {
  const meta = noticeTypeMeta(item.type)
  const publishTime = item.publishTime
  return {
    id: item.id,
    title: item.title,
    isTop: !!item.isTop,
    label: meta.label,
    // 颜色按分类分级；置顶用独立角标，不覆盖等级色
    level: meta.level,
    publishTime,
    timeText: formatNoticeTimeText(publishTime),
  }
}

/** 置顶优先，其余打乱后最多取 limit 条（默认 20）；静默刷新可关 shuffle 避免滚动条跳回 */
export const pickDashboardNotices = (
  list: DashboardNoticeResp[] | null | undefined,
  limit = 20,
  options?: { shuffle?: boolean },
): DashboardNoticeResp[] => {
  const source = [...(list || [])]
  const tops = source.filter((i) => i.isTop)
  const rest = source.filter((i) => !i.isTop)
  if (options?.shuffle !== false) {
    for (let i = rest.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[rest[i], rest[j]] = [rest[j], rest[i]]
    }
  }
  return [...tops, ...rest].slice(0, limit)
}
