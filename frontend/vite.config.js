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
        clienteLogin: resolve(import.meta.dirname, 'cliente/login.html'),
        clienteMeusPedidos: resolve(import.meta.dirname, 'cliente/meus-pedidos.html'),
        clienteCadastro: resolve(import.meta.dirname, 'cliente/cadastro.html'),
        clientePedido: resolve(import.meta.dirname, 'cliente/pedido.html'),
        adminLogin: resolve(import.meta.dirname, 'admin/login.html'),
        adminDashboard: resolve(import.meta.dirname, 'admin/dashboard.html'),
        adminIndex: resolve(import.meta.dirname, 'admin/index.html'),
        adminClientes: resolve(import.meta.dirname, 'admin/clientes.html'),
        adminPedidos: resolve(import.meta.dirname, 'admin/gerir-pedidos.html'),
        adminFacturas: resolve(import.meta.dirname, 'admin/facturas.html'),
      },
    },
  },
});
