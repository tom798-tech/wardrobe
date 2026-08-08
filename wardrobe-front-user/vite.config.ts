import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

// Vite 5 + Vue 3.5 + TS 配置
// 本地开发端口 7076（与原 Vue CLI 配置保持一致），生产构建产物 dist/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET ?? 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      host: true,
      port: 7076,
      strictPort: true,
      cors: true,
      proxy: {
        // 所有 /api 开头的请求转发给后端（和原 vue.config.js 行为一致）
        // pathRewrite 把 /api 前缀去掉，例如 /api/clothes -> http://backend:8080/clothes
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api/, ''),
        },
      },
    },
    build: {
      sourcemap: false,
      outDir: 'dist',
      chunkSizeWarningLimit: 2048,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            element: ['element-plus', '@element-plus/icons-vue'],
          },
        },
      },
    },
  }
})
