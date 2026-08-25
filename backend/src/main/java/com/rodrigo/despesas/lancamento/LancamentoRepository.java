package com.rodrigo.despesas.lancamento;

import com.rodrigo.despesas.lancamento.dto.TotalPorCategoria;
import com.rodrigo.despesas.lancamento.dto.TotalPorTipo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * O predicado do mês aparece três vezes (lista, total por tipo, total por
 * categoria) e é o coração da recorrência: um lançamento entra no mês se for
 * pontual com data dentro dele, ou recorrente com vigência que o alcança.
 *
 * Como no resto do sistema, nenhuma assinatura dispensa o id do dono.
 */
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    String PREDICADO_DO_MES = """
            l.usuario.id = :usuarioId
            and (
                  (l.data is not null and l.data between :inicio and :fim)
               or (l.data is null
                   and l.vigenciaInicio <= :fim
                   and (l.vigenciaFim is null or l.vigenciaFim >= :inicio))
            )
            """;

    Optional<Lancamento> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Data descendente no Postgres traz NULLS FIRST: os recorrentes ficam no topo. */
    @Query("select l from Lancamento l where " + PREDICADO_DO_MES + " order by l.data desc, l.descricao asc")
    List<Lancamento> doMes(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("select l from Lancamento l where " + PREDICADO_DO_MES
            + " and l.tipo = :tipo order by l.data desc, l.descricao asc")
    List<Lancamento> doMesPorTipo(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("tipo") TipoLancamento tipo);

    // Concatenação com espaços explícitos: em text block o Java remove o espaço
    // no fim da linha, e "where " + "l.usuario..." viraria "wherel.usuario...".
    @Query("select new com.rodrigo.despesas.lancamento.dto.TotalPorTipo(l.tipo, sum(l.valor))"
            + " from Lancamento l where " + PREDICADO_DO_MES + " group by l.tipo")
    List<TotalPorTipo> totalizarPorTipo(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("select new com.rodrigo.despesas.lancamento.dto.TotalPorCategoria(l.categoria, sum(l.valor))"
            + " from Lancamento l where " + PREDICADO_DO_MES
            + " group by l.categoria order by sum(l.valor) desc")
    List<TotalPorCategoria> totalizarPorCategoria(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}
