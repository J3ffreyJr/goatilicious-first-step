package com.goatilicious.controllers;

import com.goatilicious.dtos.ClienteRequest;
import com.goatilicious.models.Cliente;
import com.goatilicious.security.Acesso;
import com.goatilicious.services.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    /** Cadastro público (senha obrigatória); o staff pode registar um cliente sem senha. */
    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody ClienteRequest request, Authentication autenticacao) {
        Cliente salvo = service.criar(request, Acesso.ehStaff(autenticacao));
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    /** Só staff (regra no SecurityConfig). */
    @GetMapping
    public List<Cliente> listar() {
        return service.listar();
    }
}
