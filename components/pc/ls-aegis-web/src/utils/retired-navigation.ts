/** Product-scope removal applies to navigation snapshots from older releases too. */
export interface NavigationEntry {
  path?: string
  name?: unknown
  id?: string | number
  component?: unknown
  permission?: string
  children?: NavigationEntry[]
}
const retiredNames = new Set(['Code', 'CodeGenerator', 'Open', 'OpenApp', 'Tenant', 'TenantManagement', 'TenantPackage'])
const retiredIds = new Set(['3000', '3010', '3011', '3012', '3013', '3014', '3015', '3016', '3020', '3021', '3022', '3023', '3024', '3025', '7000', '7010', '7011', '7012', '7013', '7014', '7015', '7016', '7017', '7018', '9000', '9010', '9011', '9012', '9013', '9014'])

export function isRetiredNavigation(entry: NavigationEntry) {
  return /^\/(?:code|open|tenant)(?:\/|$)/.test((entry.path || '').split(/[?#]/)[0])
    || [entry.component, entry.permission].some((value) => typeof value === 'string' && /^tenant(?:\/|:|$)/.test(value))
    || retiredNames.has(String(entry.name || '')) || retiredIds.has(String(entry.id || ''))
}
export function activeNavigation<T extends NavigationEntry>(entries: T[]): T[] {
  return entries.filter((entry) => !isRetiredNavigation(entry)).map((entry) => ({
    ...entry, ...(entry.children ? { children: activeNavigation(entry.children) } : {}),
  } as T))
}
export function activeNavigationCaches<T>(names: T[]): T[] {
  return names.filter((name) => !isRetiredNavigation({ name, path: typeof name === 'string' ? name : '' }))
}
/** Touch only navigation keys; keep tokens, preferences and business tabs. */
export function cleanRetiredNavigationStorage(session: Pick<Storage, 'getItem' | 'setItem'>, local: Pick<Storage, 'getItem' | 'setItem'> & Partial<Pick<Storage, 'removeItem'>>) {
  // Discard the obsolete module state, retaining login and business storage.
  try { local.removeItem?.('tenant') } catch { /* Storage may be unavailable. */ }
  for (const [storage, key] of [[session, 'tabs'], [local, 'layout-search-history']] as const) {
    try {
      const raw = storage.getItem(key)
      if (!raw) continue
      const saved = JSON.parse(raw)
      let cleaned
      if (key === 'tabs' && saved && !Array.isArray(saved) && typeof saved === 'object') {
        cleaned = { ...saved,
          ...(Array.isArray(saved.tabList) ? { tabList: activeNavigation(saved.tabList) } : {}),
          ...(Array.isArray(saved.cacheList) ? { cacheList: activeNavigationCaches(saved.cacheList) } : {}),
        }
      } else if (key === 'layout-search-history' && Array.isArray(saved)) cleaned = activeNavigation(saved)
      if (cleaned && JSON.stringify(cleaned) !== JSON.stringify(saved)) storage.setItem(key, JSON.stringify(cleaned))
    } catch { /* Unavailable storage or malformed old JSON must not erase unrelated state. */ }
  }
}
