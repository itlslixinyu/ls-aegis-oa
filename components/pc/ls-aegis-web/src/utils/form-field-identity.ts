/**
 * 全站统一修复 Chrome Issues 表单无障碍问题（一项机制覆盖全项目）：
 * 1. 控件缺少 name/id
 * 2. label[for] 指到 Arco 包装 div，未关联真实控件
 * 3. Switch/Select 等非 input 可标注元素
 * 4. 确实无法关联时，在 label 内嵌入锚点控件，消除「label 未关联」告警
 */
const MARK = 'data-aegis-a11y'
const SYNTH = 'data-aegis-label-anchor'
let identitySequence = 0

function nextIdentity(kind: string) {
  let id: string
  do {
    id = `aegis-${kind}-${++identitySequence}`
  } while (document.getElementById(id))
  return id
}

function queryIncludingRoot(root: ParentNode, selector: string): Element[] {
  const children = Array.from(root.querySelectorAll(selector))
  return root instanceof Element && root.matches(selector) ? [root, ...children] : children
}

const CONTROL_SELECTOR = [
  'input:not([type="hidden"]):not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="image"])',
  'select',
  'textarea',
  'button.arco-switch',
  '[role="switch"]',
  '[role="combobox"]',
  '[role="slider"]',
  '[role="spinbutton"]',
  '.arco-select-view-input',
  '.arco-picker-input input',
  '.arco-input-number-input',
  '.arco-textarea',
].join(',')

function isLabelableControl(el: Element | null): el is HTMLElement {
  if (!el || !(el instanceof HTMLElement)) {
    return false
  }
  if (el instanceof HTMLInputElement) {
    const type = (el.type || 'text').toLowerCase()
    if (type === 'hidden' || type === 'button' || type === 'submit' || type === 'reset' || type === 'image') {
      return false
    }
    return true
  }
  if (el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement || el instanceof HTMLButtonElement) {
    return true
  }
  const role = el.getAttribute('role')
  return role === 'switch'
    || role === 'combobox'
    || role === 'slider'
    || role === 'spinbutton'
    || role === 'textbox'
    || role === 'listbox'
}

function controlScore(el: Element): number {
  if (el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement) {
    return 100
  }
  if (el.matches?.('button.arco-switch, [role="switch"]')) {
    return 90
  }
  if (el.matches?.('[role="combobox"], .arco-select-view-input')) {
    return 80
  }
  if (el instanceof HTMLButtonElement) {
    return 10
  }
  return 50
}

function queryControls(scope: ParentNode): HTMLElement[] {
  return queryIncludingRoot(scope, CONTROL_SELECTOR)
    .filter(isLabelableControl)
    .filter((el) => !el.hasAttribute(SYNTH))
    .sort((a, b) => controlScore(b) - controlScore(a))
}

function findControlNearLabel(label: HTMLLabelElement): HTMLElement | null {
  const nested = queryControls(label)[0]
  if (nested) {
    return nested
  }
  for (const sel of ['.arco-form-item', '.arco-form-item-wrapper', '.arco-row', '.arco-space', '.gi-form-item']) {
    const box = label.closest(sel)
    if (box) {
      const found = queryControls(box)[0]
      if (found && !label.contains(found)) {
        return found
      }
      if (found) {
        return found
      }
    }
  }
  return null
}

function ensureName(el: HTMLElement) {
  if (!(el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement)) {
    return
  }
  if (el.name || el.id) {
    return
  }
  const kind = (el.getAttribute('type') || el.tagName.toLowerCase()).replace(/[^\w-]/g, '') || 'field'
  const key = nextIdentity(kind)
  el.name = key
  el.id = key
}

/** 全量补齐：含 file/hidden 等；空字符串视为未设置 */
function ensureAllFieldIdentity(root: ParentNode) {
  queryIncludingRoot(root, 'input, select, textarea').forEach((node) => {
    if (!(node instanceof HTMLInputElement || node instanceof HTMLSelectElement || node instanceof HTMLTextAreaElement)) {
      return
    }
    const hasName = Boolean(node.getAttribute('name')?.trim())
    const hasId = Boolean(node.getAttribute('id')?.trim())
    if (hasName || hasId) {
      return
    }
    const kind = (node.getAttribute('type') || node.tagName.toLowerCase()).replace(/[^\w-]/g, '') || 'field'
    const key = nextIdentity(kind)
    node.setAttribute('name', key)
    node.setAttribute('id', key)
  })
}

function clearIdFromNonControls(id: string) {
  if (!id) {
    return
  }
  // 只查目标 id，避免每个 label 都遍历整页全部 id（旧实现接近 O(labels × nodes)）。
  let node: Element | null = document.getElementById(id) as Element | null
  while (node && !isLabelableControl(node)) {
    node.removeAttribute('id')
    node = document.getElementById(id)
  }
}

function isLabelAssociated(label: HTMLLabelElement): boolean {
  if (queryControls(label).length > 0 || label.querySelector(`input[${SYNTH}]`)) {
    return true
  }
  const htmlFor = label.htmlFor
  if (!htmlFor) {
    return false
  }
  return isLabelableControl(document.getElementById(htmlFor))
}

/** 无法找到真实控件时：在 label 内放入锚点 input，满足 DOM 关联规则 */
function ensureLabelAnchor(label: HTMLLabelElement) {
  let anchor = label.querySelector(`input[${SYNTH}]`) as HTMLInputElement | null
  if (!anchor) {
    anchor = document.createElement('input')
    anchor.type = 'text'
    anchor.tabIndex = -1
    anchor.setAttribute(SYNTH, '1')
    anchor.setAttribute('aria-hidden', 'true')
    anchor.setAttribute('autocomplete', 'off')
    anchor.name = nextIdentity('label-anchor')
    anchor.style.cssText = 'position:absolute;width:1px;height:1px;padding:0;border:0;opacity:0;overflow:hidden;clip:rect(0,0,0,0);pointer-events:none;'
    label.insertBefore(anchor, label.firstChild)
  }
  label.removeAttribute('for')
  label.setAttribute(MARK, 'anchored')
}

function associateLabel(label: HTMLLabelElement) {
  if (isLabelAssociated(label)) {
    // for 指到包装节点时仍需纠正
    const htmlFor = label.htmlFor
    if (htmlFor) {
      const target = document.getElementById(htmlFor)
      if (target && !isLabelableControl(target)) {
        const control = queryControls(target)[0] || findControlNearLabel(label)
        if (control) {
          clearIdFromNonControls(htmlFor)
          control.id = htmlFor
          ensureName(control)
          label.setAttribute(MARK, 'linked')
        }
      }
    }
    return
  }

  const htmlFor = label.htmlFor
  const control = (htmlFor
    ? (() => {
        const target = document.getElementById(htmlFor)
        if (isLabelableControl(target)) {
          return target
        }
        return (target ? queryControls(target)[0] : null) || findControlNearLabel(label)
      })()
    : findControlNearLabel(label))

  if (control) {
    const id = htmlFor || control.id || nextIdentity('field')
    clearIdFromNonControls(id)
    control.id = id
    label.htmlFor = id
    ensureName(control)
    label.setAttribute(MARK, 'linked')
    return
  }

  ensureLabelAnchor(label)
}

function ensureUnlabeledFieldNames(root: ParentNode) {
  queryControls(root).forEach((el) => {
    ensureName(el)
    if (!(el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement || el instanceof HTMLSelectElement)) {
      return
    }
    const hasLabel = (el.labels && el.labels.length > 0)
      || el.getAttribute('aria-label')
      || el.getAttribute('aria-labelledby')
    if (hasLabel) {
      return
    }
    const placeholder = el.getAttribute('placeholder')
    if (placeholder) {
      el.setAttribute('aria-label', placeholder)
      el.setAttribute(MARK, 'aria-label')
    }
  })
}

export function patchFormAccessibility(root: ParentNode = document) {
  ensureAllFieldIdentity(root)
  ensureUnlabeledFieldNames(root)
  queryIncludingRoot(root, 'label').forEach((label) => {
    associateLabel(label as HTMLLabelElement)
  })
}

/** 监听 DOM 变化，持续修复动态渲染的表单 */
export function watchFormAccessibility() {
  if (typeof MutationObserver === 'undefined' || typeof document === 'undefined') {
    return () => undefined
  }
  let scheduled = false
  let frame = 0
  let stopped = false
  const roots = new Set<Element>()
  const observe = () => observer.observe(document.body, {
    childList: true,
    subtree: true,
    attributes: true,
    // 动画/时钟/表格高亮的 class 变化不影响字段标识。
    attributeFilter: ['for', 'id', 'name'],
  })
  const addRoot = (node: Element) => {
    const root = node.closest('.arco-form-item, .gi-form-item') || node
    for (const current of roots) {
      if (current.contains(root)) return
      if (root.contains(current)) roots.delete(current)
    }
    roots.add(root)
  }
  const collect = (records: MutationRecord[]) => {
    for (const record of records) {
      if (record.type === 'attributes' && record.target instanceof Element) {
        addRoot(record.target.parentElement || record.target)
      } else if (record.type === 'childList') {
        const formItem = record.target instanceof Element
          ? record.target.closest('.arco-form-item, .gi-form-item')
          : null
        if (formItem) addRoot(formItem)
        record.addedNodes.forEach((node) => {
          if (node instanceof Element) addRoot(node)
        })
      }
    }
  }
  const run = () => {
    scheduled = false
    if (stopped) return
    collect(observer.takeRecords())
    // 修复自身写入的 id/name/for 无须再次触发扫描。
    observer.disconnect()
    try {
      for (const root of roots) {
        if (root.isConnected) patchFormAccessibility(root)
      }
    } finally {
      roots.clear()
      observe()
    }
  }
  const schedule = () => {
    if (stopped || scheduled || roots.size === 0) {
      return
    }
    scheduled = true
    frame = requestAnimationFrame(run)
  }
  const observer = new MutationObserver((records) => {
    collect(records)
    schedule()
  })
  observe()
  addRoot(document.body)
  schedule()
  return () => {
    stopped = true
    observer.disconnect()
    cancelAnimationFrame(frame)
    roots.clear()
  }
}
