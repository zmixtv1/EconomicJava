package com.rodrigo.despesas.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Sem JWT_SECRET a aplicação não sobe — de propósito. Um segredo padrão em
 * código seria o mesmo que não ter autenticação: qualquer pessoa com acesso ao
 * repositório poderia assinar um token válido para qualquer conta.
 *
 * Gerar: openssl rand -base64 48
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        // HS256 usa chave de 256 bits; abaixo de 32 caracteres o Nimbus recusa.
        @NotBlank(message = "app.jwt.segredo é obrigatório (defina a variável JWT_SECRET)")
        @Size(min = 32, message = "app.jwt.segredo precisa de pelo menos 32 caracteres")
        String segredo,

        @Positive(message = "app.jwt.validade-minutos deve ser positivo")
        long validadeMinutos) {
}
