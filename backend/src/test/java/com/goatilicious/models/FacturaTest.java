package com.goatilicious.models;

import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.StatusPagamento;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FacturaTest {

    private Pedido pedidoComDoisItensDe350() {
        Produto p = new Produto();
        p.setSabor("Cereja");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal("350.00"));

        ItemPedido item = new ItemPedido();
        item.setProduto(p);
        item.setQuantidade(2);
        item.setPrecoAplicado(new BigDecimal("350.00"));

        Pedido pedido = new Pedido();
        pedido.adicionarItem(item);
        return pedido;
    }

    @Test
    void rascunhoDeUsaTotalDoPedidoECondicoesPorDefeito() {
        Pedido pedido = pedidoComDoisItensDe350();

        Factura factura = Factura.rascunhoDe(pedido);

        assertSame(pedido, factura.getPedido());
        assertEquals(0, new BigDecimal("700.00").compareTo(factura.getValorTotal()));
        assertEquals(CondicaoPagamento.A_VISTA, factura.getCondicaoPagamento());
        assertEquals(EstadoFactura.RASCUNHO, factura.getEstado());
        assertEquals(StatusPagamento.PENDENTE, factura.getStatusPagamento());
    }

    @Test
    void finalizarMudaEstadoCondicaoETotal() {
        Factura factura = Factura.rascunhoDe(pedidoComDoisItensDe350());

        factura.finalizar(CondicaoPagamento.CARTAO, new BigDecimal("1050.00"));

        assertEquals(EstadoFactura.FINAL, factura.getEstado());
        assertEquals(CondicaoPagamento.CARTAO, factura.getCondicaoPagamento());
        assertEquals(0, new BigDecimal("1050.00").compareTo(factura.getValorTotal()));
        assertNotNull(factura.getDataEmissao());
    }

    @Test
    void finalizarNaoAlteraStatusPagamento() {
        Factura factura = Factura.rascunhoDe(pedidoComDoisItensDe350());

        factura.finalizar(CondicaoPagamento.PAGO_NA_ENTREGA, new BigDecimal("700.00"));

        assertEquals(StatusPagamento.PENDENTE, factura.getStatusPagamento());
    }
}
