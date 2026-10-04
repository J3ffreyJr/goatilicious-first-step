package com.goatilicious.dtos;

import com.goatilicious.enums.OrigemPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corpo do POST /api/pedidos. O cliente (site) ou o funcionário (painel) só indica
 * QUEM compra, ONDE foi feito o pedido e O QUÊ; preço, data e estado são definidos pelo servidor.
 */
public record PedidoRequest(
        @NotNull Long clienteId,
        @NotNull OrigemPedido origem,
        @NotEmpty @Valid List<ItemRequest> itens) {

    public record ItemRequest(
            @NotNull Long produtoId,
            @NotNull @Min(1) Integer quantidade) {
    }
}
