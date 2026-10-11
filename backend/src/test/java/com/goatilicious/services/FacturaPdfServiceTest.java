package com.goatilicious.services;

import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FacturaPdfServiceTest {

    private final FacturaPdfService service = new FacturaPdfService();

    private Cliente cliente;
    private Factura factura;

    @BeforeEach
    void preparar() {
        cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNome("Ana Silva");
        cliente.setEmail("ana@mail.com");
        cliente.setTelefone("841234567");
        cliente.setEndereco("Av. Julius Nyerere 100");

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

        Pedido pedido = new Pedido();
        pedido.setIdPedido(5L);
        pedido.setCliente(cliente);
        pedido.setOrigem(OrigemPedido.ONLINE);
        pedido.adicionarItem(item);

        factura = Factura.rascunhoDe(pedido);
        factura.setIdFactura(7L);
        factura.finalizar(CondicaoPagamento.PAGO_NA_ENTREGA, new BigDecimal("700.00"));
    }

    private String texto(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    @Test
    void gerarDevolveUmPdfValido() {
        byte[] pdf = service.gerar(factura);

        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void pdfContemOsDadosPrincipaisDaFactura() throws Exception {
        String texto = texto(service.gerar(factura));

        assertTrue(texto.contains("GOATILICIOUS"), texto);
        assertTrue(texto.contains("Ana Silva"), texto);
        assertTrue(texto.contains("ana@mail.com"), texto);
        assertTrue(texto.contains("841234567"), texto);
        assertTrue(texto.contains("Cereja"), texto);
        assertTrue(texto.contains("500 ml"), texto);
        assertTrue(texto.contains("700.00 MT"), texto);
        assertTrue(texto.contains("Pagamento na entrega"), texto);
    }

    @Test
    void pdfOmiteTelefoneEEnderecoQuandoVazios() throws Exception {
        cliente.setTelefone(null);
        cliente.setEndereco("  ");

        String texto = texto(service.gerar(factura));

        assertFalse(texto.contains("Telefone"), texto);
        assertFalse(texto.contains("Endere"), texto);
    }

    @Test
    void pdfMostraSubtotalPorItem() throws Exception {
        String texto = texto(service.gerar(factura));

        // 2 x 350.00 = 700.00; o preço unitário também aparece
        assertTrue(texto.contains("350.00 MT"), texto);
    }

    @Test
    void facturaEmRascunhoNaoGeraPdfELanca409() {
        Factura rascunho = Factura.rascunhoDe(factura.getPedido());
        rascunho.setIdFactura(8L);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.gerar(rascunho));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }
}
