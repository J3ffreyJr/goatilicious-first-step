package com.goatilicious.services;

import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/** Gera o PDF de uma fatura. O mesmo PDF serve para imprimir e para anexar ao e-mail. */
@Service
public class FacturaPdfService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font MARCA = new Font(Font.HELVETICA, 22, Font.BOLD);
    private static final Font TITULO = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font NEGRITO = new Font(Font.HELVETICA, 11, Font.BOLD);

    /** Só faturas finais geram PDF; um rascunho ainda pode mudar na revisão. */
    public byte[] gerar(Factura factura) {
        if (factura.getEstado() != EstadoFactura.FINAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A factura ainda é um rascunho; reveja o pedido primeiro");
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 50, 50);
        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            documento.add(new Paragraph("GOATILICIOUS", MARCA));
            documento.add(new Paragraph("Sorvetes artesanais de leite de cabra", NORMAL));
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph("Factura " + factura.getIdFactura(), TITULO));
            documento.add(new Paragraph("Pedido: " + factura.getPedido().getIdPedido(), NORMAL));
            documento.add(new Paragraph("Emitida em: " + factura.getDataEmissao().format(DATA), NORMAL));
            documento.add(new Paragraph(" "));

            adicionarCliente(documento, factura.getPedido().getCliente());
            documento.add(new Paragraph(" "));
            documento.add(tabelaDeItens(factura));
            documento.add(new Paragraph(" "));

            Paragraph total = new Paragraph("Total: " + mt(factura.getValorTotal()), TITULO);
            total.setAlignment(Element.ALIGN_RIGHT);
            documento.add(total);
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Condições de pagamento: " + rotulo(factura.getCondicaoPagamento()), NORMAL));
            documento.add(new Paragraph("Estado do pagamento: " + factura.getStatusPagamento(), NORMAL));

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Não foi possível gerar o PDF da factura", e);
        }
        return saida.toByteArray();
    }

    private void adicionarCliente(Document documento, Cliente cliente) throws DocumentException {
        documento.add(new Paragraph("Cliente", NEGRITO));
        documento.add(new Paragraph(cliente.getNome(), NORMAL));
        documento.add(new Paragraph("E-mail: " + cliente.getEmail(), NORMAL));
        if (preenchido(cliente.getTelefone())) {
            documento.add(new Paragraph("Telefone: " + cliente.getTelefone().trim(), NORMAL));
        }
        if (preenchido(cliente.getEndereco())) {
            documento.add(new Paragraph("Endereço: " + cliente.getEndereco().trim(), NORMAL));
        }
    }

    private PdfPTable tabelaDeItens(Factura factura) throws DocumentException {
        PdfPTable tabela = new PdfPTable(new float[]{4f, 1.2f, 2f, 2f});
        tabela.setWidthPercentage(100);
        for (String cabecalho : new String[]{"Produto", "Qtd", "Preço", "Subtotal"}) {
            tabela.addCell(new PdfPCell(new Phrase(cabecalho, NEGRITO)));
        }
        for (ItemPedido item : factura.getPedido().getItens()) {
            tabela.addCell(new PdfPCell(new Phrase(
                    item.getProduto().getSabor() + " " + rotulo(item.getProduto().getTamanho()), NORMAL)));
            tabela.addCell(new PdfPCell(new Phrase(String.valueOf(item.getQuantidade()), NORMAL)));
            tabela.addCell(new PdfPCell(new Phrase(mt(item.getPrecoAplicado()), NORMAL)));
            tabela.addCell(new PdfPCell(new Phrase(mt(item.getSubtotal()), NORMAL)));
        }
        return tabela;
    }

    private static String mt(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString() + " MT";
    }

    private static boolean preenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String rotulo(TamanhoProduto tamanho) {
        return switch (tamanho) {
            case ML_125 -> "125 ml";
            case ML_500 -> "500 ml";
            case L_1 -> "1 L";
            case L_5 -> "5 L";
        };
    }

    private static String rotulo(CondicaoPagamento condicao) {
        return switch (condicao) {
            case A_VISTA -> "À vista";
            case CARTAO -> "Cartão";
            case PAGO_NA_ENTREGA -> "Pagamento na entrega";
        };
    }
}
