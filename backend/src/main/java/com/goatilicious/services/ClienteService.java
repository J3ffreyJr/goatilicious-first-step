package com.goatilicious.services;

import com.goatilicious.dtos.ClienteRequest;
import com.goatilicious.enums.Papel;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Utilizador;
import com.goatilicious.repositories.ClienteRepository;
import com.goatilicious.repositories.UtilizadorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ClienteService {

    static final int SENHA_MINIMA = 8;

    private final ClienteRepository clienteRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository,
                          UtilizadorRepository utilizadorRepository,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cadastro. Quem se regista sozinho tem de definir a senha; o staff pode registar um cliente
     * sem senha (pedido por telefone), e então o cliente fica sem login.
     */
    @Transactional
    public Cliente criar(ClienteRequest request, boolean porStaff) {
        String senha = (request.senha() == null || request.senha().isBlank()) ? null : request.senha();
        if (senha == null && !porStaff) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A palavra-passe é obrigatória");
        }
        if (senha != null && senha.length() < SENHA_MINIMA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A palavra-passe deve ter pelo menos " + SENHA_MINIMA + " caracteres");
        }

        String email = request.email().trim();
        String emailConta = email.toLowerCase(Locale.ROOT);
        if (clienteRepository.existsByEmail(email)
                || (senha != null && utilizadorRepository.existsByEmail(emailConta))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setEmail(email);
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());
        Cliente salvo = clienteRepository.save(cliente);

        if (senha != null) {
            Utilizador conta = new Utilizador();
            conta.setNome(salvo.getNome());
            conta.setEmail(emailConta);
            conta.setSenhaHash(passwordEncoder.encode(senha));
            conta.setPapel(Papel.CLIENTE);
            conta.setCliente(salvo);
            utilizadorRepository.save(conta);
        }
        return salvo;
    }

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }
}
