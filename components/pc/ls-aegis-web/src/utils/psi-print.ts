/**
 * 打开新窗口打印 HTML
 */
export function printHtml(html: string, title?: string) {
  const win = window.open('', '_blank')
  if (!win) {
    throw new Error('浏览器拦截了打印窗口，请允许弹窗后重试')
  }
  win.document.open()
  win.document.write(html)
  win.document.close()
  if (title) {
    win.document.title = title
  }
  // 等待样式解析后再调打印
  win.focus()
  setTimeout(() => {
    win.print()
  }, 200)
}
