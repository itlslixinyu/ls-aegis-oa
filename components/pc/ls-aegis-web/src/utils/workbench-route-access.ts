export interface WorkbenchRoute {
  path: string
  children?: WorkbenchRoute[]
}

/** Workbench shortcuts follow the current user's installed menu routes. */
export function workbenchRoutePaths(routes: WorkbenchRoute[], parent = ''): Set<string> {
  const paths = new Set<string>()
  for (const route of routes) {
    if (/^(?:https?:)?\/\//i.test(route.path)) continue
    const path = (route.path.startsWith('/') ? route.path : `${parent}/${route.path}`)
      .replace(/\/{2,}/g, '/').replace(/\/$/, '') || '/'
    paths.add(path)
    for (const child of workbenchRoutePaths(route.children || [], path)) paths.add(child)
  }
  return paths
}
