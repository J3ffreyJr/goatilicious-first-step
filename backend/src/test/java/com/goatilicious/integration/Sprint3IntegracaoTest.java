package com.goatilicious.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goatilicious.mail.EmailFalhouException;
import com.goatilicious.mail.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprint 3 de ponta a ponta: fatura final -> PDF -> envio por e-mail -> histórico (SMTP simulado). */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint3IntegracaoTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EmailSender emailSender;

    private JsonNode lerJson(String url) throws Exception {
        String corpo = mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo);
    }

    private long criarPedido(String email, long produtoId) throws Exception {
        String cliente = "{\"nome\":\"Cliente Sprint3\",\"email\":\"" + email + "\"}";
        String respostaCliente = mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long clienteId = mapper.readTree(respostaCliente).get("idCliente").asLong();

        String pedido = "{\"clienteId\":" + clienteId + ",\"origem\":\"ONLINE\","
                + "\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":1}]}";
        String respostaPedido = mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(respostaPedido).get("idPedido").asLong();
    }

    private void rever(long idPedido, long produtoId) throws Exception {
        String revisao = "{\"condicaoPagamento\":\"CARTAO\",\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":2}]}";
        mockMvc.perform(put("/api/pedidos/" + idPedido + "/revisao").contentType(MediaType.APPLICATION_JSON).content(revisao))
                .andExpect(status().isOk());
    }

    private long idDaFactura(long idPedido) throws Exception {
        return lerJson("/api/pedidos/" + idPedido + "/factura").get("idFactura").asLong();
    }

    private long primeiroProduto() throws Exception {
        return lerJson("/api/produtos").get(0).get("idProduto").asLong();
    }

    @Test
    void facturaFinalTemPdfParaImprimir() throws Exception {
        long produtoId = primeiroProduto();
        long idPedido = criarPedido("s3.pdf@mail.com", produtoId);
        rever(idPedido, produtoId);

        byte[] pdf = mockMvc.perform(get("/api/facturas/" + idDaFactura(idPedido) + "/pdf"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void enviarFacturaFinalMarcaPedidoEnviadoEGravaHistorico() throws Exception {
        long produtoId = primeiroProduto();
        long idPedido = criarPedido("s3.envio@mail.com", produtoId);
        rever(idPedido, produtoId);
        long idFactura = idDaFactura(idPedido);

        mockMvc.perform(post("/api/facturas/" + idFactura + "/envio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.destinatario").value("s3.envio@mail.com"));

        verify(emailSender).enviar(eq("s3.envio@mail.com"), anyString(), anyString(),
                eq("factura-" + idFactura + ".pdf"), any(byte[].class));
        mockMvc.perform(get("/api/pedidos/" + idPedido)).andExpect(jsonPath("$.status").value("ENVIADO"));
        assertEquals(1, lerJson("/api/facturas/" + idFactura + "/envios").size());
    }

    @Test
    void rascunhoNaoSeImprimeNemSeEnvia() throws Exception {
        long idPedido = criarPedido("s3.rascunho@mail.com", primeiroProduto());
        long idFactura = idDaFactura(idPedido);

        mockMvc.perform(get("/api/facturas/" + idFactura + "/pdf")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/facturas/" + idFactura + "/envio")).andExpect(status().isConflict());

        verify(emailSender, never()).enviar(eq("s3.rascunho@mail.com"), anyString(), anyString(), anyString(), any(byte[].class));
        assertEquals(0, lerJson("/api/facturas/" + idFactura + "/envios").size());
    }

    @Test
    void falhaDoSmtpDevolve502GravaOErroENaoMudaOPedido() throws Exception {
        long produtoId = primeiroProduto();
        long idPedido = criarPedido("s3.falha@mail.com", produtoId);
        rever(idPedido, produtoId);
        long idFactura = idDaFactura(idPedido);
        doThrow(new EmailFalhouException("SMTP em baixo", new RuntimeException())).when(emailSender)
                .enviar(eq("s3.falha@mail.com"), anyString(), anyString(), anyString(), any(byte[].class));

        mockMvc.perform(post("/api/facturas/" + idFactura + "/envio")).andExpect(status().isBadGateway());

        JsonNode envios = lerJson("/api/facturas/" + idFactura + "/envios");
        assertEquals(1, envios.size());
        assertEquals(false, envios.get(0).get("sucesso").asBoolean());
        assertEquals("SMTP em baixo", envios.get(0).get("erro").asText());
        mockMvc.perform(get("/api/pedidos/" + idPedido)).andExpect(jsonPath("$.status").value("REVISADO"));
    }
}
