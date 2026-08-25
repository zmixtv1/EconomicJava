package com.rodrigo.despesas.usuario.dto;

/**
 * O front pergunta o que está disponível antes de desenhar a tela — assim o
 * botão do Google não aparece numa instalação onde ele não foi configurado.
 */
public record OpcoesDeEntradaResponse(boolean google, String googleClientId) {
}
