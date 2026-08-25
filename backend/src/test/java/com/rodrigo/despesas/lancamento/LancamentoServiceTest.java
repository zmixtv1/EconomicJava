package com.rodrigo.despesas.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rodrigo.despesas.common.RecursoNaoEncontradoException;
import com.rodrigo.despesas.lancamento.dto.LancamentoRequest;
import com.rodrigo.despesas.lancamento.dto.LancamentoResponse;
import com.rodrigo.despesas.lancamento.dto.TotalPorTipo;
import com.rodrigo.despesas.usuario.Usuario;
import com.rodrigo.despesas.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    private static final Long DONO = 7L;
    private static final YearMonth AGOSTO = YearMonth.of(2026, 8);

    @Mock
    private LancamentoRepository repositorio;

    @Mock
    private UsuarioRepository usuarios;

    @InjectMocks
    private LancamentoService servico;

    private void esperaSalvar() {
        when(usuarios.getReferenceById(DONO)).thenReturn(Usuario.comSenha("Ana", "ana@exemplo.com", "hash"));
        when(repositorio.save(any(Lancamento.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static LancamentoRequest pontual(TipoLancamento tipo, Categoria categoria, String valor, LocalDate data) {
        return new LancamentoRequest(tipo, categoria, "Descrição", new BigDecimal(valor), data, null, null, null);
    }

    private static LancamentoRequest mensal(TipoLancamento tipo, Categoria categoria, String valor,
            LocalDate inicio, LocalDate fim, Integer parcelas) {
        return new LancamentoRequest(tipo, categoria, "Descrição", new BigDecimal(valor),
                null, inicio, fim, parcelas);
    }

    // ---------- validação do par tipo/categoria ----------

    @Test
    @DisplayName("categoria de outro tipo é recusada")
    void categoriaDeOutroTipoERecusada() {
        assertThatThrownBy(() -> servico.criar(DONO,
                pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.SALARIO, "100.00", LocalDate.of(2026, 8, 5))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence");

        verify(repositorio, never()).save(any());
    }

    // ---------- pontual x recorrente ----------

    @Test
    @DisplayName("despesa fixa sem mês de início é recusada")
    void despesaFixaSemVigenciaERecusada() {
        assertThatThrownBy(() -> servico.criar(DONO,
                pontual(TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "1850.00", LocalDate.of(2026, 8, 5))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mensal");
    }

    @Test
    @DisplayName("despesa variável com vigência é recusada")
    void despesaVariavelComVigenciaERecusada() {
        assertThatThrownBy(() -> servico.criar(DONO,
                mensal(TipoLancamento.DESPESA_VARIAVEL, Categoria.ALIMENTACAO, "80.00",
                        LocalDate.of(2026, 8, 1), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pontual");
    }

    @Test
    @DisplayName("informar data e vigência ao mesmo tempo é recusado")
    void dataEVigenciaJuntasSaoRecusadas() {
        LancamentoRequest ambos = new LancamentoRequest(TipoLancamento.RECEITA, Categoria.SALARIO,
                "Salário", new BigDecimal("5000.00"),
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1), null, null);

        assertThatThrownBy(() -> servico.criar(DONO, ambos))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parcelas só valem em dívida")
    void parcelasSoValemEmDivida() {
        assertThatThrownBy(() -> servico.criar(DONO,
                mensal(TipoLancamento.ECONOMIA, Categoria.INVESTIMENTOS, "500.00",
                        LocalDate.of(2026, 8, 1), null, 12)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dívida");
    }

    // ---------- normalização da vigência ----------

    @Test
    @DisplayName("o início da vigência é normalizado para o primeiro dia do mês")
    void inicioEhNormalizadoParaODia1() {
        esperaSalvar();

        LancamentoResponse resposta = servico.criar(DONO, mensal(
                TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "1850.00",
                LocalDate.of(2026, 3, 17), null, null));

        assertThat(resposta.vigenciaInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(resposta.vigenciaFim()).isNull();
        assertThat(resposta.recorrente()).isTrue();
    }

    @Test
    @DisplayName("o fim da vigência é normalizado para o último dia do mês")
    void fimEhNormalizadoParaOUltimoDia() {
        esperaSalvar();

        LancamentoResponse resposta = servico.criar(DONO, mensal(
                TipoLancamento.DESPESA_FIXA, Categoria.ASSINATURAS, "39.90",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 10), null));

        assertThat(resposta.vigenciaFim()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    @DisplayName("dívida com parcelas calcula sozinha o mês da última")
    void dividaComParcelasCalculaOFim() {
        esperaSalvar();

        LancamentoResponse resposta = servico.criar(DONO, mensal(
                TipoLancamento.DIVIDA, Categoria.FINANCIAMENTO_VEICULO, "980.00",
                LocalDate.of(2026, 1, 1), null, 48));

        // 48 parcelas a partir de janeiro/2026 terminam em dezembro/2029.
        assertThat(resposta.vigenciaFim()).isEqualTo(LocalDate.of(2029, 12, 31));
        assertThat(resposta.parcelas()).isEqualTo(48);
    }

    @Test
    @DisplayName("fim antes do início é recusado")
    void fimAntesDoInicioERecusado() {
        assertThatThrownBy(() -> servico.criar(DONO, mensal(
                TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "1850.00",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 3, 1), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("antes do início");
    }

    // ---------- parcela do mês ----------

    @Test
    @DisplayName("a parcela do mês é calculada a partir do início da vigência")
    void parcelaDoMesEhCalculada() {
        Lancamento divida = new Lancamento(
                Usuario.comSenha("Ana", "ana@exemplo.com", "hash"),
                TipoLancamento.DIVIDA, Categoria.FINANCIAMENTO_VEICULO, "Carro",
                new BigDecimal("980.00"), null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2029, 12, 31), 48);

        assertThat(divida.parcelaEm(YearMonth.of(2026, 1))).isEqualTo(1);
        assertThat(divida.parcelaEm(YearMonth.of(2026, 8))).isEqualTo(8);
        assertThat(divida.parcelaEm(YearMonth.of(2029, 12))).isEqualTo(48);
        assertThat(divida.parcelaEm(YearMonth.of(2030, 1))).isNull();
        assertThat(divida.parcelaEm(YearMonth.of(2025, 12))).isNull();
    }

    // ---------- resumo ----------

    @Test
    @DisplayName("disponível é a receita menos tudo que compromete o mês")
    void disponivelDescontaTudoQueCompromete() {
        when(repositorio.totalizarPorTipo(eq(DONO), any(), any())).thenReturn(List.of(
                new TotalPorTipo(TipoLancamento.RECEITA, new BigDecimal("6000.00")),
                new TotalPorTipo(TipoLancamento.DESPESA_FIXA, new BigDecimal("2500.00")),
                new TotalPorTipo(TipoLancamento.DESPESA_VARIAVEL, new BigDecimal("900.00")),
                new TotalPorTipo(TipoLancamento.ECONOMIA, new BigDecimal("800.00")),
                new TotalPorTipo(TipoLancamento.DIVIDA, new BigDecimal("980.00"))));
        when(repositorio.totalizarPorCategoria(eq(DONO), any(), any())).thenReturn(List.of());

        var resumo = servico.resumir(DONO, AGOSTO);

        assertThat(resumo.comprometido()).isEqualByComparingTo("5180.00");
        assertThat(resumo.disponivel()).isEqualByComparingTo("820.00");
    }

    @Test
    @DisplayName("gastar mais do que ganha deixa o disponível negativo")
    void disponivelPodeFicarNegativo() {
        when(repositorio.totalizarPorTipo(eq(DONO), any(), any())).thenReturn(List.of(
                new TotalPorTipo(TipoLancamento.RECEITA, new BigDecimal("3000.00")),
                new TotalPorTipo(TipoLancamento.DESPESA_FIXA, new BigDecimal("3500.00"))));
        when(repositorio.totalizarPorCategoria(eq(DONO), any(), any())).thenReturn(List.of());

        var resumo = servico.resumir(DONO, AGOSTO);

        assertThat(resumo.disponivel()).isEqualByComparingTo("-500.00");
        // O rombo aparece no número, não como fatia do gráfico.
        assertThat(resumo.destino()).noneMatch(fatia -> fatia.chave().equals("DISPONIVEL"));
    }

    @Test
    @DisplayName("as fatias do destino somam 100%")
    void fatiasSomamCem() {
        when(repositorio.totalizarPorTipo(eq(DONO), any(), any())).thenReturn(List.of(
                new TotalPorTipo(TipoLancamento.RECEITA, new BigDecimal("6000.00")),
                new TotalPorTipo(TipoLancamento.DESPESA_FIXA, new BigDecimal("3000.00")),
                new TotalPorTipo(TipoLancamento.DESPESA_VARIAVEL, new BigDecimal("1500.00")),
                new TotalPorTipo(TipoLancamento.ECONOMIA, new BigDecimal("1500.00"))));
        when(repositorio.totalizarPorCategoria(eq(DONO), any(), any())).thenReturn(List.of());

        var resumo = servico.resumir(DONO, AGOSTO);

        assertThat(resumo.destino()).hasSize(3);
        assertThat(resumo.destino().stream()
                .map(fatia -> fatia.percentual())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("mês sem nada devolve zeros e nenhuma fatia")
    void mesVazioDevolveZeros() {
        when(repositorio.totalizarPorTipo(eq(DONO), any(), any())).thenReturn(List.of());
        when(repositorio.totalizarPorCategoria(eq(DONO), any(), any())).thenReturn(List.of());

        var resumo = servico.resumir(DONO, AGOSTO);

        assertThat(resumo.receitas()).isEqualByComparingTo("0.00");
        assertThat(resumo.disponivel()).isEqualByComparingTo("0.00");
        assertThat(resumo.destino()).isEmpty();
    }

    // ---------- isolamento ----------

    @Test
    @DisplayName("lançamento de outro dono não é encontrado")
    void lancamentoDeOutroDonoNaoEEncontrado() {
        when(repositorio.findByIdAndUsuarioId(1L, DONO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscar(DONO, 1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("remover de outro dono não apaga nada")
    void removerDeOutroDonoNaoApaga() {
        when(repositorio.findByIdAndUsuarioId(9L, DONO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.remover(DONO, 9L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(repositorio, never()).delete(any());
    }

    // ---------- mês ----------

    @Test
    @DisplayName("mês inválido vira erro de requisição, não erro interno")
    void mesInvalidoViraErroDeRequisicao() {
        assertThatThrownBy(() -> LancamentoService.interpretarMes("agosto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aaaa-MM");
    }

    @Test
    @DisplayName("mês vazio significa o mês corrente")
    void mesVazioEhOCorrente() {
        assertThat(LancamentoService.interpretarMes(null)).isEqualTo(YearMonth.now());
        assertThat(LancamentoService.interpretarMes("  ")).isEqualTo(YearMonth.now());
        assertThat(LancamentoService.interpretarMes("2026-08")).isEqualTo(AGOSTO);
    }
}
