package com.goatilicious.mail;

/** Porta de saída para e-mail: permite trocar/simular o SMTP nos testes. */
public interface EmailSender {

    /**
     * Envia um e-mail de texto com um PDF em anexo.
     *
     * @throws EmailNaoConfiguradoException se o SMTP não estiver configurado
     * @throws EmailFalhouException         se o envio falhar
     */
    void enviar(String para, String assunto, String texto, String nomeAnexo, byte[] anexoPdf);
}
