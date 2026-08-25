package com.rodrigo.despesas.lancamento;

/**
 * O eixo principal do orçamento: para onde o dinheiro vai.
 *
 * É por este campo que sai o gráfico do destino do dinheiro e o cálculo do
 * disponível — tudo que não é receita compromete o mês.
 */
public enum TipoLancamento {

    RECEITA("Receita", false),
    DESPESA_FIXA("Despesa fixa", true),
    DESPESA_VARIAVEL("Despesa variável", true),
    ECONOMIA("Economia", true),
    DIVIDA("Dívida", true);

    private final String rotulo;

    /** Se entra na conta do que compromete a receita do mês. */
    private final boolean compromete;

    TipoLancamento(String rotulo, boolean compromete) {
        this.rotulo = rotulo;
        this.compromete = compromete;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean comprometeReceita() {
        return compromete;
    }

    /** Despesa fixa é mensal por definição; despesa variável é sempre pontual. */
    public boolean exigeRecorrencia() {
        return this == DESPESA_FIXA;
    }

    public boolean proibeRecorrencia() {
        return this == DESPESA_VARIAVEL;
    }

    /** Só dívida tem número de parcelas — as demais recorrências não têm fim previsto. */
    public boolean aceitaParcelas() {
        return this == DIVIDA;
    }
}
