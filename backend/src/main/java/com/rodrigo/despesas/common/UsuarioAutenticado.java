package com.rodrigo.despesas.common;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Único ponto que traduz o token no id do dono dos dados.
 *
 * O valor vem do `subject` de um JWT já validado pelo Spring Security — se a
 * assinatura não fechasse, a requisição teria parado no filtro antes de chegar
 * ao controller.
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
