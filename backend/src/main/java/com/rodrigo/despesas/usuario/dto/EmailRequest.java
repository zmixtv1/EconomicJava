package com.rodrigo.despesas.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Usado no reenvio da confirmação e no pedido de recuperação de senha. */
public record EmailRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email) {
}
