import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Frontend dev server runs on 3000 (matches the gateway CORS allowed origin).
// All API + auth traffic is proxied to the API Gateway (8080). The interview
// WebSocket is proxied straight to interview-service (8083) in dev — the gateway
// only load-balances /ws in prod, and interview-service validates the JWT itself
// on the STOMP CONNECT frame, so hitting it directly in dev is equivalent.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
