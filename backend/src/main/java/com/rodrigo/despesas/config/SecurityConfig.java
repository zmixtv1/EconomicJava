package com.rodrigo.despesas.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rodrigo.despesas.common.FiltroDeTentativas;
import com.rodrigo.despesas.common.LimitadorDeTentativas;
import com.rodrigo.despesas.usuario.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Autenticação por JWT (HS256), sem sessão no servidor.
 *
 * Só duas rotas ficam abertas: criar conta e entrar. Todo o resto exige um
 * token válido, e o id do dono sai do próprio token — o cliente não tem como
 * informar "de quem" são os dados que está pedindo.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Custo do BCrypt. Cada ponto dobra o tempo de verificação: 12 mantém o
     * login em milissegundos para quem sabe a senha e multiplica por quatro o
     * custo de quem está testando senhas em massa.
     */
    private static final int CUSTO_BCRYPT = 12;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
            JwtDecoder jwtDecoder, LimitadorDeTentativas limitador) throws Exception {

        http
                // Antes de tudo: quem está martelando as rotas abertas leva 429
                // sem chegar ao banco nem ao BCrypt.
                .addFilterBefore(new FiltroDeTentativas(limitador), DisableEncodeUrlFilter.class)
                // Sem cookie de sessão não existe CSRF: o navegador não anexa o
                // token sozinho, o JavaScript é que precisa montar o cabeçalho.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        // Todo POST de /auth é entrada: cadastro, login, Google,
                        // confirmação, recuperação e redefinição. O único GET
                        // aberto é o que diz quais opções existem; /auth/eu
                        // continua exigindo token.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/opcoes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(this::responderNaoAutenticado)
                        .accessDeniedHandler(this::responderSemPermissao));

        return http.build();
    }

    /**
     * 401 em JSON e SEM o cabeçalho WWW-Authenticate. Com ele, o navegador
     * abriria o popup nativo de login por cima do React.
     */
    private void responderNaoAutenticado(
            jakarta.servlet.http.HttpServletRequest requisicao,
            HttpServletResponse resposta,
            org.springframework.security.core.AuthenticationException excecao) throws java.io.IOException {

        escrever(resposta, HttpServletResponse.SC_UNAUTHORIZED,
                """
                {"status":401,"title":"Não autenticado","detail":"Faça login para continuar"}""");
    }

    private void responderSemPermissao(
            jakarta.servlet.http.HttpServletRequest requisicao,
            HttpServletResponse resposta,
            org.springframework.security.access.AccessDeniedException excecao) throws java.io.IOException {

        escrever(resposta, HttpServletResponse.SC_FORBIDDEN,
                """
                {"status":403,"title":"Acesso negado","detail":"Você não tem permissão para este recurso"}""");
    }

    private static void escrever(HttpServletResponse resposta, int status, String corpo) throws java.io.IOException {
        resposta.setStatus(status);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(corpo);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(CUSTO_BCRYPT);
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties propriedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                // Algoritmo fixo: fecha a porta para confusão de algoritmo.
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Além de expiração e "não antes de", exige o emissor certo — um token
        // assinado com esta chave para outro propósito não vale aqui.
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtIssuerValidator(TokenService.EMISSOR)));

        return decodificador;
    }

    private static SecretKey chave(JwtProperties propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties propriedades) {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(propriedades.origens());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
