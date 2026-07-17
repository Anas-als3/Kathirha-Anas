import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,            // listen on the LAN so a phone can reach it
    allowedHosts: true,    // accept ngrok / tunnel hostnames in dev
    proxy: {
      // Backend port. 8080 is taken on this machine, so default to 8099.
      // Override with: $env:VITE_PROXY_TARGET='http://localhost:8080'
      '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8099', changeOrigin: true },
    },
  },
})
