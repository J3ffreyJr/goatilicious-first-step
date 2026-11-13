package com.goatilicious.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goatilicious.repositories.UtilizadorRepository;
import com.goatilicious.services.UtilizadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Separação Cliente/Staff com login real (HTTP Basic), contas gravadas com BCrypt na base. */
@SpringBootTest
@AutoConfigureMockMvc
class SeparacaoClienteStaffIntegracaoTest {

    private static final String STAFF_EMAIL = "staff.sep@goatilicious.test";
    private static final String STAFF_SENHA = "staff-senha-forte";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UtilizadorService utilizadorService;
    @Autowired
    private UtilizadorRepository utilizadorRepository;

    @BeforeEach
    void staffSemente() {
        utilizadorService.criarStaff("Maria Staff", STAFF_EMAIL, STAFF_SENHA);
    }

    private long cadastrar(String nome, String email, String senha) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
        String resposta = mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("idCliente").asLong();
    }

    private long primeiroProduto() throws Exception {
        String resposta = mockMvc.perform(get("/api/produtos")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get(0).get("idProduto").asLong();
    }

    private long pedir(String email, String senha, long clienteIdNoCorpo, String origem) throws Exception {
        String pedido = "{\"clienteId\":" + clienteIdNoCorpo + ",\"origem\":\"" + origem + "\","
                + "\"itens\":[{\"produtoId\":" + primeiroProduto() + ",\"quantidade\":1}]}";
        String resposta = mockMvc.perform(post("/api/pedidos").with(httpBasic(email, senha))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("idPedido").asLong();
    }

    @Test
    void cadastroGravaSenhaComHashELoginFunciona() throws Exception {
        cadastrar("Ana Sep", "ana.sep@mail.com", "senha-da-ana");

        assertFalse(utilizadorRepository.findByEmail("ana.sep@mail.com").orElseThrow()
                .getSenhaHash().contains("senha-da-ana"));

        mockMvc.perform(get("/api/me").with(httpBasic("ana.sep@mail.com", "senha-da-ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("CLIENTE"))
                .andExpect(jsonPath("$.nome").value("Ana Sep"));
    }

    @Test
    void loginEmMaiusculasEFunciona() throws Exception {
        cadastrar("Caio Sep", "caio.sep@mail.com", "senha-do-caio");

        mockMvc.perform(get("/api/me").with(httpBasic("CAIO.SEP@mail.com", "senha-do-caio")))
                .andExpect(status().isOk());
    }

    @Test
    void senhaErradaDa401() throws Exception {
        cadastrar("Duda Sep", "duda.sep@mail.com", "senha-da-duda");

        mockMvc.perform(get("/api/me").with(httpBasic("duda.sep@mail.com", "errada-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroSemSenhaOuComSenhaCurtaDa400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem\",\"email\":\"sem.senha@mail.com\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Curta\",\"email\":\"curta@mail.com\",\"senha\":\"1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clienteSoVeOsSeusPedidosEOStaffVeTodos() throws Exception {
        long idAna = cadastrar("Ana Dona", "ana.dona@mail.com", "senha-da-ana");
        long idBruno = cadastrar("Bruno Dono", "bruno.dono@mail.com", "senha-do-bruno");

        long pedidoAna = pedir("ana.dona@mail.com", "senha-da-ana", idAna, "ONLINE");
        long pedidoBruno = pedir("bruno.dono@mail.com", "senha-do-bruno", idBruno, "ONLINE");

        // a Ana só vê o dela
        String lista = mockMvc.perform(get("/api/pedidos").with(httpBasic("ana.dona@mail.com", "senha-da-ana")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode pedidos = mapper.readTree(lista);
        assertEquals(1, pedidos.size());
        assertEquals(pedidoAna, pedidos.get(0).get("idPedido").asLong());

        // o pedido e a fatura do Bruno aparecem como inexistentes para a Ana
        mockMvc.perform(get("/api/pedidos/" + pedidoBruno).with(httpBasic("ana.dona@mail.com", "senha-da-ana")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pedidos/" + pedidoBruno + "/factura")
                        .with(httpBasic("ana.dona@mail.com", "senha-da-ana")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pedidos/" + pedidoAna + "/factura")
                        .with(httpBasic("ana.dona@mail.com", "senha-da-ana")))
                .andExpect(status().isOk());

        // o staff vê os dois
        String todos = mockMvc.perform(get("/api/pedidos").with(httpBasic(STAFF_EMAIL, STAFF_SENHA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(mapper.readTree(todos).size() >= 2);
    }

    @Test
    void clienteNaoSePassaPorOutroNemFazPedidoDeTelefone() throws Exception {
        long idAna = cadastrar("Ana Falsa", "ana.falsa@mail.com", "senha-da-ana");
        long idBruno = cadastrar("Bruno Falso", "bruno.falso@mail.com", "senha-do-bruno");

        long pedido = pedir("ana.falsa@mail.com", "senha-da-ana", idBruno, "TELEFONE");

        String resposta = mockMvc.perform(get("/api/pedidos/" + pedido).with(httpBasic(STAFF_EMAIL, STAFF_SENHA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(resposta);
        assertEquals(idAna, json.get("cliente").get("idCliente").asLong());
        assertEquals("ONLINE", json.get("origem").asText());
    }

    @Test
    void clienteNaoPodeOperarComoStaff() throws Exception {
        cadastrar("Eva Sep", "eva.sep@mail.com", "senha-da-eva");
        String eva = "eva.sep@mail.com";

        mockMvc.perform(get("/api/clientes").with(httpBasic(eva, "senha-da-eva"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/facturas").with(httpBasic(eva, "senha-da-eva"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/facturas/1/envio").with(httpBasic(eva, "senha-da-eva")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/pedidos/1/revisao").with(httpBasic(eva, "senha-da-eva"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffAcedeAoPainelEFazLoginComoStaff() throws Exception {
        mockMvc.perform(get("/api/me").with(httpBasic(STAFF_EMAIL, STAFF_SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("STAFF"));
        mockMvc.perform(get("/api/clientes").with(httpBasic(STAFF_EMAIL, STAFF_SENHA))).andExpect(status().isOk());
        mockMvc.perform(get("/api/facturas").with(httpBasic(STAFF_EMAIL, STAFF_SENHA))).andExpect(status().isOk());
    }

    @Test
    void staffRegistaClienteSemSenhaEEsseClienteNaoTemLogin() throws Exception {
        mockMvc.perform(post("/api/clientes").with(httpBasic(STAFF_EMAIL, STAFF_SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Telefone\",\"email\":\"telefone.sep@mail.com\"}"))
                .andExpect(status().isCreated());

        assertFalse(utilizadorRepository.existsByEmail("telefone.sep@mail.com"));
    }

    @Test
    void emailJaCadastradoDa409() throws Exception {
        cadastrar("Fábio Sep", "fabio.sep@mail.com", "senha-do-fabio");

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro\",\"email\":\"fabio.sep@mail.com\",\"senha\":\"outra-senha-ok\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void dashboardSoParaStaff() throws Exception {
        cadastrar("Gil Sep", "gil.sep@mail.com", "senha-do-gil");

        mockMvc.perform(get("/api/dashboard").with(httpBasic(STAFF_EMAIL, STAFF_SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receita.total").exists())
                .andExpect(jsonPath("$.pedidos.total").exists())
                .andExpect(jsonPath("$.produtosMaisPedidos").isArray())
                .andExpect(jsonPath("$.clientesComMaisFacturas").isArray());
        mockMvc.perform(get("/api/dashboard").with(httpBasic("gil.sep@mail.com", "senha-do-gil")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
}
