package com.goatilicious.security;

import com.goatilicious.enums.Papel;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcessoTest {

    private Authentication de(ContaAutenticada conta) {
        return new UsernamePasswordAuthenticationToken(conta, null, conta.getAuthorities());
    }

    private ContaAutenticada staff() {
        return new ContaAutenticada(1L, "Maria", "maria@goat.co.mz", "hash", Papel.STAFF, null);
    }

    private ContaAutenticada cliente(Long clienteId) {
        return new ContaAutenticada(2L, "Ana", "ana@mail.com", "hash", Papel.CLIENTE, clienteId);
    }

    @Test
    void staffEReconhecido() {
        assertTrue(Acesso.ehStaff(de(staff())));
    }

    @Test
    void clienteEAnonimoNaoSaoStaff() {
        assertFalse(Acesso.ehStaff(de(cliente(7L))));
        assertFalse(Acesso.ehStaff(null));
    }

    @Test
    void clienteIdVemDaContaAutenticada() {
        assertEquals(7L, Acesso.clienteId(de(cliente(7L))));
    }

    @Test
    void clienteIdEhNuloParaStaffEAnonimo() {
        assertNull(Acesso.clienteId(de(staff())));
        assertNull(Acesso.clienteId(null));
    }

    @Test
    void clienteIdObrigatorioSemClienteDa403() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> Acesso.clienteIdObrigatorio(de(staff())));
        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    void contaGuardaPapelComoAutoridade() {
        assertEquals("ROLE_CLIENTE", cliente(7L).getAuthorities().iterator().next().getAuthority());
    }
}
