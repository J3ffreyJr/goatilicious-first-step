package com.goatilicious.security;

import com.goatilicious.enums.Papel;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/** Utilizador autenticado: além do papel, carrega o nome e (para clientes) o id do cliente. */
public class ContaAutenticada extends User {

    private final Long idUtilizador;
    private final String nome;
    private final Papel papel;
    private final Long clienteId;

    public ContaAutenticada(Long idUtilizador, String nome, String email, String senhaHash,
                            Papel papel, Long clienteId) {
        super(email, senhaHash, List.of(new SimpleGrantedAuthority("ROLE_" + papel.name())));
        this.idUtilizador = idUtilizador;
        this.nome = nome;
        this.papel = papel;
        this.clienteId = clienteId;
    }

    public Long getIdUtilizador() { return idUtilizador; }
    public String getNome() { return nome; }
    public Papel getPapel() { return papel; }
    /** Null para contas STAFF. */
    public Long getClienteId() { return clienteId; }
}
