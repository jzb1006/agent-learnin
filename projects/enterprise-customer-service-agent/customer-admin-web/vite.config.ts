import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/chat': 'http://127.0.0.1:8080',
      '/health': 'http://127.0.0.1:8080'
    }
  },
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true
  }
});
