package com.goatilicious.dtos;

import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.StatusPagamento;
import com.goatilicious.models.Factura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FacturaResposta(
        Long idFactura,
        Long idPedido,
        String nomeCliente,
        LocalDateTime dataEmissao,
        BigDecimal valorTotal,
        CondicaoPagamento condicaoPagamento,
        EstadoFactura estado,
        StatusPagamento statusPagamento) {

    public static FacturaResposta de(Factura f) {
        return new FacturaResposta(
                f.getIdFactura(),
                f.getPedido().getIdPedido(),
                f.getPedido().getCliente().getNome(),
                f.getDataEmissao(),
                f.getValorTotal(),
                f.getCondicaoPagamento(),
                f.getEstado(),
                f.getStatusPagamento());
    }
}
