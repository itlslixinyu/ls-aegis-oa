/// <reference types="vite/client" />

declare const __H5_ROLE__: 'customer' | 'engineer'

interface ImportMetaEnv {
  readonly VITE_H5_ROLE?: string
  readonly VITE_DEV_PORT?: string
  readonly VITE_API_BASE_URL?: string
  readonly VITE_HMR_CLIENT_PORT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
