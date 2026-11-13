package com.goatilicious.repositories;

import com.goatilicious.enums.Papel;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UtilizadorRepositoryTest {

    @Autowired
    private UtilizadorRepository repository;
    @Autowired
    private ClienteRepository clienteRepository;

    private Utilizador conta(String email, Papel papel) {
        Utilizador u = new Utilizador();
        u.setNome("Nome");
        u.setEmail(email);
        u.setSenhaHash("$2a$10$hash");
        u.setPapel(papel);
        return u;
    }

    @Test
    void gravaEEncontraPorEmail() {
        repository.save(conta("maria@goat.co.mz", Papel.STAFF));

        Utilizador lido = repository.findByEmail("maria@goat.co.mz").orElseThrow();
        assertNotNull(lido.getIdUtilizador());
        assertEquals(Papel.STAFF, lido.getPapel());
        assertTrue(repository.existsByEmail("maria@goat.co.mz"));
        assertFalse(repository.existsByEmail("outro@goat.co.mz"));
    }

    @Test
    void emailDeveSerUnico() {
        repository.saveAndFlush(conta("a@goat.co.mz", Papel.STAFF));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(conta("a@goat.co.mz", Papel.CLIENTE)));
    }

    @Test
    void contaDeClienteFicaLigadaAoCliente() {
        Cliente c = new Cliente();
        c.setNome("Ana");
        c.setEmail("ana@mail.com");
        c = clienteRepository.save(c);

        Utilizador u = conta("ana@mail.com", Papel.CLIENTE);
        u.setCliente(c);
        repository.saveAndFlush(u);

        assertEquals(c.getIdCliente(), repository.findByEmail("ana@mail.com").orElseThrow().getCliente().getIdCliente());
    }
}
