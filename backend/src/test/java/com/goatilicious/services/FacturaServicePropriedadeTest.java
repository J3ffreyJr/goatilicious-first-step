package com.goatilicious.services;

import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.Pedido;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import com.goatilicious.repositories.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Regras de propriedade: o cliente só vê as suas faturas. */
@ExtendWith(MockitoExtension.class)
class FacturaServicePropriedadeTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private FacturaService service;

    private Factura facturaDe(Long idCliente) {
        Cliente c = new Cliente();
        c.setIdCliente(idCliente);
        Pedido p = new Pedido();
        p.setIdPedido(50L);
        p.setCliente(c);
        return Factura.rascunhoDe(p);
    }

    @Test
    void buscarDoClienteDevolveASuaFactura() {
        Factura f = facturaDe(1L);
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(f));

        assertSame(f, service.buscarDoCliente(5L, 1L));
    }

    @Test
    void facturaDeOutroClienteDa404() {
        when(facturaRepository.findById(5L)).thenReturn(Optional.of(facturaDe(2L)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.buscarDoCliente(5L, 1L));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void buscarPorPedidoDoClienteDevolveASuaFactura() {
        Factura f = facturaDe(1L);
        when(facturaRepository.findByPedidoIdPedido(50L)).thenReturn(Optional.of(f));

        assertSame(f, service.buscarPorPedidoDoCliente(50L, 1L));
    }

    @Test
    void buscarPorPedidoDeOutroClienteDa404() {
        when(facturaRepository.findByPedidoIdPedido(50L)).thenReturn(Optional.of(facturaDe(2L)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.buscarPorPedidoDoCliente(50L, 1L));
        assertEquals(404, e.getStatusCode().value());
    }
}
