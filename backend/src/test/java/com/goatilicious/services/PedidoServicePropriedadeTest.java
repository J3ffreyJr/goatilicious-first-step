package com.goatilicious.services;

import com.goatilicious.dtos.PedidoRequest;
import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.ClienteRepository;
import com.goatilicious.repositories.PedidoRepository;
import com.goatilicious.repositories.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de propriedade: o cliente só mexe nos seus pedidos. */
@ExtendWith(MockitoExtension.class)
class PedidoServicePropriedadeTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private FacturaService facturaService;

    @InjectMocks
    private PedidoService service;

    private Pedido pedidoDe(Long idCliente) {
        Cliente c = new Cliente();
        c.setIdCliente(idCliente);
        Pedido p = new Pedido();
        p.setIdPedido(50L);
        p.setCliente(c);
        return p;
    }

    @Test
    void pedidoDoClienteUsaOClienteDaSessaoEOrigemOnlineIgnorandoOCorpo() {
        Cliente ana = new Cliente();
        ana.setIdCliente(1L);
        Produto produto = new Produto();
        produto.setIdProduto(10L);
        produto.setSabor("Cereja");
        produto.setTamanho(TamanhoProduto.ML_500);
        produto.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        produto.setPrecoUnitario(new BigDecimal("350.00"));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        // o corpo tenta passar-se por outro cliente (99) e por pedido de telefone
        PedidoRequest corpo = new PedidoRequest(99L, OrigemPedido.TELEFONE, List.of(new ItemRequest(10L, 2)));
        service.registrarComoCliente(1L, corpo);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCliente().getIdCliente());
        assertEquals(OrigemPedido.ONLINE, captor.getValue().getOrigem());
    }

    @Test
    void listarDoClienteSoDevolveOsDele() {
        List<Pedido> dele = List.of(pedidoDe(1L));
        when(pedidoRepository.findByClienteIdCliente(1L)).thenReturn(dele);

        assertSame(dele, service.listarDoCliente(1L));
    }

    @Test
    void buscarDoClienteDevolveOPedidoSeForDele() {
        Pedido pedido = pedidoDe(1L);
        when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pedido));

        assertSame(pedido, service.buscarDoCliente(50L, 1L));
    }

    @Test
    void buscarDoClienteDePedidoDeOutroDa404() {
        when(pedidoRepository.findById(50L)).thenReturn(Optional.of(pedidoDe(2L)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.buscarDoCliente(50L, 1L));
        assertEquals(404, e.getStatusCode().value());
    }
}
