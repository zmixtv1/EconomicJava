package com.rodrigo.despesas.usuario;

import com.rodrigo.despesas.config.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite o token de sessão.
 *
 * O `subject` é o id do usuário: é dele que sai o filtro de dono em toda
 * consulta. Como o token é assinado, o cliente não consegue trocar esse id
 * para ler os dados de outra pessoa sem invalidar a assinatura.
 */
@Service
public class TokenService {

    /** Quem assina. O decodificador exige exatamente este emissor. */
    public static final String EMISSOR = "despesas-api";

    public record Token(String valor, Instant expiraEm) {
    }

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;

    public TokenService(JwtEncoder encoder, JwtProperties propriedades) {
        this.encoder = encoder;
        this.propriedades = propriedades;
    }

    public Token emitirPara(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(propriedades.validadeMinutos(), ChronoUnit.MINUTES);

        JwtClaimsSet reivindicacoes = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(String.valueOf(usuario.getId()))
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, reivindicacoes)).getTokenValue();

        return new Token(valor, expiraEm);
    }
}
