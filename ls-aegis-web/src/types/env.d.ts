/// <reference types="vite/client" />

/** 声明环境变量的类型 */
interface ImportMetaEnv {
  readonly VITE_API_PREFIX: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_WS_URL: string
  readonly VITE_BASE: string
  readonly VITE_APP_SETTING: string
  readonly VITE_CLIENT_ID: string
  readonly VITE_OPEN_DEVTOOLS: string
  /** 本地开发是否启用 vite-plugin-mock（仅 `true` 开启） */
  readonly VITE_LOCAL_MOCK: string
  /** 生产构建是否打入 mock */
  readonly VITE_BUILD_MOCK: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
