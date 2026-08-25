package com.rodrigo.despesas.lancamento.dto;

import com.rodrigo.despesas.lancamento.Categoria;
import com.rodrigo.despesas.lancamento.TipoLancamento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * As regras que dependem da combinação dos campos (data x vigência, categoria
 * do tipo certo, parcelas só em dívida) ficam no serviço — Bean Validation
 * cuida do que dá para conferir campo a campo.
 */
public record LancamentoRequest(

        @NotNull(message = "O tipo é obrigatório")
        TipoLancamento tipo,

        @NotNull(message = "A categoria é obrigatória")
        Categoria categoria,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres")
        String descricao,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @DecimalMax(value = "9999999999.99", message = "O valor excede o limite permitido")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais")
        BigDecimal valor,

        /** Lançamento pontual. */
        LocalDate data,

        /** Lançamento mensal: qualquer dia do mês em que começa. */
        LocalDate vigenciaInicio,

        /** Opcional. Nulo em recorrência sem previsão de fim. */
        LocalDate vigenciaFim,

        @Positive(message = "O número de parcelas deve ser maior que zero")
        @Max(value = 600, message = "O número de parcelas parece alto demais")
        Integer parcelas) {
}
