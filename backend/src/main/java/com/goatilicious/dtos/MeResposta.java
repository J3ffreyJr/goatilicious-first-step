package com.goatilicious.dtos;

import com.goatilicious.enums.Papel;
import com.goatilicious.security.ContaAutenticada;

/** Quem está autenticado: o site usa isto para validar o login e escolher o ecrã. */
public record MeResposta(String nome, String email, Papel papel, Long clienteId) {

    public static MeResposta de(ContaAutenticada conta) {
        return new MeResposta(conta.getNome(), conta.getUsername(), conta.getPapel(), conta.getClienteId());
    }
}
