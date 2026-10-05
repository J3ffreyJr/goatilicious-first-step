package com.goatilicious.config;

import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Catálogo inicial: a base H2 é em memória, por isso o catálogo é recriado a cada arranque.
 * ATENÇÃO: os preços por tamanho são valores provisórios (só o 500ml = 350 MT vem da landing page).
 */
@Component
public class CarregadorProdutos implements CommandLineRunner {

    private static final List<String> SABORES = List.of("Cereja", "Mirtilo Silvestre");

    private static final Map<TamanhoProduto, BigDecimal> PRECOS = Map.of(
            TamanhoProduto.ML_125, new BigDecimal("120.00"),
            TamanhoProduto.ML_500, new BigDecimal("350.00"),
            TamanhoProduto.L_1, new BigDecimal("650.00"),
            TamanhoProduto.L_5, new BigDecimal("2800.00"));

    private final ProdutoRepository repository;

    public CarregadorProdutos(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<Produto> catalogo = new ArrayList<>();
        for (String sabor : SABORES) {
            for (TamanhoProduto tamanho : TamanhoProduto.values()) {
                Produto p = new Produto();
                p.setSabor(sabor);
                p.setTamanho(tamanho);
                p.setPrecoUnitario(PRECOS.get(tamanho));
                p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
                catalogo.add(p);
            }
        }
        repository.saveAll(catalogo);
    }
}
