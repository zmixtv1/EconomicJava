package com.rodrigo.despesas.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite e consome os tokens que viajam por e-mail.
 *
 * Três decisões que valem por si:
 *   - o valor é aleatório de 32 bytes, não um JWT: URL vaza em histórico, em
 *     Referer e em log de proxy, e um JWT nesses lugares não teria como ser
 *     revogado;
 *   - no banco fica só o SHA-256 — o link não é reconstruível a partir da tabela;
 *   - emitir um token novo invalida os anteriores do mesmo tipo.
 */
@Service
public class ServicoDeTokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    public record TokenEmitido(String valor, Instant expiraEm) {
    }

    private final TokenDeAcessoRepository repositorio;

    public ServicoDeTokens(TokenDeAcessoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public TokenEmitido emitir(Usuario usuario, TokenDeAcesso.Tipo tipo, Duration validade) {
        repositorio.invalidarAnteriores(usuario.getId(), tipo, Instant.now());

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String valor = BASE64_URL.encodeToString(bytes);

        Instant expiraEm = Instant.now().plus(validade);
        repositorio.save(new TokenDeAcesso(usuario, tipo, hash(valor), expiraEm));

        return new TokenEmitido(valor, expiraEm);
    }

    /**
     * Devolve o dono do token e o queima. Vazio quando o token não existe,
     * já foi usado, expirou, ou é de outro tipo — de fora, os quatro casos são
     * indistinguíveis, e é assim que deve ser.
     */
    @Transactional
    public Optional<Usuario> consumir(String valor, TokenDeAcesso.Tipo tipo) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }

        return repositorio.findByTokenHash(hash(valor))
                .filter(token -> token.getTipo() == tipo)
                .filter(TokenDeAcesso::valeAgora)
                .map(token -> {
                    token.marcarComoUsado();
                    return token.getUsuario();
                });
    }

    static String hash(String valor) {
        try {
            MessageDigest digestor = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digestor.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excecao) {
            // SHA-256 é obrigatório em toda JVM; se faltar, algo muito pior aconteceu.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", excecao);
        }
    }
}
