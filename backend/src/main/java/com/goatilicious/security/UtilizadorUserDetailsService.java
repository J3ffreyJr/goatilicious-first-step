package com.goatilicious.security;

import com.goatilicious.repositories.UtilizadorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UtilizadorUserDetailsService implements UserDetailsService {

    private final UtilizadorRepository repository;

    public UtilizadorUserDetailsService(UtilizadorRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return repository.findByEmail(email)
                .map(u -> new ContaAutenticada(u.getIdUtilizador(), u.getNome(), u.getEmail(), u.getSenhaHash(),
                        u.getPapel(), u.getCliente() == null ? null : u.getCliente().getIdCliente()))
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado"));
    }
}
