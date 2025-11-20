import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const DEV_PROXY_TARGET =
  process.env.AIRHOTEL_API ?? process.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/hotels': {
        target: DEV_PROXY_TARGET,
        changeOrigin: true
      },
      '/reservations': {
        target: DEV_PROXY_TARGET,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
});
