package com.rodrigo.despesas.usuario;

import com.rodrigo.despesas.common.CredenciaisInvalidasException;
import com.rodrigo.despesas.common.EmailNaoConfirmadoException;
import com.rodrigo.despesas.common.RecursoIndisponivelException;
import com.rodrigo.despesas.common.RecursoNaoEncontradoException;
import com.rodrigo.despesas.common.TokenInvalidoException;
import com.rodrigo.despesas.usuario.VerificadorDoGoogle.DadosDoGoogle;
import com.rodrigo.despesas.usuario.dto.LoginRequest;
import com.rodrigo.despesas.usuario.dto.RegistroRequest;
import com.rodrigo.despesas.usuario.dto.SessaoResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private static final Duration VALIDADE_DA_CONFIRMACAO = Duration.ofHours(24);
    private static final Duration VALIDADE_DA_RECUPERACAO = Duration.ofHours(1);

    /**
     * As senhas que aparecem primeiro em qualquer lista de força bruta. Não é
     * uma lista completa — é o piso: barra o que seria quebrado em segundos.
     */
    private static final Set<String> SENHAS_OBVIAS = Set.of(
            "12345678", "123456789", "1234567890", "senha123", "password", "password1",
            "qwertyui", "abc12345", "11111111", "00000000", "iloveyou", "admin123",
            "brasil123", "mudar123", "teste123", "senhasenha");

    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificador;
    private final TokenService tokens;
    private final ServicoDeTokens tokensDeEmail;
    private final ServicoDeEmail email;
    private final VerificadorDoGoogle google;

    public AutenticacaoService(UsuarioRepository repositorio, PasswordEncoder codificador, TokenService tokens,
            ServicoDeTokens tokensDeEmail, ServicoDeEmail email, VerificadorDoGoogle google) {

        this.repositorio = repositorio;
        this.codificador = codificador;
        this.tokens = tokens;
        this.tokensDeEmail = tokensDeEmail;
        this.email = email;
        this.google = google;
    }

    // ---------- cadastro por e-mail ----------

    /**
     * Não devolve sessão nem diz se o e-mail já existia.
     *
     * Os dois caminhos — conta nova e conta existente — mandam um e-mail
     * diferente e respondem exatamente igual. Quem está sondando endereços não
     * aprende nada; quem é dono do endereço recebe a informação certa.
     */
    @Transactional
    public void registrar(RegistroRequest request) {
        String endereco = Usuario.normalizarEmail(request.email());
        validarForcaDaSenha(request.senha(), endereco);

        Optional<Usuario> existente = repositorio.findByEmail(endereco);
        if (existente.isPresent()) {
            email.avisarQueJaExisteConta(existente.get());
            return;
        }

        Usuario usuario = Usuario.comSenha(request.nome(), endereco, codificador.encode(request.senha()));

        try {
            usuario = repositorio.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException excecao) {
            // Duas requisições simultâneas com o mesmo e-mail: quem perde a
            // corrida esbarra no índice único. Continua respondendo igual.
            return;
        }

        enviarConfirmacao(usuario);
    }

    @Transactional
    public void reenviarConfirmacao(String enderecoInformado) {
        repositorio.findByEmail(Usuario.normalizarEmail(enderecoInformado))
                .filter(usuario -> !usuario.emailConfirmado())
                .ifPresent(this::enviarConfirmacao);
        // Sem else: quem não existe, ou já confirmou, recebe a mesma resposta.
    }

    @Transactional
    public SessaoResponse confirmarEmail(String token) {
        Usuario usuario = tokensDeEmail.consumir(token, TokenDeAcesso.Tipo.CONFIRMACAO_EMAIL)
                .orElseThrow(TokenInvalidoException::new);

        usuario.confirmarEmail();
        return montarSessao(usuario);
    }

    // ---------- entrada por senha ----------

    @Transactional(readOnly = true)
    public SessaoResponse autenticar(LoginRequest request) {
        Usuario usuario = repositorio.findByEmail(Usuario.normalizarEmail(request.email()))
                .orElseThrow(CredenciaisInvalidasException::new);

        // Conta só do Google não tem hash para comparar.
        if (!usuario.temSenha() || !codificador.matches(request.senha(), usuario.getSenhaHash())) {
            // Mesma exceção do e-mail inexistente: responder de forma diferente
            // permitiria descobrir quais e-mails têm conta no sistema.
            throw new CredenciaisInvalidasException();
        }

        // Só depois de provar que sabe a senha é que faz sentido contar que
        // falta confirmar — aqui já não há o que enumerar.
        if (!usuario.emailConfirmado()) {
            throw new EmailNaoConfirmadoException();
        }

        return montarSessao(usuario);
    }

    // ---------- recuperação de senha ----------

    @Transactional
    public void solicitarRecuperacao(String enderecoInformado) {
        repositorio.findByEmail(Usuario.normalizarEmail(enderecoInformado)).ifPresent(usuario -> {
            if (!usuario.temSenha()) {
                // Existe, mas entra pelo Google: redefinir senha não faria nada.
                email.avisarQueContaEhDoGoogle(usuario);
                return;
            }
            ServicoDeTokens.TokenEmitido emitido = tokensDeEmail.emitir(
                    usuario, TokenDeAcesso.Tipo.RECUPERACAO_SENHA, VALIDADE_DA_RECUPERACAO);
            email.enviarRecuperacao(usuario, emitido.valor());
        });
    }

    @Transactional
    public SessaoResponse redefinirSenha(String token, String novaSenha) {
        Usuario usuario = tokensDeEmail.consumir(token, TokenDeAcesso.Tipo.RECUPERACAO_SENHA)
                .orElseThrow(TokenInvalidoException::new);

        validarForcaDaSenha(novaSenha, usuario.getEmail());
        usuario.trocarSenha(codificador.encode(novaSenha));

        return montarSessao(usuario);
    }

    // ---------- entrada pelo Google ----------

    @Transactional
    public SessaoResponse entrarComGoogle(String idToken) {
        if (!google.habilitado()) {
            throw new RecursoIndisponivelException("Login com Google não está disponível nesta instalação");
        }

        DadosDoGoogle dados = google.verificar(idToken);

        if (!dados.emailVerificado()) {
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = repositorio.findByGoogleId(dados.googleId())
                .or(() -> repositorio.findByEmail(dados.email()).map(existente -> {
                    // Mesma pessoa chegando por outro caminho: vincula em vez
                    // de criar uma segunda conta com os mesmos dados.
                    existente.vincularGoogle(dados.googleId());
                    return existente;
                }))
                .orElseGet(() -> repositorio.save(
                        Usuario.doGoogle(dados.nome(), dados.email(), dados.googleId())));

        return montarSessao(usuario);
    }

    public boolean googleDisponivel() {
        return google.habilitado();
    }

    // ---------- utilidades ----------

    @Transactional(readOnly = true)
    public SessaoResponse.UsuarioResponse buscarPorId(Long id) {
        return repositorio.findById(id)
                .map(SessaoResponse.UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void enviarConfirmacao(Usuario usuario) {
        ServicoDeTokens.TokenEmitido emitido = tokensDeEmail.emitir(
                usuario, TokenDeAcesso.Tipo.CONFIRMACAO_EMAIL, VALIDADE_DA_CONFIRMACAO);
        email.enviarConfirmacao(usuario, emitido.valor());
    }

    private SessaoResponse montarSessao(Usuario usuario) {
        TokenService.Token token = tokens.emitirPara(usuario);
        return SessaoResponse.de(token.valor(), token.expiraEm(), usuario);
    }

    /**
     * O tamanho mínimo já é cobrado pelo Bean Validation. Aqui ficam as duas
     * senhas que passam nele e mesmo assim não protegem nada: a que é igual ao
     * próprio e-mail e as campeãs de lista de senha.
     */
    private static void validarForcaDaSenha(String senha, String endereco) {
        String normalizada = senha.trim().toLowerCase(Locale.ROOT);
        String antesDoArroba = endereco.split("@")[0];

        if (normalizada.equals(endereco) || normalizada.equals(antesDoArroba)) {
            throw new IllegalArgumentException("A senha não pode ser igual ao seu e-mail");
        }
        if (SENHAS_OBVIAS.contains(normalizada)) {
            throw new IllegalArgumentException("Esta senha é conhecida demais. Escolha outra.");
        }
    }
}
