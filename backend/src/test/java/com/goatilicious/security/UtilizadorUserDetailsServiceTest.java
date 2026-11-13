package com.goatilicious.security;

import com.goatilicious.enums.Papel;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Utilizador;
import com.goatilicious.repositories.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilizadorUserDetailsServiceTest {

    @Mock
    private UtilizadorRepository repository;

    @InjectMocks
    private UtilizadorUserDetailsService service;

    private Utilizador conta(Papel papel, Cliente cliente) {
        Utilizador u = new Utilizador();
        u.setIdUtilizador(5L);
        u.setNome("Ana");
        u.setEmail("ana@mail.com");
        u.setSenhaHash("HASH");
        u.setPapel(papel);
        u.setCliente(cliente);
        return u;
    }

    @Test
    void carregaContaDeClienteComIdDoCliente() {
        Cliente c = new Cliente();
        c.setIdCliente(9L);
        when(repository.findByEmail("ana@mail.com")).thenReturn(Optional.of(conta(Papel.CLIENTE, c)));

        UserDetails d = service.loadUserByUsername("  ANA@mail.com ");

        ContaAutenticada conta = assertInstanceOf(ContaAutenticada.class, d);
        assertEquals(9L, conta.getClienteId());
        assertEquals("HASH", conta.getPassword());
        assertEquals("ROLE_CLIENTE", conta.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void contaStaffNaoTemClienteId() {
        when(repository.findByEmail("ana@mail.com")).thenReturn(Optional.of(conta(Papel.STAFF, null)));

        ContaAutenticada conta = (ContaAutenticada) service.loadUserByUsername("ana@mail.com");

        assertNull(conta.getClienteId());
    }

    @Test
    void utilizadorInexistenteLancaExcecao() {
        when(repository.findByEmail("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("x@mail.com"));
    }
}
