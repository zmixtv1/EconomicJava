package com.rodrigo.despesas.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Uma conta pode ter senha, vínculo com o Google, ou os dois — e o e-mail é o
 * que amarra tudo. Quem se cadastrou com senha e depois entra pelo Google cai
 * na mesma conta, porque o Google prova a posse daquele e-mail.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 180)
    private String email;

    /** Nulo em quem só entra pelo Google. */
    @Column(name = "senha_hash", length = 72)
    private String senhaHash;

    /** O `sub` do token do Google. Nulo em quem só usa senha. */
    @Column(name = "google_id", length = 64)
    private String googleId;

    /** Nulo enquanto a pessoa não clicou no link de confirmação. */
    @Column(name = "email_confirmado_em")
    private Instant emailConfirmadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
    }

    private Usuario(String nome, String email) {
        this.nome = Objects.requireNonNull(nome, "nome").trim();
        this.email = normalizarEmail(email);
    }

    /**
     * Cadastro por e-mail: nasce sem confirmação e não entra até confirmar.
     * Recebe o hash pronto, nunca a senha em texto puro.
     */
    public static Usuario comSenha(String nome, String email, String senhaHash) {
        Usuario usuario = new Usuario(nome, email);
        usuario.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash");
        return usuario;
    }

    /**
     * Cadastro pelo Google: já nasce confirmado, porque o Google só emite o
     * token depois de verificar o e-mail (e a gente confere essa marca).
     */
    public static Usuario doGoogle(String nome, String email, String googleId) {
        Usuario usuario = new Usuario(nome, email);
        usuario.googleId = Objects.requireNonNull(googleId, "googleId");
        usuario.emailConfirmadoEm = Instant.now();
        return usuario;
    }

    /** Login não pode depender de maiúsculas: Rodrigo@x.com é a mesma conta que rodrigo@x.com. */
    public static String normalizarEmail(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Vincula o Google a uma conta que já existia por senha.
     *
     * Confirma o e-mail de quebra: se a pessoa chegou aqui com um token do
     * Google para este endereço, a posse está provada — não faz sentido
     * continuar pedindo que ela clique num link.
     */
    public void vincularGoogle(String googleId) {
        this.googleId = Objects.requireNonNull(googleId, "googleId");
        if (emailConfirmadoEm == null) {
            emailConfirmadoEm = Instant.now();
        }
    }

    public void confirmarEmail() {
        if (emailConfirmadoEm == null) {
            emailConfirmadoEm = Instant.now();
        }
    }

    public void trocarSenha(String novoHash) {
        this.senhaHash = Objects.requireNonNull(novoHash, "novoHash");
        // Quem provou acesso ao e-mail para redefinir a senha confirmou o e-mail.
        confirmarEmail();
    }

    public boolean emailConfirmado() {
        return emailConfirmadoEm != null;
    }

    public boolean temSenha() {
        return senhaHash != null;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getGoogleId() {
        return googleId;
    }

    public Instant getEmailConfirmadoEm() {
        return emailConfirmadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Usuario outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }

    /** toString sem o hash: evita que a credencial vaze em log por acidente. */
    @Override
    public String toString() {
        return "Usuario[id=%d, email=%s]".formatted(id, email);
    }
}
