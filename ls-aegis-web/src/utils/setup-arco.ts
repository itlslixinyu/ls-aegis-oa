import type { App } from 'vue'
import { Card, Drawer, Message, Modal, Notification, Tooltip, Upload } from '@arco-design/web-vue'
import ArcoVueIcon from '@arco-design/web-vue/es/icon'

/** 保留函数式调用和动态图标；页面组件由构建解析器按需导入。 */
export function setupArco(app: App) {
  Card.props.bordered = false
  if (Upload.props?.name && typeof Upload.props.name === 'object') Upload.props.name.default = 'file'
  Modal._context = app._context
  Drawer._context = app._context
  Message._context = app._context
  Notification._context = app._context
  app.use(Modal).use(Drawer).use(Message).use(Notification)
  // MultipartUpload的render回调通过resolveComponent使用Tooltip。
  app.use(Tooltip)
  // 菜单和图标选择器支持后端配置的任意Arco图标，不能按源码静态名称裁剪。
  app.use(ArcoVueIcon)
}
