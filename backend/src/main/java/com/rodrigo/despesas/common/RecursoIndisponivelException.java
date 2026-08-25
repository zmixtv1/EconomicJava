package com.rodrigo.despesas.common;

public class RecursoIndisponivelException extends RuntimeException {

    public RecursoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
