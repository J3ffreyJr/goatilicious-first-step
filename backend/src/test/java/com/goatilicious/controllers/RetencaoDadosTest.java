package com.goatilicious.controllers;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import com.goatilicious.config.SecurityConfig;
import com.goatilicious.services.ClienteService;
import com.goatilicious.services.EnvioFacturaService;
import com.goatilicious.services.FacturaPdfService;
import com.goatilicious.services.FacturaService;
import com.goatilicious.services.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RNF01: a API não permite apagar pedidos, faturas nem clientes (dados retidos 5 anos). */
@WebMvcTest({PedidoController.class, FacturaController.class, ClienteController.class})
@Import(SecurityConfig.class)
@WithMockUser(roles = "STAFF")
class RetencaoDadosTest {

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

    @Test
    void naoSePodeApagarUmPedido() throws Exception {
        mockMvc.perform(delete("/api/pedidos/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void naoSePodeApagarUmaFactura() throws Exception {
        mockMvc.perform(delete("/api/facturas/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void naoSePodeApagarUmCliente() throws Exception {
        mockMvc.perform(delete("/api/clientes/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void naoSePodeApagarOHistoricoDeEnvios() throws Exception {
        mockMvc.perform(delete("/api/facturas/1/envios")).andExpect(status().is4xxClientError());
    }
}
