package com.rodrigo.despesas.common;

public class EmailNaoConfirmadoException extends RuntimeException {

    public EmailNaoConfirmadoException() {
        super("Confirme seu e-mail para entrar. Enviamos um link quando você se cadastrou.");
    }
}
