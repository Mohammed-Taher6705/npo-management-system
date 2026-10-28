import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// All /api calls are forwarded to the Spring Boot backend (no CORS setup needed)
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
