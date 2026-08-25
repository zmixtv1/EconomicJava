package com.rodrigo.despesas.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ajustes de defesa das rotas abertas.
 *
 * O limite é por IP e por minuto, e vale para /api/v1/auth/** — as duas únicas
 * rotas que respondem sem token e, por isso, as únicas que um atacante pode
 * martelar de graça.
 */
@Validated
@ConfigurationProperties(prefix = "app.seguranca")
public record SegurancaProperties(

        @Positive(message = "app.seguranca.tentativas-por-minuto deve ser positivo")
        int tentativasPorMinuto) {
}
