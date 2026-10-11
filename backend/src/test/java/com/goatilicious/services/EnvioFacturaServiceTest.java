package com.goatilicious.services;

import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.mail.EmailFalhouException;
import com.goatilicious.mail.EmailNaoConfiguradoException;
import com.goatilicious.mail.EmailSender;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.EnvioFactura;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.EnvioFacturaRepository;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioFacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private EnvioFacturaRepository envioRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private FacturaPdfService pdfService;
    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private EnvioFacturaService service;

    private final byte[] pdf = {37, 80, 68, 70};   // "%PDF"
    private Pedido pedido;
    private Factura factura;

    @BeforeEach
    void preparar() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNome("Ana");
        cliente.setEmail("ana@mail.com");

        Produto p = new Produto();
        p.setIdProduto(10L);
        p.setSabor("Cereja");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal("350.00"));

        ItemPedido item = new ItemPedido();
        item.setProduto(p);
        item.setQuantidade(2);
        item.setPrecoAplicado(new BigDecimal("350.00"));

        pedido = new Pedido();
        pedido.setIdPedido(5L);
        pedido.setCliente(cliente);
        pedido.setOrigem(OrigemPedido.ONLINE);
        pedido.setStatus(StatusPedido.REVISADO);
        pedido.adicionarItem(item);

        factura = Factura.rascunhoDe(pedido);
        factura.setIdFactura(7L);
        factura.finalizar(CondicaoPagamento.A_VISTA, new BigDecimal("700.00"));
    }

    private void facturaFinalComPdf() {
        when(facturaRepository.findById(7L)).thenReturn(Optional.of(factura));
        when(pdfService.gerar(factura)).thenReturn(pdf);
        when(envioRepository.save(any(EnvioFactura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private EnvioFactura envioGravado() {
        ArgumentCaptor<EnvioFactura> captor = ArgumentCaptor.forClass(EnvioFactura.class);
        verify(envioRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void enviarMandaOPdfParaOEmailDoCliente() {
        facturaFinalComPdf();

        service.enviar(7L);

        verify(emailSender).enviar(eq("ana@mail.com"), eq("Factura 7 - Goatilicious"), anyString(),
                eq("factura-7.pdf"), eq(pdf));
    }

    @Test
    void enviarComSucessoGravaEnvioEMarcaPedidoComoEnviado() {
        facturaFinalComPdf();

        EnvioFactura envio = service.enviar(7L);

        assertTrue(envio.isSucesso());
        assertEquals("ana@mail.com", envio.getDestinatario());
        assertSame(factura, envio.getFactura());
        assertEquals(StatusPedido.ENVIADO, pedido.getStatus());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void smtpNaoConfiguradoGravaFalhaELancaErro503() {
        facturaFinalComPdf();
        doThrow(new EmailNaoConfiguradoException()).when(emailSender)
                .enviar(anyString(), anyString(), anyString(), anyString(), any(byte[].class));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.enviar(7L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        EnvioFactura gravado = envioGravado();
        assertFalse(gravado.isSucesso());
        assertEquals("E-mail não configurado", gravado.getErro());
        assertEquals(StatusPedido.REVISADO, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void falhaNoEnvioGravaErroELancaErro502SemMudarOPedido() {
        facturaFinalComPdf();
        doThrow(new EmailFalhouException("SMTP em baixo", new RuntimeException())).when(emailSender)
                .enviar(anyString(), anyString(), anyString(), anyString(), any(byte[].class));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.enviar(7L));

        assertEquals(HttpStatus.BAD_GATEWAY, erro.getStatusCode());
        EnvioFactura gravado = envioGravado();
        assertFalse(gravado.isSucesso());
        assertEquals("SMTP em baixo", gravado.getErro());
        assertEquals(StatusPedido.REVISADO, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void erroInesperadoNoEnvioGravaFalhaELancaErro502SemMudarOPedido() {
        facturaFinalComPdf();
        doThrow(new IllegalArgumentException("endereço inválido")).when(emailSender)
                .enviar(anyString(), anyString(), anyString(), anyString(), any(byte[].class));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.enviar(7L));

        assertEquals(HttpStatus.BAD_GATEWAY, erro.getStatusCode());
        EnvioFactura gravado = envioGravado();
        assertFalse(gravado.isSucesso());
        assertEquals("endereço inválido", gravado.getErro());
        assertEquals(StatusPedido.REVISADO, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void falhaAGerarOPdfGravaFalhaELancaErro502() {
        when(facturaRepository.findById(7L)).thenReturn(Optional.of(factura));
        when(pdfService.gerar(factura)).thenThrow(new IllegalStateException("PDF falhou"));
        when(envioRepository.save(any(EnvioFactura.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.enviar(7L));

        assertEquals(HttpStatus.BAD_GATEWAY, erro.getStatusCode());
        EnvioFactura gravado = envioGravado();
        assertFalse(gravado.isSucesso());
        assertEquals("PDF falhou", gravado.getErro());
        verify(emailSender, never()).enviar(anyString(), anyString(), anyString(), anyString(), any(byte[].class));
        assertEquals(StatusPedido.REVISADO, pedido.getStatus());
    }

    @Test
    void facturaEmRascunhoLanca409ENadaEEnviadoNemGravado() {
        Factura rascunho = Factura.rascunhoDe(pedido);
        rascunho.setIdFactura(8L);
        when(facturaRepository.findById(8L)).thenReturn(Optional.of(rascunho));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.enviar(8L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(emailSender, never()).enviar(anyString(), anyString(), anyString(), anyString(), any(byte[].class));
        verify(envioRepository, never()).save(any());
    }

    @Test
    void facturaInexistenteLanca404() {
        when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.enviar(99L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verify(envioRepository, never()).save(any());
    }

    @Test
    void reenvioDePedidoJaEnviadoGravaNovoEnvioEMantemEnviado() {
        pedido.setStatus(StatusPedido.ENVIADO);
        facturaFinalComPdf();

        EnvioFactura envio = service.enviar(7L);

        assertTrue(envio.isSucesso());
        assertEquals(StatusPedido.ENVIADO, pedido.getStatus());
    }

    @Test
    void listarEnviosDevolveOHistoricoDaFactura() {
        EnvioFactura envio = EnvioFactura.sucesso(factura, "ana@mail.com");
        when(facturaRepository.existsById(7L)).thenReturn(true);
        when(envioRepository.findByFacturaIdFacturaOrderByDataEnvioDescIdEnvioDesc(7L)).thenReturn(List.of(envio));

        assertEquals(1, service.listarEnvios(7L).size());
    }

    @Test
    void listarEnviosDeFacturaInexistenteLanca404() {
        when(facturaRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.listarEnvios(99L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }
}
