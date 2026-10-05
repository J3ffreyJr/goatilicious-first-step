package com.goatilicious.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long idItem;

    // Ignorado no JSON para evitar ciclo Pedido -> Item -> Pedido.
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_produto")
    private Produto produto;

    @Min(1)
    @Column(nullable = false)
    private int quantidade;

    // Preço do produto no momento do pedido (não muda se o catálogo mudar depois).
    @NotNull
    @Column(name = "preco_aplicado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoAplicado;

    public BigDecimal getSubtotal() {
        return precoAplicado.multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getIdItem() { return idItem; }
    public void setIdItem(Long idItem) { this.idItem = idItem; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoAplicado() { return precoAplicado; }
    public void setPrecoAplicado(BigDecimal precoAplicado) { this.precoAplicado = precoAplicado; }
}
