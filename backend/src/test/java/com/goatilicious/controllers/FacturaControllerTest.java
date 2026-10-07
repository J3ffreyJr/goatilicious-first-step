package com.goatilicious.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.dtos.RevisaoRequest;
import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.services.FacturaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FacturaController.class)
class FacturaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FacturaService service;

    private Factura facturaExemplo(Long id) {
        Cliente c = new Cliente();
        c.setIdCliente(1L);
        c.setNome("Ana");

        Produto p = new Produto();
        p.setIdProduto(10L);
        p.setSabor("Cereja");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal("350.00"));

        ItemPedido item = new ItemPedido();
        item.setProduto(p);
        item.setQuantidade(2);
        item.setPrecoAplicado(new BigDecimal("350.00"));

        Pedido pedido = new Pedido();
        pedido.setIdPedido(5L);
        pedido.setCliente(c);
        pedido.setOrigem(OrigemPedido.ONLINE);
        pedido.adicionarItem(item);

        Factura f = Factura.rascunhoDe(pedido);
        f.setIdFactura(id);
        return f;
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private RevisaoRequest revisaoValida() {
        return new RevisaoRequest(CondicaoPagamento.CARTAO, List.of(new ItemRequest(10L, 3)));
    }

    @Test
    void getListaFacturas() throws Exception {
        when(service.listar()).thenReturn(List.of(facturaExemplo(7L)));

        mockMvc.perform(get("/api/facturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idFactura").value(7))
                .andExpect(jsonPath("$[0].nomeCliente").value("Ana"))
                .andExpect(jsonPath("$[0].estado").value("RASCUNHO"));
    }

    @Test
    void getPorIdDevolveFactura() throws Exception {
        when(service.buscarPorId(7L)).thenReturn(facturaExemplo(7L));

        mockMvc.perform(get("/api/facturas/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFactura").value(7))
                .andExpect(jsonPath("$.idPedido").value(5))
                .andExpect(jsonPath("$.valorTotal").value(700.0))
                .andExpect(jsonPath("$.condicaoPagamento").value("A_VISTA"))
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"));
    }

    @Test
    void getPorIdInexistenteDevolve404() throws Exception {
        when(service.buscarPorId(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura não encontrada"));

        mockMvc.perform(get("/api/facturas/99")).andExpect(status().isNotFound());
    }

    @Test
    void getPorPedidoDevolveFactura() throws Exception {
        when(service.buscarPorPedido(5L)).thenReturn(facturaExemplo(7L));

        mockMvc.perform(get("/api/pedidos/5/factura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(5));
    }

    @Test
    void putRevisaoDevolve200ComFacturaFinal() throws Exception {
        Factura finalizada = facturaExemplo(7L);
        finalizada.finalizar(CondicaoPagamento.CARTAO, new BigDecimal("1050.00"));
        when(service.revisar(eq(5L), any(RevisaoRequest.class))).thenReturn(finalizada);

        mockMvc.perform(put("/api/pedidos/5/revisao").contentType(MediaType.APPLICATION_JSON).content(json(revisaoValida())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINAL"))
                .andExpect(jsonPath("$.condicaoPagamento").value("CARTAO"))
                .andExpect(jsonPath("$.valorTotal").value(1050.0));
    }

    @Test
    void putRevisaoComListaVaziaDevolve400() throws Exception {
        RevisaoRequest semItens = new RevisaoRequest(CondicaoPagamento.A_VISTA, List.of());

        mockMvc.perform(put("/api/pedidos/5/revisao").contentType(MediaType.APPLICATION_JSON).content(json(semItens)))
                .andExpect(status().isBadRequest());

        verify(service, never()).revisar(any(), any());
    }

    @Test
    void putRevisaoSemCondicaoDevolve400() throws Exception {
        String corpo = "{\"itens\":[{\"produtoId\":10,\"quantidade\":1}]}";

        mockMvc.perform(put("/api/pedidos/5/revisao").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());

        verify(service, never()).revisar(any(), any());
    }

    @Test
    void putRevisaoDePedidoJaRevistoDevolve409() throws Exception {
        when(service.revisar(eq(5L), any(RevisaoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "O pedido já foi revisto"));

        mockMvc.perform(put("/api/pedidos/5/revisao").contentType(MediaType.APPLICATION_JSON).content(json(revisaoValida())))
                .andExpect(status().isConflict());
    }
}
