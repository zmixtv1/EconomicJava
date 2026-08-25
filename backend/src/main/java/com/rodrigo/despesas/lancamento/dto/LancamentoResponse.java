package com.rodrigo.despesas.lancamento.dto;

import com.rodrigo.despesas.lancamento.Categoria;
import com.rodrigo.despesas.lancamento.Lancamento;
import com.rodrigo.despesas.lancamento.TipoLancamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

public record LancamentoResponse(
        Long id,
        TipoLancamento tipo,
        String tipoRotulo,
        Categoria categoria,
        String categoriaRotulo,
        String descricao,
        BigDecimal valor,
        boolean recorrente,
        LocalDate data,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        Integer parcelas,
        /** Em que parcela está no mês consultado — "parcela 7 de 48". */
        Integer parcelaAtual,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static LancamentoResponse de(Lancamento lancamento, YearMonth mesConsultado) {
        return new LancamentoResponse(
                lancamento.getId(),
                lancamento.getTipo(),
                lancamento.getTipo().getRotulo(),
                lancamento.getCategoria(),
                lancamento.getCategoria().getRotulo(),
                lancamento.getDescricao(),
                lancamento.getValor(),
                lancamento.isRecorrente(),
                lancamento.getData(),
                lancamento.getVigenciaInicio(),
                lancamento.getVigenciaFim(),
                lancamento.getParcelas(),
                mesConsultado == null ? null : lancamento.parcelaEm(mesConsultado),
                lancamento.getCriadoEm(),
                lancamento.getAtualizadoEm());
    }
}
