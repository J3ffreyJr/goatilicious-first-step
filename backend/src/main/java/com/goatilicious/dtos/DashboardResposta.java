package com.goatilicious.dtos;

import com.goatilicious.enums.TamanhoProduto;

import java.math.BigDecimal;
import java.util.List;

/** Resumo do painel do staff. Receita e ranking de clientes contam só faturas FINAL de pedidos não cancelados. */
public record DashboardResposta(
        Receita receita,
        PedidosPorEstado pedidos,
        List<ProdutoMaisPedido> produtosMaisPedidos,
        List<ClienteComFacturas> clientesComMaisFacturas) {

    public record Receita(BigDecimal total, BigDecimal paga, BigDecimal pendente, long facturasFinais) {
    }

    /** feitos = REVISADO + ENVIADO. */
    public record PedidosPorEstado(long total, long feitos, long pendentes, long cancelados) {
    }

    public record ProdutoMaisPedido(Long idProduto, String sabor, TamanhoProduto tamanho, long quantidade) {
    }

    public record ClienteComFacturas(Long idCliente, String nome, long facturas, BigDecimal valorTotal) {
    }
}
