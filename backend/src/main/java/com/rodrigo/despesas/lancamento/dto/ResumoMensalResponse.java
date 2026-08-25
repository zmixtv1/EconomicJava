package com.rodrigo.despesas.lancamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * O painel do mês.
 *
 * `disponivel` é o que sobra da receita depois de tudo que compromete o mês —
 * despesas fixas, variáveis, economia e dívidas. Pode ser negativo, e é
 * exatamente aí que o número serve para alguma coisa.
 */
public record ResumoMensalResponse(
        String mes,
        LocalDate inicio,
        LocalDate fim,
        BigDecimal receitas,
        BigDecimal despesasFixas,
        BigDecimal despesasVariaveis,
        BigDecimal economia,
        BigDecimal dividas,
        BigDecimal comprometido,
        BigDecimal disponivel,
        /** Fatias do gráfico de destino do dinheiro, incluindo o que sobrou. */
        List<FatiaDestino> destino,
        List<TotalPorCategoria> porCategoria) {
}
