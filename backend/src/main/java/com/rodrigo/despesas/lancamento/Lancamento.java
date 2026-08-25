package com.rodrigo.despesas.lancamento;

import com.rodrigo.despesas.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Um lançamento é pontual (tem data) ou recorrente (tem vigência) — nunca os dois.
 *
 * O recorrente não gera uma linha por mês: ele vale em todo mês dentro da
 * vigência. Um aluguel de 3 anos é uma linha só, e não 36.
 */
@Entity
@Table(name = "lancamentos")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categoria categoria;

    @Column(nullable = false, length = 120)
    private String descricao;

    /** Dinheiro é sempre BigDecimal: double perde centavo na soma. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    /** Preenchido só nos pontuais. */
    @Column
    private LocalDate data;

    /** Preenchidos só nos recorrentes. Fim nulo = sem data para acabar. */
    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    /** Só dívidas: total de parcelas. */
    @Column
    private Integer parcelas;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Lancamento() {
    }

    public Lancamento(Usuario usuario, TipoLancamento tipo, Categoria categoria, String descricao,
            BigDecimal valor, LocalDate data, LocalDate vigenciaInicio, LocalDate vigenciaFim, Integer parcelas) {

        this.usuario = Objects.requireNonNull(usuario, "usuario");
        alterar(tipo, categoria, descricao, valor, data, vigenciaInicio, vigenciaFim, parcelas);
    }

    /**
     * Único ponto de mudança de estado. Guarda a invariante que o banco também
     * cobra: ou data, ou vigência.
     */
    public void alterar(TipoLancamento tipo, Categoria categoria, String descricao, BigDecimal valor,
            LocalDate data, LocalDate vigenciaInicio, LocalDate vigenciaFim, Integer parcelas) {

        if ((data == null) == (vigenciaInicio == null)) {
            throw new IllegalArgumentException(
                    "O lançamento precisa ter uma data (pontual) ou uma vigência (recorrente), nunca ambos");
        }

        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.descricao = Objects.requireNonNull(descricao, "descricao").trim();
        this.valor = Objects.requireNonNull(valor, "valor").setScale(2, RoundingMode.HALF_UP);
        this.data = data;
        this.vigenciaInicio = vigenciaInicio;
        this.vigenciaFim = vigenciaFim;
        this.parcelas = data == null ? parcelas : null;
    }

    public boolean isRecorrente() {
        return data == null;
    }

    /**
     * Em qual parcela este lançamento está no mês consultado.
     * Devolve null quando não é parcelado ou quando o mês está fora da faixa.
     */
    public Integer parcelaEm(YearMonth mes) {
        if (parcelas == null || vigenciaInicio == null) {
            return null;
        }
        long numero = ChronoUnit.MONTHS.between(YearMonth.from(vigenciaInicio), mes) + 1;
        return numero >= 1 && numero <= parcelas ? (int) numero : null;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Lancamento outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Lancamento.class.hashCode();
    }
}
