package com.rodrigo.despesas.lancamento.dto;

import java.math.BigDecimal;

/**
 * Uma fatia do gráfico "para onde foi o dinheiro".
 *
 * O percentual vem pronto do servidor para que o gráfico não precise refazer
 * a conta e acabar mostrando um número diferente do que está escrito ao lado.
 */
public record FatiaDestino(String chave, String rotulo, BigDecimal valor, BigDecimal percentual) {
}
