package com.goatilicious.services;

import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.dtos.RevisaoRequest;
import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import com.goatilicious.repositories.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private FacturaService service;

    private Produto cereja;
    private Produto mirtilo;
    private Pedido pedido;
    private Factura factura;

    @BeforeEach
    void preparar() {
        cereja = produto(10L, "Cereja", "350.00");
        mirtilo = produto(11L, "Mirtilo Silvestre", "650.00");

        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNome("Ana");

        pedido = new Pedido();
        pedido.setIdPedido(5L);
        pedido.setCliente(cliente);
        pedido.setOrigem(OrigemPedido.ONLINE);
        pedido.adicionarItem(item(cereja, 2, "350.00"));

        factura = Factura.rascunhoDe(pedido);
        factura.setIdFactura(7L);
    }

    private Produto produto(Long id, String sabor, String preco) {
        Produto p = new Produto();
        p.setIdProduto(id);
        p.setSabor(sabor);
        p.setTamanho(TamanhoProduto.ML_500);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal(preco));
        return p;
    }

    private ItemPedido item(Produto produto, int quantidade, String precoAplicado) {
        ItemPedido i = new ItemPedido();
        i.setProduto(produto);
        i.setQuantidade(quantidade);
        i.setPrecoAplicado(new BigDecimal(precoAplicado));
        return i;
    }

    private void pedidoEFacturaExistem() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(facturaRepository.findByPedidoIdPedido(5L)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RevisaoRequest revisao(CondicaoPagamento condicao, ItemRequest... itens) {
        return new RevisaoRequest(condicao, List.of(itens));
    }

    // ---------- rascunho e consultas ----------

    @Test
    void gerarRascunhoGravaFacturaComTotalDoPedido() {
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        service.gerarRascunho(pedido);

        ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        Factura gravada = captor.getValue();
        assertSame(pedido, gravada.getPedido());
        assertEquals(EstadoFactura.RASCUNHO, gravada.getEstado());
        assertEquals(CondicaoPagamento.A_VISTA, gravada.getCondicaoPagamento());
        assertEquals(0, new BigDecimal("700.00").compareTo(gravada.getValorTotal()));
    }

    @Test
    void listarDevolveTodasAsFacturas() {
        when(facturaRepository.findAll()).thenReturn(List.of(factura));

        assertEquals(1, service.listar().size());
    }

    @Test
    void buscarPorIdDevolveFactura() {
        when(facturaRepository.findById(7L)).thenReturn(Optional.of(factura));

        assertSame(factura, service.buscarPorId(7L));
    }

    @Test
    void buscarPorIdInexistenteLanca404() {
        when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.buscarPorId(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void buscarPorPedidoInexistenteLanca404() {
        when(facturaRepository.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.buscarPorPedido(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---------- revisão ----------

    @Test
    void revisarAlteraQuantidadeERecalculaTotal() {
        pedidoEFacturaExistem();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(cereja));

        Factura resultado = service.revisar(5L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(10L, 3)));

        assertEquals(EstadoFactura.FINAL, resultado.getEstado());
        assertEquals(0, new BigDecimal("1050.00").compareTo(resultado.getValorTotal()));
        assertEquals(1, pedido.getItens().size());
        assertEquals(3, pedido.getItens().get(0).getQuantidade());
    }

    @Test
    void revisarRemoveItem() {
        pedido.adicionarItem(item(mirtilo, 1, "650.00"));
        pedidoEFacturaExistem();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(cereja));

        Factura resultado = service.revisar(5L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(10L, 2)));

        assertEquals(1, pedido.getItens().size());
        assertEquals(0, new BigDecimal("700.00").compareTo(resultado.getValorTotal()));
    }

    @Test
    void revisarAdicionaProdutoNovoComPrecoAtual() {
        pedidoEFacturaExistem();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(cereja));
        when(produtoRepository.findById(11L)).thenReturn(Optional.of(mirtilo));

        Factura resultado = service.revisar(5L, revisao(CondicaoPagamento.A_VISTA,
                new ItemRequest(10L, 2), new ItemRequest(11L, 1)));

        assertEquals(2, pedido.getItens().size());
        assertEquals(0, new BigDecimal("1350.00").compareTo(resultado.getValorTotal()));
    }

    @Test
    void revisarMantemPrecoCongeladoDeProdutoExistente() {
        cereja.setPrecoUnitario(new BigDecimal("400.00"));   // catálogo mudou depois do pedido
        pedidoEFacturaExistem();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(cereja));

        Factura resultado = service.revisar(5L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(10L, 2)));

        assertEquals(0, new BigDecimal("350.00").compareTo(pedido.getItens().get(0).getPrecoAplicado()));
        assertEquals(0, new BigDecimal("700.00").compareTo(resultado.getValorTotal()));
    }

    @Test
    void revisarAtualizaCondicaoDePagamento() {
        pedidoEFacturaExistem();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(cereja));

        Factura resultado = service.revisar(5L, revisao(CondicaoPagamento.CARTAO, new ItemRequest(10L, 2)));

        assertEquals(CondicaoPagamento.CARTAO, resultado.getCondicaoPagamento());
    }

    @Test
    void revisarMarcaPedidoComoRevisado() {
        pedidoEFacturaExistem();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(cereja));

        service.revisar(5L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(10L, 2)));

        assertEquals(StatusPedido.REVISADO, pedido.getStatus());
    }

    @Test
    void revisarPedidoJaRevistoLanca409() {
        pedido.setStatus(StatusPedido.REVISADO);
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.revisar(5L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(10L, 2))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void revisarPedidoInexistenteLanca404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.revisar(99L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(10L, 2))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void revisarProdutoInexistenteLanca404SemAlterarOPedido() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(facturaRepository.findByPedidoIdPedido(5L)).thenReturn(Optional.of(factura));
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.revisar(5L, revisao(CondicaoPagamento.A_VISTA, new ItemRequest(99L, 1))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        assertEquals(2, pedido.getItens().get(0).getQuantidade());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void revisarComProdutoRepetidoLanca400() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(facturaRepository.findByPedidoIdPedido(5L)).thenReturn(Optional.of(factura));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.revisar(5L, revisao(CondicaoPagamento.A_VISTA,
                        new ItemRequest(10L, 1), new ItemRequest(10L, 2))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        verify(facturaRepository, never()).save(any());
    }
}
