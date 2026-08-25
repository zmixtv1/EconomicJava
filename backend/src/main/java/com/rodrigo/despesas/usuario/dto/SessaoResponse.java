package com.rodrigo.despesas.usuario.dto;

import com.rodrigo.despesas.usuario.Usuario;
import java.time.Instant;

/**
 * O que o front recebe ao entrar. Não existe campo de senha aqui, nem hash —
 * essa informação nunca sai do servidor.
 */
public record SessaoResponse(
        String token,
        Instant expiraEm,
        UsuarioResponse usuario) {

    public record UsuarioResponse(Long id, String nome, String email) {

        public static UsuarioResponse de(Usuario usuario) {
            return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
        }
    }

    public static SessaoResponse de(String token, Instant expiraEm, Usuario usuario) {
        return new SessaoResponse(token, expiraEm, UsuarioResponse.de(usuario));
    }
}
