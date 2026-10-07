package com.goatilicious.models;

import com.goatilicious.enums.CondicaoPagamento;
import com.goatilicious.enums.EstadoFactura;
import com.goatilicious.enums.StatusPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fatura de um pedido: nasce RASCUNHO ao registar o pedido e passa a FINAL na revisão. */
@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Long idFactura;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;

    @NotNull
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_pagamento", nullable = false)
    private CondicaoPagamento condicaoPagamento = CondicaoPagamento.A_VISTA;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFactura estado = EstadoFactura.RASCUNHO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @PrePersist
    void definirDataEmissao() {
        if (dataEmissao == null) {
            dataEmissao = LocalDateTime.now();
        }
    }

    public static Factura rascunhoDe(Pedido pedido) {
        Factura factura = new Factura();
        factura.pedido = pedido;
        factura.valorTotal = pedido.getTotal();
        return factura;
    }

    public void finalizar(CondicaoPagamento condicao, BigDecimal novoTotal) {
        this.condicaoPagamento = condicao;
        this.valorTotal = novoTotal;
        this.estado = EstadoFactura.FINAL;
        this.dataEmissao = LocalDateTime.now();
    }

    public Long getIdFactura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }

    public Pedido getPedido() { return pedido; }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public CondicaoPagamento getCondicaoPagamento() { return condicaoPagamento; }
    public EstadoFactura getEstado() { return estado; }
    public StatusPagamento getStatusPagamento() { return statusPagamento; }
}
