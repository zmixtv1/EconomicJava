package com.rodrigo.despesas.lancamento;

import com.rodrigo.despesas.common.RecursoNaoEncontradoException;
import com.rodrigo.despesas.lancamento.dto.FatiaDestino;
import com.rodrigo.despesas.lancamento.dto.LancamentoRequest;
import com.rodrigo.despesas.lancamento.dto.LancamentoResponse;
import com.rodrigo.despesas.lancamento.dto.ResumoMensalResponse;
import com.rodrigo.despesas.lancamento.dto.TotalPorCategoria;
import com.rodrigo.despesas.lancamento.dto.TotalPorTipo;
import com.rodrigo.despesas.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LancamentoService {

    private final LancamentoRepository repositorio;
    private final UsuarioRepository usuarios;

    public LancamentoService(LancamentoRepository repositorio, UsuarioRepository usuarios) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
    }

    /** Aceita "2026-08"; vazio ou nulo significa o mês corrente. */
    public static YearMonth interpretarMes(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes);
        } catch (DateTimeParseException excecao) {
            throw new IllegalArgumentException("Mês inválido. Use o formato aaaa-MM, por exemplo 2026-08");
        }
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponse> listar(Long usuarioId, YearMonth mes, TipoLancamento tipo) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        List<Lancamento> encontrados = tipo == null
                ? repositorio.doMes(usuarioId, inicio, fim)
                : repositorio.doMesPorTipo(usuarioId, inicio, fim, tipo);

        return encontrados.stream().map(lancamento -> LancamentoResponse.de(lancamento, mes)).toList();
    }

    @Transactional(readOnly = true)
    public LancamentoResponse buscar(Long usuarioId, Long id) {
        return LancamentoResponse.de(buscarEntidade(usuarioId, id), null);
    }

    @Transactional
    public LancamentoResponse criar(Long usuarioId, LancamentoRequest request) {
        Periodo periodo = validarEnormalizar(request);

        Lancamento lancamento = new Lancamento(
                usuarios.getReferenceById(usuarioId),
                request.tipo(), request.categoria(), request.descricao(), request.valor(),
                periodo.data(), periodo.inicio(), periodo.fim(), periodo.parcelas());

        return LancamentoResponse.de(repositorio.save(lancamento), null);
    }

    @Transactional
    public LancamentoResponse atualizar(Long usuarioId, Long id, LancamentoRequest request) {
        Periodo periodo = validarEnormalizar(request);
        Lancamento lancamento = buscarEntidade(usuarioId, id);

        lancamento.alterar(request.tipo(), request.categoria(), request.descricao(), request.valor(),
                periodo.data(), periodo.inicio(), periodo.fim(), periodo.parcelas());

        return LancamentoResponse.de(lancamento, null);
    }

    @Transactional
    public void remover(Long usuarioId, Long id) {
        repositorio.delete(buscarEntidade(usuarioId, id));
    }

    // ---------- resumo do mês ----------

    @Transactional(readOnly = true)
    public ResumoMensalResponse resumir(Long usuarioId, YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        Map<TipoLancamento, BigDecimal> porTipo = new EnumMap<>(TipoLancamento.class);
        for (TotalPorTipo total : repositorio.totalizarPorTipo(usuarioId, inicio, fim)) {
            porTipo.put(total.tipo(), total.total());
        }

        BigDecimal receitas = valor(porTipo, TipoLancamento.RECEITA);
        BigDecimal fixas = valor(porTipo, TipoLancamento.DESPESA_FIXA);
        BigDecimal variaveis = valor(porTipo, TipoLancamento.DESPESA_VARIAVEL);
        BigDecimal economia = valor(porTipo, TipoLancamento.ECONOMIA);
        BigDecimal dividas = valor(porTipo, TipoLancamento.DIVIDA);

        BigDecimal comprometido = fixas.add(variaveis).add(economia).add(dividas);
        BigDecimal disponivel = receitas.subtract(comprometido);

        List<TotalPorCategoria> porCategoria = repositorio.totalizarPorCategoria(usuarioId, inicio, fim).stream()
                .filter(total -> total.tipo() != TipoLancamento.RECEITA)
                .toList();

        return new ResumoMensalResponse(
                mes.toString(), inicio, fim,
                receitas, fixas, variaveis, economia, dividas,
                comprometido, disponivel,
                montarDestino(fixas, variaveis, economia, dividas, disponivel),
                porCategoria);
    }

    /**
     * As fatias do gráfico. O que sobrou entra como uma fatia própria: sem ela
     * o gráfico mostraria a divisão dos gastos, e não o destino da receita.
     * Sobra negativa não vira fatia — o rombo aparece no número, não na pizza.
     */
    private static List<FatiaDestino> montarDestino(BigDecimal fixas, BigDecimal variaveis,
            BigDecimal economia, BigDecimal dividas, BigDecimal disponivel) {

        List<FatiaDestino> fatias = new ArrayList<>();
        adicionar(fatias, "DESPESA_FIXA", TipoLancamento.DESPESA_FIXA.getRotulo(), fixas);
        adicionar(fatias, "DESPESA_VARIAVEL", TipoLancamento.DESPESA_VARIAVEL.getRotulo(), variaveis);
        adicionar(fatias, "DIVIDA", TipoLancamento.DIVIDA.getRotulo(), dividas);
        adicionar(fatias, "ECONOMIA", TipoLancamento.ECONOMIA.getRotulo(), economia);
        adicionar(fatias, "DISPONIVEL", "Disponível", disponivel.max(BigDecimal.ZERO));

        BigDecimal soma = fatias.stream().map(FatiaDestino::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (soma.signum() == 0) {
            return List.of();
        }

        List<FatiaDestino> comPercentual = new ArrayList<>(fatias.stream()
                .map(fatia -> new FatiaDestino(fatia.chave(), fatia.rotulo(), fatia.valor(),
                        fatia.valor().multiply(CEM).divide(soma, 1, RoundingMode.HALF_UP)))
                .toList());

        // Arredondar cada fatia isolada faz o total dar 99,9 ou 100,1 — e um
        // gráfico que não fecha em 100% é a primeira coisa que alguém repara.
        // A sobra vai para a maior fatia, onde 0,1 ponto não muda a leitura.
        BigDecimal somaPercentual = comPercentual.stream()
                .map(FatiaDestino::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferenca = CEM.setScale(1).subtract(somaPercentual);

        if (diferenca.signum() != 0) {
            int maior = 0;
            for (int i = 1; i < comPercentual.size(); i++) {
                if (comPercentual.get(i).valor().compareTo(comPercentual.get(maior).valor()) > 0) {
                    maior = i;
                }
            }
            FatiaDestino ajustada = comPercentual.get(maior);
            comPercentual.set(maior, new FatiaDestino(ajustada.chave(), ajustada.rotulo(),
                    ajustada.valor(), ajustada.percentual().add(diferenca)));
        }

        return List.copyOf(comPercentual);
    }

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private static void adicionar(List<FatiaDestino> fatias, String chave, String rotulo, BigDecimal valor) {
        if (valor.signum() > 0) {
            fatias.add(new FatiaDestino(chave, rotulo, valor, BigDecimal.ZERO));
        }
    }

    private static BigDecimal valor(Map<TipoLancamento, BigDecimal> porTipo, TipoLancamento tipo) {
        return porTipo.getOrDefault(tipo, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- validação ----------

    private record Periodo(LocalDate data, LocalDate inicio, LocalDate fim, Integer parcelas) {
    }

    /**
     * Confere as regras que dependem da combinação dos campos e normaliza a
     * vigência para o primeiro e o último dia do mês — assim a consulta do mês
     * não precisa se preocupar com que dia a pessoa digitou.
     */
    private static Periodo validarEnormalizar(LancamentoRequest request) {
        TipoLancamento tipo = request.tipo();

        if (request.categoria().getTipo() != tipo) {
            throw new IllegalArgumentException("A categoria %s não pertence a %s"
                    .formatted(request.categoria().getRotulo(), tipo.getRotulo()));
        }

        boolean pontual = request.data() != null;
        boolean recorrente = request.vigenciaInicio() != null;

        if (pontual == recorrente) {
            throw new IllegalArgumentException(
                    "Informe uma data (lançamento pontual) ou um mês de início (lançamento mensal)");
        }
        if (tipo.exigeRecorrencia() && !recorrente) {
            throw new IllegalArgumentException("%s é mensal: informe o mês de início".formatted(tipo.getRotulo()));
        }
        if (tipo.proibeRecorrencia() && !pontual) {
            throw new IllegalArgumentException("%s é pontual: informe a data".formatted(tipo.getRotulo()));
        }
        if (request.parcelas() != null && !tipo.aceitaParcelas()) {
            throw new IllegalArgumentException("Só lançamentos do tipo Dívida têm número de parcelas");
        }

        if (pontual) {
            return new Periodo(request.data(), null, null, null);
        }

        LocalDate inicio = request.vigenciaInicio().withDayOfMonth(1);
        LocalDate fim;

        if (request.parcelas() != null) {
            // Com parcelas, o fim é consequência: não faz sentido aceitar os dois.
            fim = YearMonth.from(inicio).plusMonths(request.parcelas() - 1L).atEndOfMonth();
        } else if (request.vigenciaFim() != null) {
            fim = YearMonth.from(request.vigenciaFim()).atEndOfMonth();
            if (fim.isBefore(inicio)) {
                throw new IllegalArgumentException("O fim da vigência não pode ser antes do início");
            }
        } else {
            fim = null;
        }

        return new Periodo(null, inicio, fim, request.parcelas());
    }

    /**
     * Lançamento de outro dono responde não-encontrado, e não acesso negado:
     * dizer "existe, mas não é seu" já entregaria a informação.
     */
    private Lancamento buscarEntidade(Long usuarioId, Long id) {
        return repositorio.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento %d não encontrado".formatted(id)));
    }
}
