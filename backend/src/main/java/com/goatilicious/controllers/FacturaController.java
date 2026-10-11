package com.goatilicious.controllers;

import com.goatilicious.dtos.EnvioResposta;
import com.goatilicious.dtos.FacturaResposta;
import com.goatilicious.dtos.RevisaoRequest;
import com.goatilicious.services.EnvioFacturaService;
import com.goatilicious.services.FacturaPdfService;
import com.goatilicious.services.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
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
    public FacturaResposta buscarPorId(@PathVariable Long id) {
        return FacturaResposta.de(service.buscarPorId(id));
    }

    @GetMapping("/pedidos/{id}/factura")
    public FacturaResposta buscarPorPedido(@PathVariable Long id) {
        return FacturaResposta.de(service.buscarPorPedido(id));
    }

    @PutMapping("/pedidos/{id}/revisao")
    public FacturaResposta revisar(@PathVariable Long id, @Valid @RequestBody RevisaoRequest request) {
        return FacturaResposta.de(service.revisar(id, request));
    }

    /** PDF da fatura final: o navegador abre-o para imprimir ou gravar. */
    @GetMapping("/facturas/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = pdfService.gerar(service.buscarPorId(id));
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
}
