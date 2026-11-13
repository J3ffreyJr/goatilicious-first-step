package com.goatilicious.controllers;

import com.goatilicious.config.SecurityConfig;
import com.goatilicious.dtos.DashboardResposta;
import com.goatilicious.dtos.DashboardResposta.ClienteComFacturas;
import com.goatilicious.dtos.DashboardResposta.PedidosPorEstado;
import com.goatilicious.dtos.DashboardResposta.ProdutoMaisPedido;
import com.goatilicious.dtos.DashboardResposta.Receita;
import com.goatilicious.enums.Papel;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.security.ContaAutenticada;
import com.goatilicious.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService service;

    private ContaAutenticada staff() {
        return new ContaAutenticada(1L, "Maria", "maria@goat.co.mz", "x", Papel.STAFF, null);
    }

    private ContaAutenticada cliente() {
        return new ContaAutenticada(2L, "Ana", "ana@mail.com", "x", Papel.CLIENTE, 7L);
    }

    @Test
    void staffRecebeOResumo() throws Exception {
        when(service.resumo()).thenReturn(new DashboardResposta(
                new Receita(new BigDecimal("500.50"), new BigDecimal("200.50"), new BigDecimal("300.00"), 2),
                new PedidosPorEstado(5, 2, 2, 1),
                List.of(new ProdutoMaisPedido(1L, "Cereja", TamanhoProduto.ML_500, 6)),
                List.of(new ClienteComFacturas(1L, "Ana", 2, new BigDecimal("200.00")))));

        mockMvc.perform(get("/api/dashboard").with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receita.total").value(500.50))
                .andExpect(jsonPath("$.receita.facturasFinais").value(2))
                .andExpect(jsonPath("$.pedidos.feitos").value(2))
                .andExpect(jsonPath("$.pedidos.pendentes").value(2))
                .andExpect(jsonPath("$.pedidos.cancelados").value(1))
                .andExpect(jsonPath("$.produtosMaisPedidos[0].sabor").value("Cereja"))
                .andExpect(jsonPath("$.produtosMaisPedidos[0].quantidade").value(6))
                .andExpect(jsonPath("$.clientesComMaisFacturas[0].nome").value("Ana"));
    }

    @Test
    void clienteNaoVeODashboard403() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user(cliente()))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void anonimoNaoVeODashboard401() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }
}
