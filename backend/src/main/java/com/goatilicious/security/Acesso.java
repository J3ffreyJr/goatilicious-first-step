package com.goatilicious.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/** Leitura do papel e do cliente a partir da autenticação do pedido (tolera autenticação nula). */
public final class Acesso {

    private Acesso() {
    }

    public static boolean ehStaff(Authentication autenticacao) {
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STAFF".equals(a.getAuthority()));
    }

    /** Id do cliente da sessão, ou null se não houver (staff, anónimo ou conta sem cliente). */
    public static Long clienteId(Authentication autenticacao) {
        if (autenticacao != null && autenticacao.getPrincipal() instanceof ContaAutenticada conta) {
            return conta.getClienteId();
        }
        return null;
    }

    /** Como {@link #clienteId}, mas responde 403 se a sessão não tiver cliente. */
    public static Long clienteIdObrigatorio(Authentication autenticacao) {
        Long id = clienteId(autenticacao);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A conta não tem cliente associado");
        }
        return id;
    }
}
