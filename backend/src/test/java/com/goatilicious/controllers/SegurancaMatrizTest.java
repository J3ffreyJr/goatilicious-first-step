package com.goatilicious.controllers;

import com.goatilicious.config.SecurityConfig;
import com.goatilicious.enums.Papel;
import com.goatilicious.repositories.ProdutoRepository;
import com.goatilicious.security.ContaAutenticada;
import com.goatilicious.services.ClienteService;
import com.goatilicious.services.EnvioFacturaService;
import com.goatilicious.services.FacturaPdfService;
import com.goatilicious.services.FacturaService;
import com.goatilicious.services.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Matriz de permissões: anónimo (401), cliente (403 no que é do staff) e staff. */
@WebMvcTest({PedidoController.class, FacturaController.class, ClienteController.class,
        ProdutoController.class, MeController.class})
@Import(SecurityConfig.class)
class SegurancaMatrizTest {

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
    @MockBean
    private ClienteService clienteService;
    @MockBean
    private ProdutoRepository produtoRepository;

    private static RequestPostProcessor staff() {
        return user(new ContaAutenticada(1L, "Maria", "maria@goat.co.mz", "x", Papel.STAFF, null));
    }

    private static RequestPostProcessor cliente() {
        return user(new ContaAutenticada(2L, "Ana", "ana@mail.com", "x", Papel.CLIENTE, 7L));
    }

    // --- anónimo ---

    @Test
    void anonimoPodeVerOCatalogo() throws Exception {
        mockMvc.perform(get("/api/produtos")).andExpect(status().isOk());
    }

    @Test
    void anonimoPodeCadastrarSe() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@mail.com\",\"senha\":\"senha-longa\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void anonimoNaoAcedeAoResto401() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pedidos/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pedidos/1/factura")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/facturas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/facturas/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/facturas/1/pdf")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/facturas/1/envio")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/facturas/1/envios")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/pedidos/1/revisao").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void respostas401NaoTemCabecalhoQueAbreOPopupDoBrowser() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void credenciaisInvalidasDao401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Basic YW5hQG1haWwuY29tOmVycmFkYQ=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaDesconhecidaExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/qualquer-coisa")).andExpect(status().isUnauthorized());
    }

    // --- cliente: tudo o que é do staff dá 403 ---

    @Test
    void clienteNaoAcedeAoQueEDoStaff403() throws Exception {
        mockMvc.perform(get("/api/clientes").with(cliente())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/facturas").with(cliente())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/facturas/1/envio").with(cliente())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/facturas/1/envios").with(cliente())).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/pedidos/1/revisao").with(cliente())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    // --- staff e cliente nas rotas partilhadas ---

    @Test
    void clienteEStaffPodemListarPedidos() throws Exception {
        mockMvc.perform(get("/api/pedidos").with(cliente())).andExpect(status().isOk());
        mockMvc.perform(get("/api/pedidos").with(staff())).andExpect(status().isOk());
    }

    @Test
    void staffPodeListarClientesEFacturas() throws Exception {
        mockMvc.perform(get("/api/clientes").with(staff())).andExpect(status().isOk());
        mockMvc.perform(get("/api/facturas").with(staff())).andExpect(status().isOk());
    }

    // --- /api/me ---

    @Test
    void meDevolveQuemEstaLogadoSemSenha() throws Exception {
        mockMvc.perform(get("/api/me").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.papel").value("CLIENTE"))
                .andExpect(jsonPath("$.clienteId").value(7))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        mockMvc.perform(get("/api/me").with(staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("STAFF"))
                .andExpect(jsonPath("$.clienteId").doesNotExist());
    }

    // --- CORS (o site no Vite fala com a API noutra porta, com cabeçalho Authorization) ---

    @Test
    void preflightComAuthorizationNaoPedeLogin() throws Exception {
        mockMvc.perform(options("/api/pedidos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
