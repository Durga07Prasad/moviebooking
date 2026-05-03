import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,         // try 3000 first
    strictPort: false,  // fallback to next port if 3000 is busy
    proxy: {
      // Forward all /api requests to Spring Boot — this avoids all CORS issues
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
