package com.rodrigo.despesas.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lista fechada de origens permitidas (a URL da Vercel, em producao).
 * Nunca use "*" junto com credenciais — o navegador rejeita, e seria inseguro.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> origens) {
}
