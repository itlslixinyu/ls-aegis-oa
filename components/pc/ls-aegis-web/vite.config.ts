import { URL, fileURLToPath } from 'node:url'
import { defineConfig, loadEnv, type UserConfig } from 'vite'
import createVitePlugins from './config/plugins'

export default defineConfig(async ({ command, mode }): Promise<UserConfig> => {
  const env = loadEnv(mode, process.cwd()) as ImportMetaEnv
  const apiPrefix = env.VITE_API_PREFIX || '/api'

  return {
    // 开发或生产环境服务的公共基础路径
    base: env.VITE_BASE,
    // 路径别名（@/api/* → apis/*：兼容若依；勿匹配 @/apis）
    resolve: {
      alias: [
        {
          find: /^@\/api\//,
          replacement: `${fileURLToPath(new URL('./src/apis/', import.meta.url))}`,
        },
        { find: '~', replacement: fileURLToPath(new URL('./', import.meta.url)) },
        { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) },
      ],
    },
    // 引入sass全局样式变量
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/styles/var.scss" as *;`,
          api: 'modern-compiler',
        },
      },
    },
    // 依赖预构建：加快冷启动与 HMR
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'axios',
        'dayjs',
        'lodash-es',
        'echarts',
        '@arco-design/web-vue',
        '@arco-design/web-vue/es/icon',
        'vue-draggable-plus',
        '@vueuse/core',
        'query-string',
        'nprogress',
      ],
    },
    server: {
      host: true,
      port: 5173,
      strictPort: false,
      open: true,
      // 热更新
      hmr: {
        overlay: true,
      },
      // /api → 本机后端 8000（见 .env.development；完整 Docker 栈才是 18000）
      proxy: {
        [apiPrefix]: {
          target: env.VITE_API_BASE_URL,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(new RegExp(`^${apiPrefix}`), ''),
        },
      },
    },
    plugins: await createVitePlugins(env, command === 'build'),
    // 构建（仅部署上线时执行 npm run build）
    build: {
      outDir: 'dist',
      // 用于核对入口/业务页的静态依赖；报告不把全部产物误算成首屏下载。
      manifest: true,
      sourcemap: false,
      // 关闭 gzip 体积统计，加快打包
      reportCompressedSize: false,
      chunkSizeWarningLimit: 2000,
      // esbuild 压缩明显快于 terser
      minify: 'esbuild',
      target: 'es2015',
      cssCodeSplit: true,
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return
            }
            if (id.includes('echarts') || id.includes('zrender')) {
              return 'echarts'
            }
            // vue 与 arco 必须同 chunk：拆开会形成循环依赖，运行时报
            // Cannot access 'xx' before initialization（页面一直转圈）
            if (
              id.includes('/vue/')
              || id.includes('/vue-router/')
              || id.includes('/pinia/')
              || id.includes('/@vue/')
              || id.includes('@arco-design')
            ) {
              return 'vue-vendor'
            }
            if (
              id.includes('lodash-es')
              || id.includes('dayjs')
              || id.includes('axios')
              || id.includes('query-string')
            ) {
              return 'utils'
            }
          },
        },
      },
    },
    esbuild: {
      // 生产构建去掉 console / debugger
      drop: command === 'build' ? ['console', 'debugger'] : [],
    },
    // 以 envPrefix 开头的环境变量会通过 import.meta.env 暴露在你的客户端源码中。
    envPrefix: ['VITE', 'FILE'],
  }
})
