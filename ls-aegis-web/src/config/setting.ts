export const defaultSettings: App.AppSettings = {
  theme: 'light',
  themeColor: '#165DFF',
  tab: true,
  tabMode: 'rounded',
  animate: false,
  animateMode: 'zoom-fade',
  menuCollapse: false,
  menuAccordion: true,
  menuDark: false,
  copyrightDisplay: true,
  layout: 'left',
  showBreadcrumb: true,
  tableBordered: true,
  tableSize: 'large',
  isOpenWatermark: false,
  enableColorWeaknessMode: false,
  enableMourningMode: false,
}
// 根据环境返回配置
export const getSettings = (): App.AppSettings => {
  return defaultSettings
}
