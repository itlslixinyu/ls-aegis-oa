/** Repair-application badge backgrounds follow the customer workbench palette. */
const backgrounds = Object.freeze({
  WAIT: '#e99b39',
  CONFIRM: '#4b7ee1',
  ORDER: '#34a978',
  CANCEL: '#7f8a9a'
})
export function getApplyStatusBackground(status) {
  return backgrounds[status] || '#969799'
}
