import { resolve } from 'path';
import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 5173,
    open: true,
  },
  build: {
    target: 'es2022',
    outDir: 'dist',
    sourcemap: true,
    chunkSizeWarningLimit: 1100,
    rollupOptions: {
      input: {
        main: resolve(import.meta.dirname, 'index.html'),
        clienteLanding: resolve(import.meta.dirname, 'cliente/index.html'),
        clienteCadastro: resolve(import.meta.dirname, 'cliente/cadastro.html'),
        clientePedido: resolve(import.meta.dirname, 'cliente/pedido.html'),
        adminIndex: resolve(import.meta.dirname, 'admin/index.html'),
        adminClientes: resolve(import.meta.dirname, 'admin/clientes.html'),
        adminPedidos: resolve(import.meta.dirname, 'admin/gerir-pedidos.html'),
      },
    },
  },
});
