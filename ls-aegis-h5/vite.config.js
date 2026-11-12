import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import path from 'path'

/** 同源反代：去掉浏览器 Origin，避免后端 CORS 把 H5 开发端口判为跨域（403 Invalid CORS request） */
function stripOrigin() {
  return {
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => {
        proxyReq.removeHeader('origin')
      })
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
    const baseUrl = env.VITE_API_BASE_URL || process.env.VITE_API_BASE_URL || 'http://127.0.0.1:8000'
  const hmrClientPort = env.VITE_HMR_CLIENT_PORT || process.env.VITE_HMR_CLIENT_PORT
  // customer→5174 / engineer→5175；与 PC(5173) 错开
  const role = env.VITE_H5_ROLE === 'engineer' ? 'engineer' : 'customer'
  const port = Number(env.VITE_DEV_PORT || (role === 'engineer' ? 5175 : 5174))

  const apiProxy = {
    target: baseUrl,
    changeOrigin: true,
    ...stripOrigin(),
  }

  // 生产路径反代可用：BASE_PATH=/h5/ 或 /h5-engineer/（Docker ARG）
  const base = env.BASE_PATH || process.env.BASE_PATH || '/'

  return {
    base,
    plugins: [
      vue(),
      Components({
        resolvers: [VantResolver()]
      })
    ],
    define: {
      __H5_ROLE__: JSON.stringify(role),
    },
    resolve: {
      alias: {
        '#h5-routes': path.resolve(__dirname, `src/router/${role}-routes.js`),
        '@': path.resolve(__dirname, 'src'),
      },
      dedupe: ['vue', 'vue-router', 'pinia'],
    },
    optimizeDeps: {
      include: ['vue', 'vue-router', 'pinia', 'vant', 'axios', 'sm-crypto'],
    },
    server: {
      host: '0.0.0.0',
      port,
      strictPort: true,
      watch: {
        usePolling: true,
        interval: 300,
      },
      allowedHosts: true,
      hmr: hmrClientPort ? { clientPort: Number(hmrClientPort) } : true,
      proxy: {
        '/eoms': { ...apiProxy },
        '/auth': { ...apiProxy },
        '/user/profile': { ...apiProxy },
        '/captcha': { ...apiProxy },
        '/profile': { ...apiProxy },
        '/file': { ...apiProxy },
        '/api': {
          ...apiProxy,
          rewrite: (p) => p.replace(/^\/api/, ''),
        },
        '/system': { ...apiProxy },
      },
    },
    preview: {
      host: '0.0.0.0',
      port,
      strictPort: true,
    },
    build: {
      outDir: role === 'engineer' ? 'dist-engineer' : 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      minify: 'esbuild',
      rollupOptions: {
        output: {
          manualChunks: {
            vant: ['vant'],
            vue: ['vue', 'vue-router', 'pinia'],
          },
        },
      },
    },
  }
})
