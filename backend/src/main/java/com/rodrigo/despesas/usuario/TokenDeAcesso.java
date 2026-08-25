package com.rodrigo.despesas.usuario;

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
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Token de uso único enviado por e-mail.
 *
 * O valor em claro só existe dentro do link que a pessoa recebe; aqui fica o
 * hash. Isso significa que nem eu, nem alguém com acesso ao banco, consegue
 * reconstruir um link válido.
 */
@Entity
@Table(name = "tokens_de_acesso")
public class TokenDeAcesso {

    public enum Tipo {
        CONFIRMACAO_EMAIL,
        RECUPERACAO_SENHA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Tipo tipo;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected TokenDeAcesso() {
    }

    public TokenDeAcesso(Usuario usuario, Tipo tipo, String tokenHash, Instant expiraEm) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public boolean valeAgora() {
        return usadoEm == null && Instant.now().isBefore(expiraEm);
    }

    public void marcarComoUsado() {
        this.usadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getUsadoEm() {
        return usadoEm;
    }
}
