import CronParser from 'cron-parser'
import dayjs from 'dayjs'
import { DATETIME_FORMAT } from '@/constant/datetime'

/** 可选年份字段：四位年份、区间、逗号列表、起始/步长。预览不改变提交给调度器的表达式。 */
function parseYears(field: string): number[] {
  const years = new Set<number>()
  for (const part of field.split(',')) {
    const match = /^(\*|\d{4}(?:-\d{4})?)(?:\/(\d+))?$/.exec(part)
    if (!match) throw new Error('年份表达式错误')
    const [range, interval] = [match[1], Number(match[2] ?? 1)]
    const [start, finish] = range === '*' ? [1970, 9999] : range.split('-').map(Number)
    const end = finish ?? (match[2] ? 9999 : start)
    if (start < 1970 || end > 9999 || start > end || !Number.isSafeInteger(interval) || interval < 1) {
      throw new Error('年份范围或步长错误')
    }
    for (let year = start; year <= end; year += interval) years.add(year)
  }
  return [...years].sort((a, b) => a - b)
}

/** 调度专用：只由调度页面加载，不放回公共 utils 桶中。 */
export function getNextCronTimes(expression: string, currentDate = new Date()): string[] {
  const fields = expression.trim().split(/\s+/)
  if (fields.length < 5 || fields.length > 7) throw new Error('表达式字段数错误')
  const cron = (fields.length === 7 ? fields.slice(0, 6) : fields).join(' ')
  const iterator = CronParser.parseExpression(cron, { currentDate })
  if (fields.length !== 7 || fields[6] === '*') {
    return Array.from({ length: 5 }, () => dayjs(iterator.next().toDate()).format(DATETIME_FORMAT))
  }
  const result: string[] = []
  // 直接跳到允许年份，避免从当前日期逐秒迭代到几十年后的目标。
  for (const year of parseYears(fields[6])) {
    if (year < currentDate.getFullYear()) continue
    const start = new Date(Math.max(currentDate.getTime(), new Date(year, 0, 1).getTime() - 1))
    const window = CronParser.parseExpression(cron, { currentDate: start, endDate: new Date(year, 11, 31, 23, 59, 59, 999) })
    while (result.length < 5 && window.hasNext()) result.push(dayjs(window.next().toDate()).format(DATETIME_FORMAT))
    if (result.length === 5) break
  }
  return result
}

export function parseCron(expression: string): string {
  try {
    return getNextCronTimes(expression).join('\n') || '无执行时间'
  } catch {
    return '表达式错误'
  }
}
