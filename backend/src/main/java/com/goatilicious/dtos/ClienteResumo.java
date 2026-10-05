package com.goatilicious.dtos;

/** Dados mínimos de um cliente expostos ao site (sem email, telefone ou endereço). */
public record ClienteResumo(Long idCliente, String nome) {
}
