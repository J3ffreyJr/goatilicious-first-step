package com.goatilicious.dtos;

import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.enums.CondicaoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corpo do PUT /api/pedidos/{id}/revisao. A lista de itens é a lista FINAL do pedido:
 * substitui os itens atuais (permite alterar quantidades, remover e adicionar produtos).
 */
public record RevisaoRequest(
        @NotNull CondicaoPagamento condicaoPagamento,
        @NotEmpty @Valid List<ItemRequest> itens) {
}
