package com.rodrigo.despesas.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Barra o excesso de tentativas antes que elas cheguem ao banco ou ao BCrypt.
 *
 * Só atua nas rotas abertas de autenticação: o resto da API já exige token
 * válido, e ali o custo de uma tentativa inútil é desprezível.
 */
public class FiltroDeTentativas extends OncePerRequestFilter {

    private static final String PREFIXO = "/api/v1/auth/";

    private final LimitadorDeTentativas limitador;

    public FiltroDeTentativas(LimitadorDeTentativas limitador) {
        this.limitador = limitador;
    }

    /**
     * Só os POST entram na cota.
     *
     * O único GET aberto em /auth é /opcoes, que apenas diz se o botão do
     * Google existe: não toca no banco, não gasta BCrypt e não serve para
     * adivinhar nada. Limitá-lo só atrapalharia quem abre a tela de entrada em
     * mais de uma aba.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !"POST".equalsIgnoreCase(requisicao.getMethod())
                || !requisicao.getRequestURI().startsWith(PREFIXO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
            FilterChain corrente) throws ServletException, IOException {

        // Com forward-headers-strategy=framework, atrás do Caddy este já é o
        // IP real do cliente, e não o do proxy.
        if (limitador.permitir(requisicao.getRemoteAddr())) {
            corrente.doFilter(requisicao, resposta);
            return;
        }

        // O Servlet API não tem constante para 429; o status vem do Spring.
        resposta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.setHeader("Retry-After", "60");
        resposta.getWriter().write("""
                {"status":429,"title":"Tentativas demais",\
                "detail":"Muitas tentativas seguidas. Espere um minuto e tente de novo."}""");
    }
}
