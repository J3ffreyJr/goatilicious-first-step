package com.goatilicious.models;

import java.math.BigDecimal;

import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "produto", uniqueConstraints = @UniqueConstraint(columnNames = {"sabor", "tamanho"}))
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long idProduto;

    @NotBlank
    @Column(nullable = false)
    private String sabor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TamanhoProduto tamanho;

    @NotNull
    @Positive
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_estoque", nullable = false)
    private TipoEstoque tipoEstoque;

    public Long getIdProduto() { return idProduto; }
    public void setIdProduto(Long idProduto) { this.idProduto = idProduto; }

    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }

    public TamanhoProduto getTamanho() { return tamanho; }
    public void setTamanho(TamanhoProduto tamanho) { this.tamanho = tamanho; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public TipoEstoque getTipoEstoque() { return tipoEstoque; }
    public void setTipoEstoque(TipoEstoque tipoEstoque) { this.tipoEstoque = tipoEstoque; }
}
