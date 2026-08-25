package com.rodrigo.despesas.lancamento.dto;

import com.rodrigo.despesas.lancamento.Categoria;
import com.rodrigo.despesas.lancamento.TipoLancamento;
import java.math.BigDecimal;

public record TotalPorCategoria(
        Categoria categoria,
        String categoriaRotulo,
        TipoLancamento tipo,
        BigDecimal total) {

    public TotalPorCategoria(Categoria categoria, BigDecimal total) {
        this(categoria, categoria.getRotulo(), categoria.getTipo(), total);
    }
}
