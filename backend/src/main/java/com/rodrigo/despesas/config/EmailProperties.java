package com.rodrigo.despesas.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * De quem os e-mails saem e para onde os links apontam.
 *
 * A URL do app é a base dos links de confirmação e de recuperação — em
 * produção é o domínio da Vercel, em desenvolvimento é o localhost do Vite.
 */
@Validated
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(

        @NotBlank(message = "app.email.remetente é obrigatório")
        String remetente,

        @NotBlank(message = "app.email.url-do-app é obrigatório")
        String urlDoApp) {

    /** Monta o link sem depender de rota no servidor: a query vem antes do hash. */
    public String link(String rota, String token) {
        return "%s/?token=%s#/%s".formatted(urlDoApp().replaceAll("/+$", ""), token, rota);
    }
}
