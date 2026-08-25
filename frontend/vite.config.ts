import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Em desenvolvimento o front chama /api e o Vite repassa para o Spring Boot,
    // entao nao existe CORS nem variavel de ambiente para configurar localmente.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },

  // O mesmo proxy no `vite preview`: sem isto, testar o build de producao
  // localmente derruba todas as chamadas de API em 404.
  preview: {
    port: 4173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
