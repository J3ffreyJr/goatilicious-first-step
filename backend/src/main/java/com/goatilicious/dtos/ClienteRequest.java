package com.goatilicious.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /api/clientes. A senha nunca volta nas respostas. */
public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório") String nome,
        @NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") String email,
        String telefone,
        String endereco,
        String senha) {
}
