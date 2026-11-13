package com.goatilicious.controllers;

import com.goatilicious.dtos.MeResposta;
import com.goatilicious.enums.Papel;
import com.goatilicious.security.Acesso;
import com.goatilicious.security.ContaAutenticada;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    /** Valida o login (Basic) e diz ao site quem é e que ecrã mostrar. */
    @GetMapping
    public MeResposta me(Authentication autenticacao) {
        if (autenticacao.getPrincipal() instanceof ContaAutenticada conta) {
            return MeResposta.de(conta);
        }
        Papel papel = Acesso.ehStaff(autenticacao) ? Papel.STAFF : Papel.CLIENTE;
        return new MeResposta(autenticacao.getName(), autenticacao.getName(), papel, Acesso.clienteId(autenticacao));
    }
}
