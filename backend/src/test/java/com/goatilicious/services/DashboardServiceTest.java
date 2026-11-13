package com.goatilicious.services;

import com.goatilicious.dtos.DashboardResposta;
import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.StatusPagamento;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private FacturaRepository facturaRepository;

    @InjectMocks
    private DashboardService service;

    private long proximoId = 1;

    private Cliente cliente(long id, String nome) {
        Cliente c = new Cliente();
        c.setIdCliente(id);
        c.setNome(nome);
        return c;
    }

    private Produto produto(long id, String sabor, TamanhoProduto tamanho) {
        Produto p = new Produto();
        p.setIdProduto(id);
        p.setSabor(sabor);
        p.setTamanho(tamanho);
        p.setPrecoUnitario(new BigDecimal("100.00"));
        return p;
    }

    /** Pedido com um item por produto/quantidade dados. */
    private Pedido pedido(Cliente c, StatusPedido status, Object... produtoEQuantidade) {
        Pedido p = new Pedido();
        p.setIdPedido(proximoId++);
        p.setCliente(c);
        p.setStatus(status);
        for (int i = 0; i < produtoEQuantidade.length; i += 2) {
            ItemPedido item = new ItemPedido();
            item.setProduto((Produto) produtoEQuantidade[i]);
            item.setQuantidade((Integer) produtoEQuantidade[i + 1]);
            item.setPrecoAplicado(new BigDecimal("100.00"));
            p.adicionarItem(item);
        }
        return p;
    }

    private Factura finalDe(Pedido p, String valor) {
        Factura f = Factura.rascunhoDe(p);
        f.finalizar(CondicaoPagamento.A_VISTA, new BigDecimal(valor));
        return f;
    }

    @Test
    void semDadosTudoZeroEListasVazias() {
        when(pedidoRepository.findAll()).thenReturn(List.of());
        when(facturaRepository.findAll()).thenReturn(List.of());

        DashboardResposta r = service.resumo();

        assertEquals(0, r.receita().total().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.receita().facturasFinais());
        assertEquals(0, r.pedidos().total());
        assertTrue(r.produtosMaisPedidos().isEmpty());
        assertTrue(r.clientesComMaisFacturas().isEmpty());
    }

    @Test
    void contaPedidosPorEstado() {
        Cliente ana = cliente(1, "Ana");
        List<Pedido> pedidos = List.of(
                pedido(ana, StatusPedido.PENDENTE), pedido(ana, StatusPedido.PENDENTE),
                pedido(ana, StatusPedido.REVISADO), pedido(ana, StatusPedido.ENVIADO),
                pedido(ana, StatusPedido.CANCELADO));
        when(pedidoRepository.findAll()).thenReturn(pedidos);
        when(facturaRepository.findAll()).thenReturn(List.of());

        DashboardResposta.PedidosPorEstado p = service.resumo().pedidos();

        assertEquals(5, p.total());
        assertEquals(2, p.feitos());      // REVISADO + ENVIADO
        assertEquals(2, p.pendentes());
        assertEquals(1, p.cancelados());
    }

    @Test
    void receitaSoContaFacturasFinaisDePedidosNaoCancelados() {
        Cliente ana = cliente(1, "Ana");
        Pedido revisado = pedido(ana, StatusPedido.REVISADO);
        Pedido enviado = pedido(ana, StatusPedido.ENVIADO);
        Pedido pendente = pedido(ana, StatusPedido.PENDENTE);       // fatura ainda rascunho
        Pedido cancelado = pedido(ana, StatusPedido.CANCELADO);
        Factura f1 = finalDe(revisado, "300.00");
        Factura f2 = finalDe(enviado, "200.50");
        ReflectionTestUtils.setField(f2, "statusPagamento", StatusPagamento.PAGO);
        Factura rascunho = Factura.rascunhoDe(pendente);
        Factura deCancelado = finalDe(cancelado, "999.00");
        when(pedidoRepository.findAll()).thenReturn(List.of(revisado, enviado, pendente, cancelado));
        when(facturaRepository.findAll()).thenReturn(List.of(f1, f2, rascunho, deCancelado));

        DashboardResposta.Receita r = service.resumo().receita();

        assertEquals(0, new BigDecimal("500.50").compareTo(r.total()));
        assertEquals(0, new BigDecimal("200.50").compareTo(r.paga()));
        assertEquals(0, new BigDecimal("300.00").compareTo(r.pendente()));
        assertEquals(2, r.facturasFinais());
    }

    @Test
    void produtosMaisPedidosOrdenadosPorQuantidadeComMaximoDeCinco() {
        Cliente ana = cliente(1, "Ana");
        Produto p1 = produto(1, "Cereja", TamanhoProduto.ML_500);
        Produto p2 = produto(2, "Manga", TamanhoProduto.L_1);
        Produto p3 = produto(3, "Baunilha", TamanhoProduto.ML_125);
        Produto p4 = produto(4, "Coco", TamanhoProduto.L_5);
        Produto p5 = produto(5, "Morango", TamanhoProduto.ML_500);
        Produto p6 = produto(6, "Limão", TamanhoProduto.ML_500);
        List<Pedido> pedidos = List.of(
                pedido(ana, StatusPedido.PENDENTE, p1, 2, p2, 10, p3, 1),
                pedido(ana, StatusPedido.REVISADO, p1, 4, p4, 3, p5, 2, p6, 1),
                pedido(ana, StatusPedido.CANCELADO, p3, 50));        // cancelado não conta
        when(pedidoRepository.findAll()).thenReturn(pedidos);
        when(facturaRepository.findAll()).thenReturn(List.of());

        List<DashboardResposta.ProdutoMaisPedido> top = service.resumo().produtosMaisPedidos();

        assertEquals(5, top.size());
        assertEquals("Manga", top.get(0).sabor());
        assertEquals(10, top.get(0).quantidade());
        assertEquals("Cereja", top.get(1).sabor());
        assertEquals(6, top.get(1).quantidade());
        assertEquals("Coco", top.get(2).sabor());
        assertEquals(3, top.get(2).quantidade());
        assertEquals("Morango", top.get(3).sabor());
        // Baunilha (1, só no pedido não cancelado) e Limão (1) empatam: desempata por sabor -> Baunilha entra, Limão fica de fora
        assertEquals("Baunilha", top.get(4).sabor());
        assertEquals(1, top.get(4).quantidade());
    }

    @Test
    void clientesComMaisFacturasOrdenadosPorNumeroEDepoisPorValor() {
        Cliente ana = cliente(1, "Ana");
        Cliente bruno = cliente(2, "Bruno");
        Cliente carla = cliente(3, "Carla");
        List<Factura> finais = new ArrayList<>();
        finais.add(finalDe(pedido(ana, StatusPedido.REVISADO), "100.00"));
        finais.add(finalDe(pedido(ana, StatusPedido.REVISADO), "100.00"));
        finais.add(finalDe(pedido(bruno, StatusPedido.REVISADO), "50.00"));
        finais.add(finalDe(pedido(carla, StatusPedido.ENVIADO), "400.00"));
        finais.add(finalDe(pedido(bruno, StatusPedido.REVISADO), "50.00"));
        finais.add(finalDe(pedido(carla, StatusPedido.ENVIADO), "10.00"));
        when(pedidoRepository.findAll()).thenReturn(List.of());
        when(facturaRepository.findAll()).thenReturn(finais);

        List<DashboardResposta.ClienteComFacturas> top = service.resumo().clientesComMaisFacturas();

        assertEquals(3, top.size());
        // todos têm 2 faturas: desempata por valor total (Carla 410 > Ana 200 > Bruno 100)
        assertEquals("Carla", top.get(0).nome());
        assertEquals(2, top.get(0).facturas());
        assertEquals(0, new BigDecimal("410.00").compareTo(top.get(0).valorTotal()));
        assertEquals("Ana", top.get(1).nome());
        assertEquals("Bruno", top.get(2).nome());
    }
}
