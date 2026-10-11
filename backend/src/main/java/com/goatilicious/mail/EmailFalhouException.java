package com.goatilicious.mail;

public class EmailFalhouException extends RuntimeException {

    public EmailFalhouException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
