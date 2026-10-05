package com.goatilicious.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goatilicious.dtos.PedidoRequest;
import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.services.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PedidoService service;

    private Pedido pedidoExemplo(Long id) {
        Cliente c = new Cliente();
        c.setIdCliente(1L);
        c.setNome("Ana");
        c.setEmail("ana@mail.com");

        Produto p = new Produto();
        p.setIdProduto(10L);
        p.setSabor("Cereja");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal("350.00"));

        ItemPedido item = new ItemPedido();
        item.setIdItem(100L);
        item.setProduto(p);
        item.setQuantidade(2);
        item.setPrecoAplicado(new BigDecimal("350.00"));

        Pedido pedido = new Pedido();
        pedido.setIdPedido(id);
        pedido.setCliente(c);
        pedido.setOrigem(OrigemPedido.ONLINE);
        pedido.setStatus(StatusPedido.PENDENTE);
        pedido.setDataPedido(LocalDateTime.of(2026, 10, 4, 18, 0));
        pedido.adicionarItem(item);
        return pedido;
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private PedidoRequest requestValido() {
        return new PedidoRequest(1L, OrigemPedido.ONLINE, List.of(new ItemRequest(10L, 2)));
    }

    @Test
    void postDeveCriarPedidoERetornar201() throws Exception {
        when(service.registrar(any(PedidoRequest.class))).thenReturn(pedidoExemplo(7L));

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPedido").value(7))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.origem").value("ONLINE"))
                .andExpect(jsonPath("$.cliente.nome").value("Ana"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].produto.sabor").value("Cereja"))
                .andExpect(jsonPath("$.total").value(700.0));
    }

    @Test
    void postSemItensDeveRetornar400() throws Exception {
        PedidoRequest semItens = new PedidoRequest(1L, OrigemPedido.ONLINE, List.of());

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json(semItens)))
                .andExpect(status().isBadRequest());

        verify(service, never()).registrar(any());
    }

    @Test
    void postComQuantidadeZeroDeveRetornar400() throws Exception {
        PedidoRequest quantidadeZero = new PedidoRequest(1L, OrigemPedido.ONLINE, List.of(new ItemRequest(10L, 0)));

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json(quantidadeZero)))
                .andExpect(status().isBadRequest());

        verify(service, never()).registrar(any());
    }

    @Test
    void postSemClienteDeveRetornar400() throws Exception {
        PedidoRequest semCliente = new PedidoRequest(null, OrigemPedido.ONLINE, List.of(new ItemRequest(10L, 1)));

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json(semCliente)))
                .andExpect(status().isBadRequest());

        verify(service, never()).registrar(any());
    }

    @Test
    void postSemOrigemDeveRetornar400() throws Exception {
        PedidoRequest semOrigem = new PedidoRequest(1L, null, List.of(new ItemRequest(10L, 1)));

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json(semOrigem)))
                .andExpect(status().isBadRequest());

        verify(service, never()).registrar(any());
    }

    @Test
    void postComOrigemDesconhecidaDeveRetornar400() throws Exception {
        String corpo = "{\"clienteId\":1,\"origem\":\"CARRIER_PIGEON\",\"itens\":[{\"produtoId\":10,\"quantidade\":1}]}";

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());

        verify(service, never()).registrar(any());
    }

    @Test
    void postComClienteInexistenteDeveRetornar404() throws Exception {
        when(service.registrar(any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json(requestValido())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDeveListarPedidos() throws Exception {
        when(service.listar()).thenReturn(List.of(pedidoExemplo(1L), pedidoExemplo(2L)));

        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].idPedido").value(2));
    }

    @Test
    void getPorIdDeveRetornarOPedido() throws Exception {
        when(service.buscarPorId(7L)).thenReturn(pedidoExemplo(7L));

        mockMvc.perform(get("/api/pedidos/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(7));
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        mockMvc.perform(get("/api/pedidos/99"))
                .andExpect(status().isNotFound());
    }
}
