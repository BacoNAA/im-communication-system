import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import vueDevTools from 'vite-plugin-vue-devtools';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue({
      script: {
        defineModel: true,
        propsDestructure: true,
      },
    }),
    vueJsx(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@components': fileURLToPath(new URL('./src/components', import.meta.url)),
      '@utils': fileURLToPath(new URL('./src/utils', import.meta.url)),
      '@api': fileURLToPath(new URL('./src/api', import.meta.url)),
      '@types': fileURLToPath(new URL('./src/types', import.meta.url)),
      '@composables': fileURLToPath(new URL('./src/composables', import.meta.url)),
      'group': fileURLToPath(new URL('./src/components/group', import.meta.url)),
    },
  },
  server: {
    port: 3000,
    open: true,
    cors: true,
    proxy: {
      // API 接口代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path,
      },
      // 原生WebSocket连接代理
      '/ws-native': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      // SockJS WebSocket 连接代理
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      // STOMP WebSocket 端点代理
      '/app': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      // 消息订阅端点代理
      '/topic': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      '/queue': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      '/user': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
      // 文件上传和静态资源代理
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // 图片资源代理
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  build: {
    target: 'esnext',
    minify: 'esbuild',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          utils: ['@/utils/index'],
        },
      },
    },
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia'],
  },
});
