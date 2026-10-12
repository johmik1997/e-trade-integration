import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // Same-origin in dev, so photo URLs returned by the API ("/api/v1/photos/...")
    // can be dropped straight into <img src> without any base-URL juggling.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
