package com.goatilicious.controllers;

import com.goatilicious.config.SecurityConfig;
import com.goatilicious.dtos.PedidoRequest;
import com.goatilicious.enums.Papel;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.Pedido;
import com.goatilicious.security.ContaAutenticada;
import com.goatilicious.services.EnvioFacturaService;
import com.goatilicious.services.FacturaPdfService;
import com.goatilicious.services.FacturaService;
import com.goatilicious.services.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O cliente só vê e cria o que é seu; o staff vê tudo. */
@WebMvcTest({PedidoController.class, FacturaController.class})
@Import(SecurityConfig.class)
class PropriedadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;
    @MockBean
    private FacturaService facturaService;
    @MockBean
    private FacturaPdfService pdfService;
    @MockBean
    private EnvioFacturaService envioService;

    private static RequestPostProcessor staff() {
        return user(new ContaAutenticada(1L, "Maria", "maria@goat.co.mz", "x", Papel.STAFF, null));
    }

    private static RequestPostProcessor cliente() {
        return user(new ContaAutenticada(2L, "Ana", "ana@mail.com", "x", Papel.CLIENTE, 7L));
    }

    private static RequestPostProcessor clienteSemCliente() {
        return user(new ContaAutenticada(3L, "Sem", "sem@mail.com", "x", Papel.CLIENTE, null));
    }

    private Pedido pedido() {
        Cliente c = new Cliente();
        c.setIdCliente(7L);
        c.setNome("Ana");
        c.setEmail("ana@mail.com");
        Pedido p = new Pedido();
        p.setIdPedido(5L);
        p.setCliente(c);
        return p;
    }

    private static final String CORPO_PEDIDO =
            "{\"clienteId\":99,\"origem\":\"TELEFONE\",\"itens\":[{\"produtoId\":10,\"quantidade\":2}]}";

    // --- criar pedido ---

    @Test
    void clientePedeSempreParaSiIgnorandoOClienteIdDoCorpo() throws Exception {
        when(pedidoService.registrarComoCliente(any(Long.class), any(PedidoRequest.class))).thenReturn(pedido());

        mockMvc.perform(post("/api/pedidos").with(cliente())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_PEDIDO))
                .andExpect(status().isCreated());

        verify(pedidoService).registrarComoCliente(org.mockito.ArgumentMatchers.eq(7L), any(PedidoRequest.class));
        verify(pedidoService, never()).registrar(any());
    }

    @Test
    void staffPedeEmNomeDeQualquerCliente() throws Exception {
        when(pedidoService.registrar(any(PedidoRequest.class))).thenReturn(pedido());

        mockMvc.perform(post("/api/pedidos").with(staff())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_PEDIDO))
                .andExpect(status().isCreated());

        verify(pedidoService, never()).registrarComoCliente(any(), any());
    }

    @Test
    void contaClienteSemClienteAssociadoNaoPodePedir403() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(clienteSemCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_PEDIDO))
                .andExpect(status().isForbidden());
    }

    // --- ler pedidos ---

    @Test
    void clienteListaSoOsSeusPedidos() throws Exception {
        when(pedidoService.listarDoCliente(7L)).thenReturn(List.of(pedido()));

        mockMvc.perform(get("/api/pedidos").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(pedidoService, never()).listar();
    }

    @Test
    void staffListaTodosOsPedidos() throws Exception {
        when(pedidoService.listar()).thenReturn(List.of(pedido(), pedido()));

        mockMvc.perform(get("/api/pedidos").with(staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(pedidoService, never()).listarDoCliente(any());
    }

    @Test
    void clienteVeOSeuPedido() throws Exception {
        when(pedidoService.buscarDoCliente(5L, 7L)).thenReturn(pedido());

        mockMvc.perform(get("/api/pedidos/5").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(5));
    }

    @Test
    void pedidoDeOutroClienteDa404() throws Exception {
        when(pedidoService.buscarDoCliente(5L, 7L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        mockMvc.perform(get("/api/pedidos/5").with(cliente()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffVeQualquerPedido() throws Exception {
        when(pedidoService.buscarPorId(5L)).thenReturn(pedido());

        mockMvc.perform(get("/api/pedidos/5").with(staff()))
                .andExpect(status().isOk());

        verify(pedidoService, never()).buscarDoCliente(any(), any());
    }

    // --- facturas ---

    private Factura factura() {
        return Factura.rascunhoDe(pedido());
    }

    @Test
    void clienteVeAFacturaDoSeuPedido() throws Exception {
        when(facturaService.buscarPorPedidoDoCliente(5L, 7L)).thenReturn(factura());

        mockMvc.perform(get("/api/pedidos/5/factura").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(5));
    }

    @Test
    void facturaDeOutroClienteDa404() throws Exception {
        when(facturaService.buscarDoCliente(9L, 7L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura não encontrada"));

        mockMvc.perform(get("/api/facturas/9").with(cliente())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/facturas/9/pdf").with(cliente())).andExpect(status().isNotFound());
    }

    @Test
    void clienteAbreOPdfDaSuaFactura() throws Exception {
        Factura f = factura();
        when(facturaService.buscarDoCliente(9L, 7L)).thenReturn(f);
        when(pdfService.gerar(f)).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/facturas/9/pdf").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(facturaService, never()).buscarPorId(any());
    }

    @Test
    void staffAbreOPdfDeQualquerFactura() throws Exception {
        Factura f = factura();
        when(facturaService.buscarPorId(9L)).thenReturn(f);
        when(pdfService.gerar(f)).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/facturas/9/pdf").with(staff())).andExpect(status().isOk());

        verify(facturaService, never()).buscarDoCliente(any(), any());
    }
}
