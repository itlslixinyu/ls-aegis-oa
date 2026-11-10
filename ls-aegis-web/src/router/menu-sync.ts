import type { Router } from 'vue-router'
import { useRouteStore, useTabsStore } from '@/stores'
import { isRetiredNavigation } from '@/utils/retired-navigation'

/** Refresh already-open clients after menu deployments without resetting their sessions. */
export function setupMenuSynchronization(router: Router) {
  const synchronize = () => {
    if (document.visibilityState === 'hidden') return
    const routes = useRouteStore()
    routes.pruneRetiredRoutes(router)
    useTabsStore().pruneRetiredNavigation()
    if (isRetiredNavigation(router.currentRoute.value)) void router.replace('/dashboard/workplace')
    void routes.refreshRoutes(router).catch(() => {
      // Keep working menus on network failure; a later event can retry after one minute.
    })
  }
  const removeAfterEach = router.afterEach(synchronize)
  window.addEventListener('focus', synchronize)
  document.addEventListener('visibilitychange', synchronize)
  synchronize()
  return () => {
    removeAfterEach()
    window.removeEventListener('focus', synchronize)
    document.removeEventListener('visibilitychange', synchronize)
  }
}
