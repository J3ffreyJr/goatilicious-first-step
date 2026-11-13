package com.goatilicious.services;

import com.goatilicious.enums.Papel;
import com.goatilicious.models.Utilizador;
import com.goatilicious.repositories.UtilizadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UtilizadorService {

    private final UtilizadorRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UtilizadorService(UtilizadorRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Cria uma conta de staff; se o email já existe, não altera nada e devolve a conta existente. */
    @Transactional
    public Utilizador criarStaff(String nome, String email, String senha) {
        String emailConta = email.trim().toLowerCase(Locale.ROOT);
        return repository.findByEmail(emailConta).orElseGet(() -> {
            Utilizador staff = new Utilizador();
            staff.setNome(nome);
            staff.setEmail(emailConta);
            staff.setSenhaHash(passwordEncoder.encode(senha));
            staff.setPapel(Papel.STAFF);
            return repository.save(staff);
        });
    }
}
