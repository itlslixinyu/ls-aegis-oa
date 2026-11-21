import { computed } from 'vue'
import { useRouteStore } from '@/stores'
import { workbenchRoutePaths } from '@/utils/workbench-route-access'

export function useWorkbenchAccess() {
  const routeStore = useRouteStore()
  const paths = computed(() => workbenchRoutePaths(routeStore.routes))
  const canOpen = (path: string) => paths.value.has(path)
  const canOpenAny = (targets: string[]) => targets.some(canOpen)
  return { canOpen, canOpenAny }
}
