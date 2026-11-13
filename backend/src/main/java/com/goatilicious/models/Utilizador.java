package com.goatilicious.models;

import com.goatilicious.enums.Papel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Conta de acesso (login). O perfil de negócio do cliente continua em {@link Cliente};
 * só as contas CLIENTE têm cliente associado. A senha guarda-se apenas como hash BCrypt.
 */
@Entity
@Table(name = "utilizador")
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilizador")
    private Long idUtilizador;

    @Column(nullable = false)
    private String nome;

    /** Nome de utilizador: sempre em minúsculas e sem espaços nas pontas. */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @OneToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    public Long getIdUtilizador() { return idUtilizador; }
    public void setIdUtilizador(Long idUtilizador) { this.idUtilizador = idUtilizador; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
