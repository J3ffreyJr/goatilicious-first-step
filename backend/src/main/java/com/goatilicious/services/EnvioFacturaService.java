package com.goatilicious.services;

import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.mail.EmailFalhouException;
import com.goatilicious.mail.EmailNaoConfiguradoException;
import com.goatilicious.mail.EmailSender;
import com.goatilicious.models.EnvioFactura;
import com.goatilicious.models.Factura;
import com.goatilicious.models.Pedido;
import com.goatilicious.repositories.EnvioFacturaRepository;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EnvioFacturaService {

    private final FacturaRepository facturaRepository;
    private final EnvioFacturaRepository envioRepository;
    private final PedidoRepository pedidoRepository;
    private final FacturaPdfService pdfService;
    private final EmailSender emailSender;

    public EnvioFacturaService(FacturaRepository facturaRepository,
                               EnvioFacturaRepository envioRepository,
                               PedidoRepository pedidoRepository,
                               FacturaPdfService pdfService,
                               EmailSender emailSender) {
        this.facturaRepository = facturaRepository;
        this.envioRepository = envioRepository;
        this.pedidoRepository = pedidoRepository;
        this.pdfService = pdfService;
        this.emailSender = emailSender;
    }

    /**
     * Envia a fatura final, em PDF, para o e-mail do cliente e regista a tentativa no histórico.
     * O registo de falha tem de sobreviver ao erro HTTP devolvido, por isso não há rollback
     * quando lançamos {@link ResponseStatusException}.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EnvioFactura enviar(Long idFactura) {
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura não encontrada"));

        if (factura.getEstado() != EstadoFactura.FINAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A factura ainda é um rascunho; reveja o pedido antes de a enviar");
        }

        Pedido pedido = factura.getPedido();
        String destinatario = pedido.getCliente().getEmail();

        try {
            emailSender.enviar(destinatario,
                    "Factura " + idFactura + " - Goatilicious",
                    corpo(factura),
                    "factura-" + idFactura + ".pdf",
                    pdfService.gerar(factura));
        } catch (EmailNaoConfiguradoException e) {
            envioRepository.save(EnvioFactura.falha(factura, destinatario, e.getMessage()));
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "E-mail não configurado");
        } catch (EmailFalhouException e) {
            envioRepository.save(EnvioFactura.falha(factura, destinatario, e.getMessage()));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao enviar o e-mail");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            // Qualquer outro erro (PDF, endereço inválido, ...) também fica no histórico.
            envioRepository.save(EnvioFactura.falha(factura, destinatario, e.getMessage()));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao enviar o e-mail");
        }

        pedido.setStatus(StatusPedido.ENVIADO);
        pedidoRepository.save(pedido);
        return envioRepository.save(EnvioFactura.sucesso(factura, destinatario));
    }

    public List<EnvioFactura> listarEnvios(Long idFactura) {
        if (!facturaRepository.existsById(idFactura)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura não encontrada");
        }
        return envioRepository.findByFacturaIdFacturaOrderByDataEnvioDescIdEnvioDesc(idFactura);
    }

    private String corpo(Factura factura) {
        return "Olá " + factura.getPedido().getCliente().getNome() + ",\n\n"
                + "Segue em anexo a factura " + factura.getIdFactura()
                + " do seu pedido " + factura.getPedido().getIdPedido() + ".\n\n"
                + "Obrigado,\nGoatilicious";
    }
}
