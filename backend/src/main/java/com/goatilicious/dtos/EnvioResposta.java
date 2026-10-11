package com.goatilicious.dtos;

import com.goatilicious.models.EnvioFactura;

import java.time.LocalDateTime;

public record EnvioResposta(
        Long idEnvio,
        Long idFactura,
        LocalDateTime dataEnvio,
        String destinatario,
        boolean sucesso,
        String erro) {

    public static EnvioResposta de(EnvioFactura e) {
        return new EnvioResposta(
                e.getIdEnvio(),
                e.getFactura().getIdFactura(),
                e.getDataEnvio(),
                e.getDestinatario(),
                e.isSucesso(),
                e.getErro());
    }
}
