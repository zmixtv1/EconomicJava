package com.rodrigo.despesas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identificador do cliente OAuth do Google.
 *
 * Vazio desliga o login com Google — a aplicação sobe e o resto funciona
 * normalmente. Isso mantém o ambiente local rodando sem exigir que cada pessoa
 * crie um projeto no Google Cloud só para levantar o app.
 */
@ConfigurationProperties(prefix = "app.google")
public record GoogleProperties(String clientId) {

    public boolean habilitado() {
        return clientId != null && !clientId.isBlank();
    }
}
