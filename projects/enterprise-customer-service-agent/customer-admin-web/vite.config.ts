import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiBaseUrl = process.env.CUSTOMER_AGENT_API_BASE_URL ?? 'http://127.0.0.1:8080';

export default defineConfig({
  server: {
    proxy: {
      '/admin': apiBaseUrl,
      '/api': apiBaseUrl,
      '/chat': apiBaseUrl,
      '/health': apiBaseUrl
    }
  },
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true
  }
});
