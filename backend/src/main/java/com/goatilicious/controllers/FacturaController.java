package com.goatilicious.controllers;

import com.goatilicious.dtos.FacturaResposta;
import com.goatilicious.dtos.RevisaoRequest;
import com.goatilicious.services.FacturaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public FacturaController(FacturaService service) {
        this.service = service;
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
}
