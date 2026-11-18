import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // Same-origin calls to the API in dev, like Nginx will do later.
      // Cookie auth and CSRF (decisions/0009) rely on same-origin requests.
      '/api': {
        target: 'http://localhost:8082',
        // Keep the browser's host (localhost:5173) and tell the API about it. The API builds the GitHub
        // callback URL from it, and the callback must come back through this same origin.
        changeOrigin: false,
        xfwd: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: false,
  },
})
