package com.goatilicious.services;

import com.goatilicious.dtos.ClienteRequest;
import com.goatilicious.enums.Papel;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Utilizador;
import com.goatilicious.repositories.ClienteRepository;
import com.goatilicious.repositories.UtilizadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UtilizadorRepository utilizadorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteService service;

    private Cliente salvo;

    @BeforeEach
    void preparar() {
        salvo = new Cliente();
        salvo.setIdCliente(1L);
        salvo.setNome("Ana");
        salvo.setEmail("Ana@Mail.com");
    }

    private ClienteRequest pedido(String senha) {
        return new ClienteRequest("Ana", " Ana@Mail.com ", "+258 84 000 0000", "Maputo", senha);
    }

    @Test
    void cadastroCriaClienteEContaClienteComHash() {
        when(clienteRepository.existsByEmail("Ana@Mail.com")).thenReturn(false);
        when(utilizadorRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(salvo);
        when(passwordEncoder.encode("senha-longa")).thenReturn("HASH");

        Cliente resultado = service.criar(pedido("senha-longa"), false);

        assertSame(salvo, resultado);
        ArgumentCaptor<Utilizador> captor = ArgumentCaptor.forClass(Utilizador.class);
        verify(utilizadorRepository).save(captor.capture());
        Utilizador conta = captor.getValue();
        assertEquals("ana@mail.com", conta.getEmail());
        assertEquals("HASH", conta.getSenhaHash());
        assertNotEquals("senha-longa", conta.getSenhaHash());
        assertEquals(Papel.CLIENTE, conta.getPapel());
        assertSame(salvo, conta.getCliente());
    }

    @Test
    void cadastroPublicoSemSenhaDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.criar(pedido(null), false));
        assertEquals(400, e.getStatusCode().value());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void senhaEmBrancoContaComoAusente() {
        assertThrows(ResponseStatusException.class, () -> service.criar(pedido("   "), false));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void senhaCurtaDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.criar(pedido("1234567"), false));
        assertEquals(400, e.getStatusCode().value());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void staffPodeRegistarClienteSemSenhaENaoCriaConta() {
        when(clienteRepository.existsByEmail("Ana@Mail.com")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(salvo);

        service.criar(pedido(null), true);

        verify(utilizadorRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void emailDeClienteJaCadastradoDa409() {
        when(clienteRepository.existsByEmail("Ana@Mail.com")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.criar(pedido("senha-longa"), false));
        assertEquals(409, e.getStatusCode().value());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void emailJaUsadoPorOutraContaDa409() {
        when(clienteRepository.existsByEmail("Ana@Mail.com")).thenReturn(false);
        when(utilizadorRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.criar(pedido("senha-longa"), false));
        assertEquals(409, e.getStatusCode().value());
        verify(clienteRepository, never()).save(any());
    }
}
