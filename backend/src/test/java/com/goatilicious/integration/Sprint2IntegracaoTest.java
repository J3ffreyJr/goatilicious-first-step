package com.goatilicious.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Percorre o Sprint 2 de ponta a ponta: pedido -> fatura rascunho -> revisão -> fatura final. */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint2IntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    private JsonNode lerJson(String url) throws Exception {
        String corpo = mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo);
    }

    private long criarCliente(String email) throws Exception {
        String json = "{\"nome\":\"Cliente Sprint2\",\"email\":\"" + email + "\"}";
        String corpo = mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo).get("idCliente").asLong();
    }

    private long criarPedido(String email, String origem, long produtoId, int quantidade) throws Exception {
        long clienteId = criarCliente(email);
        String pedido = "{\"clienteId\":" + clienteId + ",\"origem\":\"" + origem + "\","
                + "\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":" + quantidade + "}]}";
        String corpo = mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo).get("idPedido").asLong();
    }

    private String revisao(String condicao, String itensJson) {
        return "{\"condicaoPagamento\":\"" + condicao + "\",\"itens\":" + itensJson + "}";
    }

    @Test
    void novoPedidoGeraFacturaRascunho() throws Exception {
        JsonNode produto = lerJson("/api/produtos").get(0);
        double preco = produto.get("precoUnitario").asDouble();
        long idPedido = criarPedido("s2.rascunho@mail.com", "ONLINE", produto.get("idProduto").asLong(), 2);

        mockMvc.perform(get("/api/pedidos/" + idPedido + "/factura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RASCUNHO"))
                .andExpect(jsonPath("$.condicaoPagamento").value("A_VISTA"))
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
                .andExpect(jsonPath("$.nomeCliente").value("Cliente Sprint2"))
                .andExpect(jsonPath("$.valorTotal").value(preco * 2));
    }

    @Test
    void revisaoGeraFacturaFinalComNovoTotalEPedidoRevisado() throws Exception {
        JsonNode produto = lerJson("/api/produtos").get(0);
        long produtoId = produto.get("idProduto").asLong();
        double preco = produto.get("precoUnitario").asDouble();
        long idPedido = criarPedido("s2.revisao@mail.com", "TELEFONE", produtoId, 1);

        String itens = "[{\"produtoId\":" + produtoId + ",\"quantidade\":2}]";
        mockMvc.perform(put("/api/pedidos/" + idPedido + "/revisao")
                        .contentType(MediaType.APPLICATION_JSON).content(revisao("CARTAO", itens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINAL"))
                .andExpect(jsonPath("$.condicaoPagamento").value("CARTAO"))
                .andExpect(jsonPath("$.valorTotal").value(preco * 2));

        mockMvc.perform(get("/api/pedidos/" + idPedido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVISADO"));
    }

    @Test
    void revisaoPermiteAdicionarProdutoNovo() throws Exception {
        JsonNode produtos = lerJson("/api/produtos");
        long primeiro = produtos.get(0).get("idProduto").asLong();
        long segundo = produtos.get(1).get("idProduto").asLong();
        double precoPrimeiro = produtos.get(0).get("precoUnitario").asDouble();
        double precoSegundo = produtos.get(1).get("precoUnitario").asDouble();
        long idPedido = criarPedido("s2.adicionar@mail.com", "ONLINE", primeiro, 1);

        String itens = "[{\"produtoId\":" + primeiro + ",\"quantidade\":1},"
                + "{\"produtoId\":" + segundo + ",\"quantidade\":2}]";
        mockMvc.perform(put("/api/pedidos/" + idPedido + "/revisao")
                        .contentType(MediaType.APPLICATION_JSON).content(revisao("PAGO_NA_ENTREGA", itens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(precoPrimeiro + precoSegundo * 2));

        assertEquals(2, lerJson("/api/pedidos/" + idPedido).get("itens").size());
    }

    @Test
    void segundaRevisaoDevolve409EMantemATotalAnterior() throws Exception {
        JsonNode produto = lerJson("/api/produtos").get(0);
        long produtoId = produto.get("idProduto").asLong();
        double preco = produto.get("precoUnitario").asDouble();
        long idPedido = criarPedido("s2.dupla@mail.com", "ONLINE", produtoId, 1);

        mockMvc.perform(put("/api/pedidos/" + idPedido + "/revisao").contentType(MediaType.APPLICATION_JSON)
                        .content(revisao("A_VISTA", "[{\"produtoId\":" + produtoId + ",\"quantidade\":2}]")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/pedidos/" + idPedido + "/revisao").contentType(MediaType.APPLICATION_JSON)
                        .content(revisao("CARTAO", "[{\"produtoId\":" + produtoId + ",\"quantidade\":5}]")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/pedidos/" + idPedido + "/factura"))
                .andExpect(jsonPath("$.valorTotal").value(preco * 2))
                .andExpect(jsonPath("$.condicaoPagamento").value("A_VISTA"));
    }
}
