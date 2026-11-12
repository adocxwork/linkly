import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Proxy keys are path prefixes: '/r/' (not '/r'), otherwise '/register' would be proxied too.
    proxy: {
      '/api/': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/r/': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
