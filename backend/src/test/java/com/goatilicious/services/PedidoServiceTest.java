package com.goatilicious.services;

import com.goatilicious.dtos.PedidoRequest;
import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.ClienteRepository;
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
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private PedidoService service;

    private Cliente cliente;
    private Produto baunilha;
    private Produto mirtilo;

    @BeforeEach
    void preparar() {
        cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNome("Ana");
        cliente.setEmail("ana@mail.com");

        baunilha = produto(10L, "Baunilha & Mel", "350.00");
        mirtilo = produto(11L, "Mirtilo Silvestre", "650.00");
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

    private PedidoRequest pedidoDeDoisItens(OrigemPedido origem) {
        return new PedidoRequest(1L, origem, List.of(
                new ItemRequest(10L, 2),
                new ItemRequest(11L, 1)));
    }

    @Test
    void registrarDeveCriarPedidoPendenteComPrecosAplicados() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(baunilha));
        when(produtoRepository.findById(11L)).thenReturn(Optional.of(mirtilo));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido pedido = service.registrar(pedidoDeDoisItens(OrigemPedido.TELEFONE));

        assertSame(cliente, pedido.getCliente());
        assertEquals(OrigemPedido.TELEFONE, pedido.getOrigem());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        assertEquals(2, pedido.getItens().size());
        assertEquals(2, pedido.getItens().get(0).getQuantidade());
        assertEquals(0, new BigDecimal("350.00").compareTo(pedido.getItens().get(0).getPrecoAplicado()));
        assertEquals(0, new BigDecimal("1350.00").compareTo(pedido.getTotal()));
        assertSame(pedido, pedido.getItens().get(0).getPedido());
    }

    @Test
    void precoAplicadoNaoMudaQuandoOPrecoDoProdutoMudaDepois() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(baunilha));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido pedido = service.registrar(new PedidoRequest(1L, OrigemPedido.ONLINE, List.of(new ItemRequest(10L, 1))));
        baunilha.setPrecoUnitario(new BigDecimal("999.00"));

        assertEquals(0, new BigDecimal("350.00").compareTo(pedido.getItens().get(0).getPrecoAplicado()));
    }

    @Test
    void registrarDeveGravarPeloRepositorio() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(baunilha));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(new PedidoRequest(1L, OrigemPedido.ONLINE, List.of(new ItemRequest(10L, 1))));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertEquals(OrigemPedido.ONLINE, captor.getValue().getOrigem());
    }

    @Test
    void clienteInexistenteDeveLancar404ENaoGravar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(pedidoDeDoisItens(OrigemPedido.ONLINE)));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void produtoInexistenteDeveLancar404ENaoGravar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(baunilha));
        when(produtoRepository.findById(11L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(pedidoDeDoisItens(OrigemPedido.ONLINE)));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void listarDeveDevolverTodosOsPedidos() {
        when(pedidoRepository.findAll()).thenReturn(List.of(new Pedido(), new Pedido()));

        assertEquals(2, service.listar().size());
    }

    @Test
    void buscarPorIdDeveDevolverOPedido() {
        Pedido pedido = new Pedido();
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        assertSame(pedido, service.buscarPorId(5L));
    }

    @Test
    void buscarPorIdInexistenteDeveLancar404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.buscarPorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }
}
