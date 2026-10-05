package com.goatilicious.models;

import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {

    private ItemPedido item(String preco, int quantidade) {
        Produto p = new Produto();
        p.setSabor("Cereja");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal(preco));

        ItemPedido i = new ItemPedido();
        i.setProduto(p);
        i.setQuantidade(quantidade);
        i.setPrecoAplicado(new BigDecimal(preco));
        return i;
    }

    @Test
    void pedidoNovoNaoTemItensETemTotalZero() {
        Pedido pedido = new Pedido();

        assertTrue(pedido.getItens().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(pedido.getTotal()));
    }

    @Test
    void adicionarItemLigaOItemAoPedido() {
        Pedido pedido = new Pedido();
        ItemPedido item = item("350.00", 1);

        pedido.adicionarItem(item);

        assertEquals(1, pedido.getItens().size());
        assertSame(pedido, item.getPedido());
    }

    @Test
    void totalSomaPrecoAplicadoVezesQuantidade() {
        Pedido pedido = new Pedido();
        pedido.adicionarItem(item("350.00", 2));   // 700
        pedido.adicionarItem(item("650.00", 1));   // 650

        assertEquals(0, new BigDecimal("1350.00").compareTo(pedido.getTotal()));
    }

    @Test
    void subtotalDoItemEPrecoVezesQuantidade() {
        assertEquals(0, new BigDecimal("1050.00").compareTo(item("350.00", 3).getSubtotal()));
    }
}
