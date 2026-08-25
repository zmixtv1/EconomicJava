package com.rodrigo.despesas.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinicaoRequest(

        @NotBlank(message = "O token é obrigatório")
        String token,

        // O BCrypt ignora bytes além de 72; aceitar mais criaria a ilusão de
        // uma senha mais forte do que a real.
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha) {
}
