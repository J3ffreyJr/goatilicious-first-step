package com.goatilicious.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goatilicious.config.SecurityConfig;
import com.goatilicious.dtos.ClienteRequest;
import com.goatilicious.enums.Papel;
import com.goatilicious.models.Cliente;
import com.goatilicious.security.ContaAutenticada;
import com.goatilicious.services.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteService service;

    private Cliente cliente(Long id, String nome, String email) {
        Cliente c = new Cliente();
        c.setIdCliente(id);
        c.setNome(nome);
        c.setEmail(email);
        c.setTelefone("+258 84 000 0000");
        c.setEndereco("Maputo");
        c.setDataCadastro(LocalDate.of(2026, 10, 2));
        return c;
    }

    private String corpo(String nome, String email, String senha) throws Exception {
        return objectMapper.writeValueAsString(new ClienteRequest(nome, email, "+258 84 000 0000", "Maputo", senha));
    }

    private ContaAutenticada staff() {
        return new ContaAutenticada(1L, "Maria", "maria@goat.co.mz", "x", Papel.STAFF, null);
    }

    private ContaAutenticada clienteLogado() {
        return new ContaAutenticada(2L, "Ana", "ana@mail.com", "x", Papel.CLIENTE, 7L);
    }

    // --- cadastro público ---

    @Test
    void cadastroAnonimoCriaClienteERetorna201SemSenhaNaResposta() throws Exception {
        when(service.criar(any(ClienteRequest.class), eq(false))).thenReturn(cliente(1L, "Ana", "ana@mail.com"));

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", "ana@mail.com", "senha-longa")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(1))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void cadastroFeitoPorStaffAvisaOServicoQueEStaff() throws Exception {
        when(service.criar(any(ClienteRequest.class), eq(true))).thenReturn(cliente(1L, "Ana", "ana@mail.com"));

        mockMvc.perform(post("/api/clientes").with(user(staff())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", "ana@mail.com", null)))
                .andExpect(status().isCreated());

        verify(service).criar(any(ClienteRequest.class), eq(true));
    }

    @Test
    void cadastroFeitoPorClienteLogadoNaoContaComoStaff() throws Exception {
        when(service.criar(any(ClienteRequest.class), eq(false))).thenReturn(cliente(1L, "Ana", "ana@mail.com"));

        mockMvc.perform(post("/api/clientes").with(user(clienteLogado())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", "ana@mail.com", "senha-longa")))
                .andExpect(status().isCreated());

        verify(service).criar(any(ClienteRequest.class), eq(false));
    }

    @Test
    void postComEmailInvalidoDeveRetornar400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", "email-invalido", "senha-longa")))
                .andExpect(status().isBadRequest());

        verify(service, never()).criar(any(), eq(false));
    }

    @Test
    void postSemNomeDeveRetornar400() throws Exception {
        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("", "ana@mail.com", "senha-longa")))
                .andExpect(status().isBadRequest());

        verify(service, never()).criar(any(), eq(false));
    }

    @Test
    void erroDoServicoPropagaOStatus() throws Exception {
        when(service.criar(any(ClienteRequest.class), eq(false)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado"));

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", "ana@mail.com", "senha-longa")))
                .andExpect(status().isConflict());
    }

    // --- listagem: só staff ---

    @Test
    void staffListaClientes() throws Exception {
        when(service.listar()).thenReturn(List.of(
                cliente(1L, "Ana", "ana@mail.com"),
                cliente(2L, "Bruno", "bruno@mail.com")));

        mockMvc.perform(get("/api/clientes").with(user(staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[1].nome").value("Bruno"));
    }

    @Test
    void anonimoNaoListaClientes401SemDesafioDoBrowser() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void clienteNaoListaClientes403() throws Exception {
        mockMvc.perform(get("/api/clientes").with(user(clienteLogado())))
                .andExpect(status().isForbidden());

        verify(service, never()).listar();
    }

    @Test
    void identificacaoPorEmailFoiRemovida() throws Exception {
        mockMvc.perform(get("/api/clientes/por-email").param("email", "ana@mail.com"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/clientes/por-email").param("email", "ana@mail.com").with(user(staff())))
                .andExpect(status().is4xxClientError());
    }
}
