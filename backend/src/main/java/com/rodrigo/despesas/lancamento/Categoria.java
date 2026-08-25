package com.rodrigo.despesas.lancamento;

import java.util.Arrays;
import java.util.List;

/**
 * Cada categoria pertence a um tipo. Isso deixa impossível cadastrar
 * "Salário" como dívida ou "Reserva de emergência" como receita: a validação
 * confere o par (tipo, categoria) antes de gravar.
 */
public enum Categoria {

    // ---------- receitas ----------
    SALARIO(TipoLancamento.RECEITA, "Salário"),
    FREELANCE(TipoLancamento.RECEITA, "Freelance"),
    RENDIMENTOS(TipoLancamento.RECEITA, "Rendimentos"),
    OUTRAS_RECEITAS(TipoLancamento.RECEITA, "Outras receitas"),

    // ---------- despesas fixas ----------
    MORADIA(TipoLancamento.DESPESA_FIXA, "Moradia"),
    CONTAS_BASICAS(TipoLancamento.DESPESA_FIXA, "Água, luz e gás"),
    INTERNET_TELEFONE(TipoLancamento.DESPESA_FIXA, "Internet e telefone"),
    ASSINATURAS(TipoLancamento.DESPESA_FIXA, "Assinaturas"),
    EDUCACAO(TipoLancamento.DESPESA_FIXA, "Educação"),
    SEGUROS(TipoLancamento.DESPESA_FIXA, "Seguros"),
    OUTRAS_FIXAS(TipoLancamento.DESPESA_FIXA, "Outras fixas"),

    // ---------- despesas variáveis ----------
    ALIMENTACAO(TipoLancamento.DESPESA_VARIAVEL, "Alimentação"),
    TRANSPORTE(TipoLancamento.DESPESA_VARIAVEL, "Transporte"),
    LAZER(TipoLancamento.DESPESA_VARIAVEL, "Lazer"),
    SAUDE(TipoLancamento.DESPESA_VARIAVEL, "Saúde"),
    COMPRAS(TipoLancamento.DESPESA_VARIAVEL, "Compras"),
    OUTRAS_VARIAVEIS(TipoLancamento.DESPESA_VARIAVEL, "Outras variáveis"),

    // ---------- economia ----------
    RESERVA_EMERGENCIA(TipoLancamento.ECONOMIA, "Reserva de emergência"),
    INVESTIMENTOS(TipoLancamento.ECONOMIA, "Investimentos"),
    OBJETIVOS(TipoLancamento.ECONOMIA, "Objetivos"),

    // ---------- dívidas ----------
    FINANCIAMENTO_VEICULO(TipoLancamento.DIVIDA, "Financiamento de veículo"),
    FINANCIAMENTO_IMOVEL(TipoLancamento.DIVIDA, "Financiamento de imóvel"),
    CARTAO_CREDITO(TipoLancamento.DIVIDA, "Cartão de crédito"),
    EMPRESTIMO(TipoLancamento.DIVIDA, "Empréstimo"),
    OUTRAS_DIVIDAS(TipoLancamento.DIVIDA, "Outras dívidas");

    private final TipoLancamento tipo;
    private final String rotulo;

    Categoria(TipoLancamento tipo, String rotulo) {
        this.tipo = tipo;
        this.rotulo = rotulo;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public static List<Categoria> doTipo(TipoLancamento tipo) {
        return Arrays.stream(values()).filter(categoria -> categoria.tipo == tipo).toList();
    }
}
