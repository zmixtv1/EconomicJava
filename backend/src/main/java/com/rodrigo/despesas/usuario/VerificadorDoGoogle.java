package com.rodrigo.despesas.usuario;

import com.rodrigo.despesas.config.GoogleProperties;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

/**
 * Valida o ID token que o botão do Google devolve ao navegador.
 *
 * O token é um JWT assinado pelo Google; conferimos assinatura (contra as
 * chaves públicas dele), emissor, destinatário e expiração. Nada disso confia
 * no que o front diz — o front só transporta o token.
 */
@Service
public class VerificadorDoGoogle {

    private static final String CHAVES = "https://www.googleapis.com/oauth2/v3/certs";

    /** O Google emite as duas formas, dependendo do fluxo. */
    private static final Set<String> EMISSORES = Set.of("accounts.google.com", "https://accounts.google.com");

    public record DadosDoGoogle(String googleId, String email, String nome, boolean emailVerificado) {
    }

    private final GoogleProperties propriedades;
    private final NimbusJwtDecoder decodificador;

    public VerificadorDoGoogle(GoogleProperties propriedades) {
        this.propriedades = propriedades;
        this.decodificador = propriedades.habilitado() ? montarDecodificador(propriedades.clientId()) : null;
    }

    public boolean habilitado() {
        return decodificador != null;
    }

    /** Lança JwtException quando o token não presta — o controller traduz para 401. */
    public DadosDoGoogle verificar(String idToken) {
        if (decodificador == null) {
            throw new IllegalStateException("Login com Google não está configurado nesta instalação");
        }

        Jwt token = decodificador.decode(idToken);

        return new DadosDoGoogle(
                token.getSubject(),
                Usuario.normalizarEmail(token.getClaimAsString("email")),
                nomeDe(token),
                Boolean.TRUE.equals(token.getClaim("email_verified")));
    }

    private static String nomeDe(Jwt token) {
        String nome = token.getClaimAsString("name");
        if (nome != null && !nome.isBlank()) {
            return nome;
        }
        // Conta sem nome no perfil: usa o que vem antes do arroba.
        String email = token.getClaimAsString("email");
        return email == null ? "Usuário" : email.split("@")[0];
    }

    private static NimbusJwtDecoder montarDecodificador(String clientId) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(CHAVES).build();

        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                emissorConhecido(),
                destinadoAEsteApp(clientId)));

        return decodificador;
    }

    private static OAuth2TokenValidator<Jwt> emissorConhecido() {
        return token -> EMISSORES.contains(String.valueOf(token.getIssuer()))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_issuer", "Emissor inesperado no token do Google", null));
    }

    /**
     * Sem esta checagem, um token emitido pelo Google para QUALQUER outro
     * aplicativo seria aceito aqui — e qualquer dono de app poderia entrar
     * como qualquer usuário nosso.
     */
    private static OAuth2TokenValidator<Jwt> destinadoAEsteApp(String clientId) {
        return token -> {
            List<String> destinatarios = token.getAudience();
            return destinatarios != null && destinatarios.contains(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_audience", "Token do Google não é deste aplicativo", null));
        };
    }

    /** Só para mensagem de erro; nunca usado em decisão de segurança. */
    public String clientIdConfigurado() {
        return propriedades.clientId();
    }

    public static boolean tokenInvalido(RuntimeException excecao) {
        return excecao instanceof JwtException;
    }
}
