package com.rodrigo.despesas.usuario.dto;

import jakarta.validation.constraints.NotBlank;

/** O ID token que o botão do Google entrega ao navegador. */
public record GoogleRequest(@NotBlank(message = "O token do Google é obrigatório") String credential) {
}
