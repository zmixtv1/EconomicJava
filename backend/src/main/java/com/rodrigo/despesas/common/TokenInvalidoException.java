package com.rodrigo.despesas.common;

/**
 * Serve para link expirado, já usado, inexistente ou adulterado — de fora os
 * quatro casos são a mesma coisa, de propósito.
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("Este link não vale mais. Peça um novo.");
    }
}
