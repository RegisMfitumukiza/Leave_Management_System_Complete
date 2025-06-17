import path from "path"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/api/oauth2': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/oauth2': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/api/leave': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false
      },
      '/api/leave-balances': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false
      }
    },
  },
})
