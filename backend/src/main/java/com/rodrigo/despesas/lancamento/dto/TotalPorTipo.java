package com.rodrigo.despesas.lancamento.dto;

import com.rodrigo.despesas.lancamento.TipoLancamento;
import java.math.BigDecimal;

public record TotalPorTipo(TipoLancamento tipo, String tipoRotulo, BigDecimal total) {

    /** Usado na projeção JPQL, que não tem como preencher o rótulo sozinha. */
    public TotalPorTipo(TipoLancamento tipo, BigDecimal total) {
        this(tipo, tipo.getRotulo(), total);
    }
}
