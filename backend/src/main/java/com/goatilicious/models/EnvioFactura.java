package com.goatilicious.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Histórico (RNF01): cada tentativa de envio de uma fatura por e-mail, com sucesso ou erro. */
@Entity
@Table(name = "envio_factura")
public class EnvioFactura {

    private static final int MAX_ERRO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Long idEnvio;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_factura", nullable = false)
    private Factura factura;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private boolean sucesso;

    @Column(length = MAX_ERRO)
    private String erro;

    @PrePersist
    void definirDataEnvio() {
        if (dataEnvio == null) {
            dataEnvio = LocalDateTime.now();
        }
    }

    public static EnvioFactura sucesso(Factura factura, String destinatario) {
        EnvioFactura envio = new EnvioFactura();
        envio.factura = factura;
        envio.destinatario = destinatario;
        envio.sucesso = true;
        return envio;
    }

    public static EnvioFactura falha(Factura factura, String destinatario, String erro) {
        EnvioFactura envio = new EnvioFactura();
        envio.factura = factura;
        envio.destinatario = destinatario;
        envio.sucesso = false;
        String mensagem = (erro == null || erro.isBlank()) ? "Erro desconhecido" : erro;
        envio.erro = mensagem.length() > MAX_ERRO ? mensagem.substring(0, MAX_ERRO) : mensagem;
        return envio;
    }

    public Long getIdEnvio() { return idEnvio; }
    public void setIdEnvio(Long idEnvio) { this.idEnvio = idEnvio; }

    public Factura getFactura() { return factura; }
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public String getDestinatario() { return destinatario; }
    public boolean isSucesso() { return sucesso; }
    public String getErro() { return erro; }
}
