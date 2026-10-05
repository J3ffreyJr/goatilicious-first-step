package com.goatilicious.controllers;

import com.goatilicious.dtos.ClienteResumo;
import com.goatilicious.models.Cliente;
import com.goatilicious.repositories.ClienteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin
public class ClienteController {

    private final ClienteRepository repository;

    public ClienteController(ClienteRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody Cliente cliente) {
        if (repository.existsByEmail(cliente.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }
        cliente.setIdCliente(null);      // o ID é sempre gerado pelo servidor
        cliente.setDataCadastro(null);   // a data é definida no @PrePersist
        Cliente salvo = repository.save(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public List<Cliente> listar() {
        return repository.findAll();
    }

    /** Usado pelo site para identificar o cliente (sem login): devolve apenas id e nome. */
    @GetMapping("/por-email")
    public ClienteResumo buscarPorEmail(@RequestParam String email) {
        return repository.findByEmail(email.trim())
                .map(c -> new ClienteResumo(c.getIdCliente(), c.getNome()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }
}
