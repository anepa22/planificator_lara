import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  // AnepaVps Ingress: /dev/planificator — override con VITE_BASE=/
  base: process.env.VITE_BASE || '/',
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://100.92.148.40:8880',
        changeOrigin: true,
      },
    },
  },
})
