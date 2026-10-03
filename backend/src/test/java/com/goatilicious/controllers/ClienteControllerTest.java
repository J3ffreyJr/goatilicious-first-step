package com.goatilicious.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goatilicious.models.Cliente;
import com.goatilicious.repositories.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteRepository repository;

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

    @Test
    void postDeveCriarClienteERetornar201() throws Exception {
        Cliente entrada = cliente(null, "Ana", "ana@mail.com");
        when(repository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(repository.save(any(Cliente.class))).thenReturn(cliente(1L, "Ana", "ana@mail.com"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entrada)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(1))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"));
    }

    @Test
    void postComEmailInvalidoDeveRetornar400() throws Exception {
        Cliente entrada = cliente(null, "Ana", "email-invalido");

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entrada)))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any());
    }

    @Test
    void postSemNomeDeveRetornar400() throws Exception {
        Cliente entrada = cliente(null, "", "ana@mail.com");

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entrada)))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any());
    }

    @Test
    void postComEmailJaCadastradoDeveRetornar409() throws Exception {
        Cliente entrada = cliente(null, "Ana", "ana@mail.com");
        when(repository.existsByEmail("ana@mail.com")).thenReturn(true);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entrada)))
                .andExpect(status().isConflict());

        verify(repository, never()).save(any());
    }

    @Test
    void getDeveListarClientes() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                cliente(1L, "Ana", "ana@mail.com"),
                cliente(2L, "Bruno", "bruno@mail.com")));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[1].nome").value("Bruno"));
    }
}
