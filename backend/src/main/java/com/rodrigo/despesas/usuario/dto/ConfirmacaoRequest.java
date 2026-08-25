package com.rodrigo.despesas.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmacaoRequest(@NotBlank(message = "O token é obrigatório") String token) {
}
