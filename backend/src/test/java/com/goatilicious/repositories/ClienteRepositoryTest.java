package com.goatilicious.repositories;

import com.goatilicious.models.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    private Cliente novoCliente(String nome, String email) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setEmail(email);
        c.setTelefone("+258 84 000 0000");
        c.setEndereco("Av. Julius Nyerere, 100, Maputo");
        return c;
    }

    @Test
    void deveGravarClienteGerandoIdEDataCadastro() {
        Cliente salvo = repository.save(novoCliente("Ana", "ana@mail.com"));

        assertNotNull(salvo.getIdCliente());
        assertEquals(LocalDate.now(), salvo.getDataCadastro());
    }

    @Test
    void deveListarTodosOsClientes() {
        repository.save(novoCliente("Ana", "ana@mail.com"));
        repository.save(novoCliente("Bruno", "bruno@mail.com"));

        List<Cliente> todos = repository.findAll();

        assertEquals(2, todos.size());
    }

    @Test
    void deveBuscarClientePorEmail() {
        repository.save(novoCliente("Ana", "ana@mail.com"));

        Optional<Cliente> encontrado = repository.findByEmail("ana@mail.com");

        assertTrue(encontrado.isPresent());
        assertEquals("Ana", encontrado.get().getNome());
    }

    @Test
    void deveRetornarVazioQuandoEmailNaoExiste() {
        assertTrue(repository.findByEmail("naoexiste@mail.com").isEmpty());
        assertFalse(repository.existsByEmail("naoexiste@mail.com"));
    }

    @Test
    void naoDevePermitirEmailDuplicado() {
        repository.saveAndFlush(novoCliente("Ana", "ana@mail.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(novoCliente("Outra Ana", "ana@mail.com")));
    }
}
