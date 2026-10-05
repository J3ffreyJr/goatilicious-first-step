package com.goatilicious.repositories;

import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository repository;

    private Produto produto(String sabor, TamanhoProduto tamanho, String preco) {
        Produto p = new Produto();
        p.setSabor(sabor);
        p.setTamanho(tamanho);
        p.setPrecoUnitario(new BigDecimal(preco));
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        return p;
    }

    @Test
    void deveGravarProdutoGerandoId() {
        Produto salvo = repository.save(produto("Mirtilo Silvestre", TamanhoProduto.L_1, "650.00"));

        assertNotNull(salvo.getIdProduto());
    }

    @Test
    void deveListarTodosOsProdutos() {
        repository.save(produto("Mirtilo Silvestre", TamanhoProduto.ML_500, "350.00"));
        repository.save(produto("Baunilha & Mel", TamanhoProduto.ML_500, "350.00"));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void naoDevePermitirMesmoSaborETamanhoDuplicados() {
        repository.saveAndFlush(produto("Mirtilo Silvestre", TamanhoProduto.ML_500, "350.00"));

        assertThrows(DataIntegrityViolationException.class, () ->
                repository.saveAndFlush(produto("Mirtilo Silvestre", TamanhoProduto.ML_500, "400.00")));
    }

    @Test
    void mesmoSaborEmTamanhosDiferentesEPermitido() {
        repository.saveAndFlush(produto("Mirtilo Silvestre", TamanhoProduto.ML_500, "350.00"));
        repository.saveAndFlush(produto("Mirtilo Silvestre", TamanhoProduto.L_1, "650.00"));

        assertEquals(2, repository.count());
    }
}
