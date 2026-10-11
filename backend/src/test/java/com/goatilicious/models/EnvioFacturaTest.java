package com.goatilicious.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvioFacturaTest {

    @Test
    void sucessoGravaDestinatarioESemErro() {
        Factura factura = new Factura();

        EnvioFactura envio = EnvioFactura.sucesso(factura, "ana@mail.com");

        assertSame(factura, envio.getFactura());
        assertEquals("ana@mail.com", envio.getDestinatario());
        assertTrue(envio.isSucesso());
        assertNull(envio.getErro());
    }

    @Test
    void falhaGravaMensagemDeErro() {
        EnvioFactura envio = EnvioFactura.falha(new Factura(), "ana@mail.com", "SMTP em baixo");

        assertFalse(envio.isSucesso());
        assertEquals("SMTP em baixo", envio.getErro());
    }

    @Test
    void falhaTruncaMensagensLongasA500Caracteres() {
        EnvioFactura envio = EnvioFactura.falha(new Factura(), "ana@mail.com", "x".repeat(900));

        assertEquals(500, envio.getErro().length());
    }

    @Test
    void falhaSemMensagemUsaTextoPadrao() {
        EnvioFactura envio = EnvioFactura.falha(new Factura(), "ana@mail.com", null);

        assertEquals("Erro desconhecido", envio.getErro());
    }
}
