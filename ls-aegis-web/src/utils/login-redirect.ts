/** Only resume a local business route; flatten login wrappers left by concurrent expiry responses. */
export const safeLoginRedirect = (value: unknown): string => {
  let target = typeof value === 'string' ? value : '/'
  for (let depth = 0; depth < 8; depth++) {
    if (!target.startsWith('/') || target.startsWith('//') || /[\\\u0000-\u001f]/.test(target)) return '/'
    let url: URL
    try { url = new URL(target, 'https://internal.invalid') } catch { return '/' }
    if (url.origin !== 'https://internal.invalid') return '/'
    if (url.pathname !== '/login') return target
    target = url.searchParams.get('redirect') || '/'
  }
  return '/'
}

interface SessionExpiryDependencies {
  getToken: () => string | undefined | null
  route: () => { path: string, fullPath: string }
  prompt: (message: string, onOk: () => Promise<void>) => void
  logout: () => Promise<unknown>
  replace: (location: { path: string, query: { redirect: string } }) => Promise<unknown>
}

/** Coalesce one expired session, and never let an old response/dialog clear a newly signed-in session. */
export const createSessionExpiryHandler = (deps: SessionExpiryDependencies) => {
  let promptedToken: string | undefined
  return (authorization: unknown, message: string) => {
    const token = deps.getToken()
    if (!token || authorization !== `Bearer ${token}` || deps.route().path === '/login' || promptedToken === token) return
    const redirect = safeLoginRedirect(deps.route().fullPath)
    promptedToken = token
    deps.prompt(message, async () => {
      try {
        if (deps.getToken() !== token) return
        await deps.logout()
        if (deps.getToken()) return
        await deps.replace({ path: '/login', query: { redirect } })
      } finally {
        if (promptedToken === token) promptedToken = undefined
      }
    })
  }
}
