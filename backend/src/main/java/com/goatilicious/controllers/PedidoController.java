package com.goatilicious.controllers;

import com.goatilicious.dtos.PedidoRequest;
import com.goatilicious.models.Pedido;
import com.goatilicious.security.Acesso;
import com.goatilicious.services.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /** Staff: pedido em nome de qualquer cliente. Cliente: só para si, sempre ONLINE. */
    @PostMapping
    public ResponseEntity<Pedido> criar(@Valid @RequestBody PedidoRequest request, Authentication autenticacao) {
        Pedido pedido = Acesso.ehStaff(autenticacao)
                ? service.registrar(request)
                : service.registrarComoCliente(Acesso.clienteIdObrigatorio(autenticacao), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping
    public List<Pedido> listar(Authentication autenticacao) {
        return Acesso.ehStaff(autenticacao)
                ? service.listar()
                : service.listarDoCliente(Acesso.clienteIdObrigatorio(autenticacao));
    }

    @GetMapping("/{id}")
    public Pedido buscarPorId(@PathVariable Long id, Authentication autenticacao) {
        return Acesso.ehStaff(autenticacao)
                ? service.buscarPorId(id)
                : service.buscarDoCliente(id, Acesso.clienteIdObrigatorio(autenticacao));
    }
}
