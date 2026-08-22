/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

const apiOrigin = process.env.API_ORIGIN ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [svelte()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: apiOrigin, changeOrigin: true }
    }
  },
  test: {
    environment: 'jsdom',
    include: ['tests/**/*.test.ts']
  }
});
