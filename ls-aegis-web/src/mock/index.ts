import { createProdMockServer } from 'vite-plugin-mock/client'

import area from './area'

export async function setupProdMockServer() {
  await createProdMockServer([...area])
}
