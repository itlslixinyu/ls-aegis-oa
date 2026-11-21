/** Work-order service-state backgrounds follow the existing workbench palette. */
const backgrounds = Object.freeze({
  WAIT_ACCEPT: '#e99b39',
  ACCEPTED: '#4b7ee1',
  PROCESSING: '#7359ce',
  COMPLETED: '#34a978',
  CLOSED: '#7f8a9a'
})
export function getWorkOrderStatusBackground(status) {
  // Finishing service does not imply that financial settlement has finished.
  const key = ['WAIT_SETTLEMENT', 'FINISHED'].includes(status) ? 'COMPLETED' : status
  return backgrounds[key] || '#969799'
}
