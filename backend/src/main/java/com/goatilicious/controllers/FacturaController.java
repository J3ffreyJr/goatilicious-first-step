package com.goatilicious.controllers;

import com.goatilicious.dtos.EnvioResposta;
import com.goatilicious.dtos.FacturaResposta;
import com.goatilicious.dtos.RevisaoRequest;
import com.goatilicious.models.Factura;
import com.goatilicious.security.Acesso;
import com.goatilicious.services.EnvioFacturaService;
import com.goatilicious.services.FacturaPdfService;
import com.goatilicious.services.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FacturaController {

    private final FacturaService service;
    private final FacturaPdfService pdfService;
    private final EnvioFacturaService envioService;

    public FacturaController(FacturaService service,
                             FacturaPdfService pdfService,
                             EnvioFacturaService envioService) {
        this.service = service;
        this.pdfService = pdfService;
        this.envioService = envioService;
    }

    @GetMapping("/facturas")
    public List<FacturaResposta> listar() {
        return service.listar().stream().map(FacturaResposta::de).toList();
    }

    @GetMapping("/facturas/{id}")
    public FacturaResposta buscarPorId(@PathVariable Long id, Authentication autenticacao) {
        return FacturaResposta.de(facturaVisivel(id, autenticacao));
    }

    @GetMapping("/pedidos/{id}/factura")
    public FacturaResposta buscarPorPedido(@PathVariable Long id, Authentication autenticacao) {
        Factura factura = Acesso.ehStaff(autenticacao)
                ? service.buscarPorPedido(id)
                : service.buscarPorPedidoDoCliente(id, Acesso.clienteIdObrigatorio(autenticacao));
        return FacturaResposta.de(factura);
    }

    @PutMapping("/pedidos/{id}/revisao")
    public FacturaResposta revisar(@PathVariable Long id, @Valid @RequestBody RevisaoRequest request) {
        return FacturaResposta.de(service.revisar(id, request));
    }

    /** PDF da fatura final: o navegador abre-o para imprimir ou gravar. */
    @GetMapping("/facturas/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, Authentication autenticacao) {
        byte[] pdf = pdfService.gerar(facturaVisivel(id, autenticacao));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("factura-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @PostMapping("/facturas/{id}/envio")
    public EnvioResposta enviar(@PathVariable Long id) {
        return EnvioResposta.de(envioService.enviar(id));
    }

    @GetMapping("/facturas/{id}/envios")
    public List<EnvioResposta> listarEnvios(@PathVariable Long id) {
        return envioService.listarEnvios(id).stream().map(EnvioResposta::de).toList();
    }

    /** Staff vê qualquer fatura; o cliente só as suas (as de outros dão 404). */
    private Factura facturaVisivel(Long id, Authentication autenticacao) {
        return Acesso.ehStaff(autenticacao)
                ? service.buscarPorId(id)
                : service.buscarDoCliente(id, Acesso.clienteIdObrigatorio(autenticacao));
    }
}
