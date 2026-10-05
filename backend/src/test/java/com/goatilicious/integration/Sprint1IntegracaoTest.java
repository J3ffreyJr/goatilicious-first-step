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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Percorre o Sprint 1 de ponta a ponta com a aplicação real (H2 + catálogo semeado). */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint1IntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    private JsonNode lerJson(String url) throws Exception {
        String corpo = mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo);
    }

    private long criarCliente(String nome, String email) throws Exception {
        String json = "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\"}";
        String corpo = mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo).get("idCliente").asLong();
    }

    @Test
    void catalogoInicialTem8Produtos() throws Exception {
        assertEquals(8, lerJson("/api/produtos").size());
    }

    @Test
    void clienteDoSiteRegistaSeIdentificaSePorEmailEFazPedidoOnline() throws Exception {
        long id = criarCliente("Ana Integracao", "ana.int@mail.com");

        mockMvc.perform(get("/api/clientes/por-email").param("email", "ana.int@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCliente").value(id));

        long produtoId = lerJson("/api/produtos").get(0).get("idProduto").asLong();
        String pedido = "{\"clienteId\":" + id + ",\"origem\":\"ONLINE\","
                + "\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":2}]}";

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.origem").value("ONLINE"))
                .andExpect(jsonPath("$.itens.length()").value(1));
    }

    @Test
    void funcionarioRegistaClienteEPedidoPorTelefoneECongelaOPreco() throws Exception {
        long id = criarCliente("Bruno Integracao", "bruno.int@mail.com");

        JsonNode produto = lerJson("/api/produtos").get(0);
        long produtoId = produto.get("idProduto").asLong();
        double preco = produto.get("precoUnitario").asDouble();

        String pedido = "{\"clienteId\":" + id + ",\"origem\":\"TELEFONE\","
                + "\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":3}]}";
        String corpo = mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode criado = mapper.readTree(corpo);
        assertEquals("TELEFONE", criado.get("origem").asText());
        assertEquals(preco, criado.get("itens").get(0).get("precoAplicado").asDouble(), 0.001);
        assertEquals(preco * 3, criado.get("total").asDouble(), 0.001);

        long idPedido = criado.get("idPedido").asLong();
        mockMvc.perform(get("/api/pedidos/" + idPedido)).andExpect(status().isOk());
        assertTrue(lerJson("/api/pedidos").size() >= 1);
    }

    @Test
    void pedidoDeClienteInexistenteDeveRetornar404() throws Exception {
        long produtoId = lerJson("/api/produtos").get(0).get("idProduto").asLong();
        String pedido = "{\"clienteId\":999999,\"origem\":\"ONLINE\","
                + "\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":1}]}";

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isNotFound());
    }

    @Test
    void pedidoSemItensDeveRetornar400() throws Exception {
        long id = criarCliente("Carla Integracao", "carla.int@mail.com");
        String pedido = "{\"clienteId\":" + id + ",\"origem\":\"ONLINE\",\"itens\":[]}";

        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isBadRequest());
    }
}
