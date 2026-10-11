package com.goatilicious.mail;

public class EmailNaoConfiguradoException extends RuntimeException {

    public EmailNaoConfiguradoException() {
        super("E-mail não configurado");
    }
}
