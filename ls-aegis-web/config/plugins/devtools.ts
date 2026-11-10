export default async function createDevtools(env) {
  const { VITE_OPEN_DEVTOOLS } = env
  if (VITE_OPEN_DEVTOOLS !== 'true') {
    return null
  }
  const { default: VueDevTools } = await import('vite-plugin-vue-devtools')
  return VueDevTools()
}
