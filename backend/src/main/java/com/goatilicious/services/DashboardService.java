package com.goatilicious.services;

import com.goatilicious.dtos.DashboardResposta;
import com.goatilicious.dtos.DashboardResposta.ClienteComFacturas;
import com.goatilicious.dtos.DashboardResposta.PedidosPorEstado;
import com.goatilicious.dtos.DashboardResposta.ProdutoMaisPedido;
import com.goatilicious.dtos.DashboardResposta.Receita;
import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.StatusPagamento;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Números do painel do staff. Os volumes são pequenos: agrega-se em memória (KISS). */
@Service
public class DashboardService {

    static final int TOP = 5;

    private final PedidoRepository pedidoRepository;
    private final FacturaRepository facturaRepository;

    public DashboardService(PedidoRepository pedidoRepository, FacturaRepository facturaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.facturaRepository = facturaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResposta resumo() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        List<Factura> finais = facturaRepository.findAll().stream()
                .filter(f -> f.getEstado() == EstadoFactura.FINAL)
                .filter(f -> f.getPedido().getStatus() != StatusPedido.CANCELADO)
                .toList();

        return new DashboardResposta(receita(finais), pedidosPorEstado(pedidos),
                produtosMaisPedidos(pedidos), clientesComMaisFacturas(finais));
    }

    private static BigDecimal valor(Factura f) {
        return f.getValorTotal() == null ? BigDecimal.ZERO : f.getValorTotal();
    }

    private Receita receita(List<Factura> finais) {
        BigDecimal total = finais.stream().map(DashboardService::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paga = finais.stream()
                .filter(f -> f.getStatusPagamento() == StatusPagamento.PAGO)
                .map(DashboardService::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Receita(total, paga, total.subtract(paga), finais.size());
    }

    private PedidosPorEstado pedidosPorEstado(List<Pedido> pedidos) {
        long feitos = pedidos.stream()
                .filter(p -> p.getStatus() == StatusPedido.REVISADO || p.getStatus() == StatusPedido.ENVIADO).count();
        long pendentes = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.PENDENTE).count();
        long cancelados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count();
        return new PedidosPorEstado(pedidos.size(), feitos, pendentes, cancelados);
    }

    /** Produtos (sabor + tamanho) mais pedidos, por quantidade, sem contar pedidos cancelados. */
    private List<ProdutoMaisPedido> produtosMaisPedidos(List<Pedido> pedidos) {
        Map<Long, long[]> quantidades = new LinkedHashMap<>();
        Map<Long, Produto> produtos = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            if (pedido.getStatus() == StatusPedido.CANCELADO) continue;
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = item.getProduto();
                produtos.putIfAbsent(produto.getIdProduto(), produto);
                quantidades.computeIfAbsent(produto.getIdProduto(), k -> new long[1])[0] += item.getQuantidade();
            }
        }
        return produtos.values().stream()
                .map(p -> new ProdutoMaisPedido(p.getIdProduto(), p.getSabor(), p.getTamanho(),
                        quantidades.get(p.getIdProduto())[0]))
                .sorted(Comparator.comparingLong(ProdutoMaisPedido::quantidade).reversed()
                        .thenComparing(ProdutoMaisPedido::sabor)
                        .thenComparing(ProdutoMaisPedido::tamanho))
                .limit(TOP)
                .toList();
    }

    /** Clientes com mais faturas finais; empate resolve-se pelo maior valor faturado e depois pelo nome. */
    private List<ClienteComFacturas> clientesComMaisFacturas(List<Factura> finais) {
        Map<Long, Cliente> clientes = new LinkedHashMap<>();
        Map<Long, Long> contagem = new LinkedHashMap<>();
        Map<Long, BigDecimal> valores = new LinkedHashMap<>();
        for (Factura f : finais) {
            Cliente c = f.getPedido().getCliente();
            clientes.putIfAbsent(c.getIdCliente(), c);
            contagem.merge(c.getIdCliente(), 1L, Long::sum);
            valores.merge(c.getIdCliente(), valor(f), BigDecimal::add);
        }
        return clientes.values().stream()
                .map(c -> new ClienteComFacturas(c.getIdCliente(), c.getNome(),
                        contagem.get(c.getIdCliente()), valores.get(c.getIdCliente())))
                .sorted(Comparator.comparingLong(ClienteComFacturas::facturas).reversed()
                        .thenComparing(ClienteComFacturas::valorTotal, Comparator.reverseOrder())
                        .thenComparing(ClienteComFacturas::nome))
                .limit(TOP)
                .toList();
    }
}
