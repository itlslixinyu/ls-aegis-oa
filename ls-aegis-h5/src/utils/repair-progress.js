/** Only recorded events represent progress; settlement completion is not customer consent. */
export function buildRepairProgress(apply, order, confirmation, formatTime) {
  if (!order) return []
  const steps = [{ title: '提交报修', text: formatTime(apply?.applyTime) }]
  if (order.acceptTime) steps.push({ title: '工程师接单', text: formatTime(order.acceptTime) })
  if (order.startTime) steps.push({ title: '开始维修', text: formatTime(order.startTime) })
  if (order.finishTime) steps.push({ title: '维修完成', text: formatTime(order.finishTime) })
  if (confirmation?.confirmed === true) {
    steps.push({ title: '客户确认', text: formatTime(confirmation.confirmedAt) })
  }
  if (order.status === 'CLOSED') steps.push({ title: '工单关闭', text: '已关闭' })
  return steps
}
