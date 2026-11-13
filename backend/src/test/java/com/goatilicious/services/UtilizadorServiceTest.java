package com.goatilicious.services;

import com.goatilicious.enums.Papel;
import com.goatilicious.models.Utilizador;
import com.goatilicious.repositories.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilizadorServiceTest {

    @Mock
    private UtilizadorRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UtilizadorService service;

    @Test
    void criaStaffComHashEEmailEmMinusculas() {
        when(repository.findByEmail("maria@goat.co.mz")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("segredo-forte")).thenReturn("HASH");
        when(repository.save(any(Utilizador.class))).thenAnswer(i -> i.getArgument(0));

        service.criarStaff("Maria", " Maria@Goat.co.mz ", "segredo-forte");

        ArgumentCaptor<Utilizador> captor = ArgumentCaptor.forClass(Utilizador.class);
        verify(repository).save(captor.capture());
        assertEquals("maria@goat.co.mz", captor.getValue().getEmail());
        assertEquals("HASH", captor.getValue().getSenhaHash());
        assertEquals(Papel.STAFF, captor.getValue().getPapel());
    }

    @Test
    void seOStaffJaExisteNaoAlteraNada() {
        Utilizador existente = new Utilizador();
        when(repository.findByEmail("maria@goat.co.mz")).thenReturn(Optional.of(existente));

        Utilizador resultado = service.criarStaff("Maria", "maria@goat.co.mz", "outra-senha");

        assertSame(existente, resultado);
        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
