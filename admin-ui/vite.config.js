import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/upload': {
        target: 'http://localhost:8080/api',
        changeOrigin: true
      },
      // 你可以根据需要继续添加其它接口代理
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})