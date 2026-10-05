package com.goatilicious.config;

import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarregadorProdutosTest {

    @Mock
    private ProdutoRepository repository;

    @Captor
    private ArgumentCaptor<List<Produto>> captor;

    @InjectMocks
    private CarregadorProdutos carregador;

    @Test
    void catalogoVazioDeveSerPopuladoComTodosOsSaboresETamanhos() {
        when(repository.count()).thenReturn(0L);

        carregador.run();

        verify(repository).saveAll(captor.capture());
        List<Produto> produtos = captor.getValue();

        Set<String> sabores = produtos.stream().map(Produto::getSabor).collect(Collectors.toSet());
        assertEquals(Set.of("Baunilha & Mel", "Mirtilo Silvestre"), sabores);
        assertEquals(sabores.size() * TamanhoProduto.values().length, produtos.size());
        assertTrue(produtos.stream().allMatch(p -> p.getPrecoUnitario().signum() > 0));
        assertTrue(produtos.stream().allMatch(p -> p.getTipoEstoque() != null));
    }

    @Test
    void catalogoJaPopuladoNaoDeveSerAlterado() {
        when(repository.count()).thenReturn(8L);

        carregador.run();

        verify(repository, never()).saveAll(any());
    }
}
