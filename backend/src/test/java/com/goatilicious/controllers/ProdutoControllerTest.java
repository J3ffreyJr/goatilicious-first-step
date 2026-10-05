package com.goatilicious.controllers;

import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProdutoController.class)
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProdutoRepository repository;

    private Produto produto(Long id, String sabor, TamanhoProduto tamanho, String preco) {
        Produto p = new Produto();
        p.setIdProduto(id);
        p.setSabor(sabor);
        p.setTamanho(tamanho);
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        p.setPrecoUnitario(new BigDecimal(preco));
        return p;
    }

    @Test
    void getDeveListarOCatalogo() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                produto(1L, "Cereja", TamanhoProduto.ML_500, "350.00"),
                produto(2L, "Mirtilo Silvestre", TamanhoProduto.L_1, "650.00")));

        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sabor").value("Cereja"))
                .andExpect(jsonPath("$[0].tamanho").value("ML_500"))
                .andExpect(jsonPath("$[1].precoUnitario").value(650.0));
    }
}
