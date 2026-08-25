package com.rodrigo.despesas.lancamento;

import static org.assertj.core.api.Assertions.assertThat;

import com.rodrigo.despesas.lancamento.dto.LancamentoRequest;
import com.rodrigo.despesas.lancamento.dto.LancamentoResponse;
import com.rodrigo.despesas.lancamento.dto.ResumoMensalResponse;
import com.rodrigo.despesas.usuario.ServicoDeEmail;
import com.rodrigo.despesas.usuario.UsuarioRepository;
import com.rodrigo.despesas.usuario.dto.ConfirmacaoRequest;
import com.rodrigo.despesas.usuario.dto.EmailRequest;
import com.rodrigo.despesas.usuario.dto.LoginRequest;
import com.rodrigo.despesas.usuario.dto.RedefinicaoRequest;
import com.rodrigo.despesas.usuario.dto.RegistroRequest;
import com.rodrigo.despesas.usuario.dto.SessaoResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sobe a aplicação inteira contra um Postgres real.
 *
 * O que só este teste prova: que um lançamento mensal cadastrado uma vez
 * aparece em todos os meses da vigência sem existir uma linha por mês, que a
 * parcela é contada certo, que o disponível fecha e que uma conta não alcança
 * os dados da outra.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LancamentoIT {

    private static final String SENHA_PADRAO = "senha-de-teste-1";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void configuracao(DynamicPropertyRegistry registro) {
        registro.add("app.jwt.segredo", () -> "segredo-de-teste-com-mais-de-32-caracteres-aqui");
        registro.add("app.cors.origens", () -> "http://localhost:5173");
        // Limite alto: estes testes registram várias contas em segundos.
        registro.add("app.seguranca.tentativas-por-minuto", () -> 10_000);
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private LancamentoRepository lancamentos;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private JwtEncoder encoder;

    /** E-mail mockado: os testes capturam o token do argumento, sem SMTP. */
    @MockitoBean
    private ServicoDeEmail emails;

    private Conta conta;

    @BeforeEach
    void limpar() {
        lancamentos.deleteAll();
        usuarios.deleteAll();
        conta = registrar("Ana", "ana@exemplo.com");
    }

    // ---------- recorrência ----------

    @Test
    @DisplayName("uma despesa fixa cadastrada uma vez aparece em todos os meses seguintes")
    void despesaFixaApareceEmTodosOsMeses() {
        criar(mensal(TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "Aluguel", "1850.00",
                LocalDate.of(2026, 3, 1), null, null));

        // Uma linha só no banco...
        assertThat(lancamentos.count()).isEqualTo(1);

        // ...mas presente em março, agosto e dezembro.
        for (String mes : new String[] {"2026-03", "2026-08", "2026-12", "2027-05"}) {
            assertThat(listar(mes)).as("mês %s", mes)
                    .extracting(LancamentoResponse::descricao)
                    .containsExactly("Aluguel");
        }

        // E ausente antes do início da vigência.
        assertThat(listar("2026-02")).isEmpty();
    }

    @Test
    @DisplayName("vigência com fim para de aparecer no mês seguinte ao término")
    void vigenciaComFimParaDeAparecer() {
        criar(mensal(TipoLancamento.DESPESA_FIXA, Categoria.ASSINATURAS, "Streaming", "39.90",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 1), null));

        assertThat(listar("2026-06")).hasSize(1);
        assertThat(listar("2026-07")).isEmpty();
    }

    @Test
    @DisplayName("a dívida sabe em que parcela está e quando quita")
    void dividaContaAsParcelas() {
        criar(mensal(TipoLancamento.DIVIDA, Categoria.FINANCIAMENTO_VEICULO, "Parcela do carro", "980.00",
                LocalDate.of(2026, 1, 1), null, 48));

        assertThat(listar("2026-01").get(0).parcelaAtual()).isEqualTo(1);
        assertThat(listar("2026-08").get(0).parcelaAtual()).isEqualTo(8);

        LancamentoResponse ultima = listar("2029-12").get(0);
        assertThat(ultima.parcelaAtual()).isEqualTo(48);
        assertThat(ultima.vigenciaFim()).isEqualTo(LocalDate.of(2029, 12, 31));

        // Depois da última parcela, some do mês.
        assertThat(listar("2030-01")).isEmpty();
    }

    @Test
    @DisplayName("despesa variável só aparece no mês da sua data")
    void despesaVariavelSoApareceNoMesDela() {
        criar(pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.ALIMENTACAO, "Mercado", "742.35",
                LocalDate.of(2026, 8, 8)));

        assertThat(listar("2026-08")).hasSize(1);
        assertThat(listar("2026-09")).isEmpty();
    }

    // ---------- validação ----------

    @Test
    @DisplayName("categoria de outro tipo é recusada com 400")
    void categoriaDeOutroTipoVira400() {
        ResponseEntity<String> resposta = enviar(HttpMethod.POST, "/api/v1/lancamentos",
                pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.SALARIO, "Errado", "10.00",
                        LocalDate.of(2026, 8, 1)),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("não pertence");
    }

    @Test
    @DisplayName("despesa fixa sem mês de início é recusada com 400")
    void despesaFixaSemVigenciaVira400() {
        ResponseEntity<String> resposta = enviar(HttpMethod.POST, "/api/v1/lancamentos",
                pontual(TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "Aluguel", "1850.00",
                        LocalDate.of(2026, 8, 1)),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("mês em formato inválido vira 400, não erro interno")
    void mesInvalidoVira400() {
        assertThat(enviar(HttpMethod.GET, "/api/v1/lancamentos?mes=agosto", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------- resumo e gráfico ----------

    @Test
    @DisplayName("o resumo do mês mistura mensais e pontuais e fecha o disponível")
    void resumoFechaODisponivel() {
        criar(mensal(TipoLancamento.RECEITA, Categoria.SALARIO, "Salário", "6000.00",
                LocalDate.of(2026, 1, 1), null, null));
        criar(mensal(TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "Aluguel", "1850.00",
                LocalDate.of(2026, 1, 1), null, null));
        criar(mensal(TipoLancamento.ECONOMIA, Categoria.RESERVA_EMERGENCIA, "Reserva", "800.00",
                LocalDate.of(2026, 1, 1), null, null));
        criar(mensal(TipoLancamento.DIVIDA, Categoria.FINANCIAMENTO_VEICULO, "Carro", "980.00",
                LocalDate.of(2026, 1, 1), null, 48));
        criar(pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.ALIMENTACAO, "Mercado", "742.35",
                LocalDate.of(2026, 8, 8)));
        criar(pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.TRANSPORTE, "Uber", "186.90",
                LocalDate.of(2026, 8, 12)));

        ResumoMensalResponse resumo = resumo("2026-08");

        assertThat(resumo.receitas()).isEqualByComparingTo("6000.00");
        assertThat(resumo.despesasFixas()).isEqualByComparingTo("1850.00");
        assertThat(resumo.despesasVariaveis()).isEqualByComparingTo("929.25");
        assertThat(resumo.economia()).isEqualByComparingTo("800.00");
        assertThat(resumo.dividas()).isEqualByComparingTo("980.00");
        assertThat(resumo.comprometido()).isEqualByComparingTo("4559.25");
        assertThat(resumo.disponivel()).isEqualByComparingTo("1440.75");

        // As cinco fatias do destino: quatro tipos que comprometem + o que sobrou.
        assertThat(resumo.destino()).hasSize(5);
        assertThat(resumo.destino().stream().map(f -> f.percentual())
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.0");
        assertThat(resumo.destino()).anySatisfy(fatia -> {
            assertThat(fatia.chave()).isEqualTo("DISPONIVEL");
            assertThat(fatia.valor()).isEqualByComparingTo("1440.75");
        });

        // A receita não entra na quebra por categoria: ela é a origem, não o destino.
        assertThat(resumo.porCategoria()).noneMatch(total -> total.tipo() == TipoLancamento.RECEITA);
    }

    @Test
    @DisplayName("centavos sobrevivem à soma no banco")
    void centavosSobrevivem() {
        criar(pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.ALIMENTACAO, "Café", "0.10",
                LocalDate.of(2026, 8, 1)));
        criar(pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.ALIMENTACAO, "Pão", "0.20",
                LocalDate.of(2026, 8, 1)));

        assertThat(resumo("2026-08").despesasVariaveis()).isEqualByComparingTo("0.30");
    }

    @Test
    @DisplayName("filtro por tipo devolve só aquela aba")
    void filtroPorTipo() {
        criar(mensal(TipoLancamento.RECEITA, Categoria.SALARIO, "Salário", "6000.00",
                LocalDate.of(2026, 1, 1), null, null));
        criar(mensal(TipoLancamento.DESPESA_FIXA, Categoria.MORADIA, "Aluguel", "1850.00",
                LocalDate.of(2026, 1, 1), null, null));

        assertThat(listarPorTipo("2026-08", TipoLancamento.RECEITA))
                .extracting(LancamentoResponse::descricao).containsExactly("Salário");
        assertThat(listarPorTipo("2026-08", TipoLancamento.DESPESA_FIXA))
                .extracting(LancamentoResponse::descricao).containsExactly("Aluguel");
        assertThat(listarPorTipo("2026-08", TipoLancamento.ECONOMIA)).isEmpty();
    }

    // ---------- isolamento ----------

    @Test
    @DisplayName("uma conta não alcança o lançamento da outra")
    void umaContaNaoAlcancaADaOutra() {
        Long idDaAna = criar(mensal(TipoLancamento.RECEITA, Categoria.SALARIO, "Salário da Ana", "6000.00",
                LocalDate.of(2026, 1, 1), null, null));

        Conta bruno = registrar("Bruno", "bruno@exemplo.com");

        assertThat(comConta(bruno, HttpMethod.GET, "/api/v1/lancamentos?mes=2026-08", null, String.class).getBody())
                .doesNotContain("Salário da Ana");

        assertThat(comConta(bruno, HttpMethod.GET, "/api/v1/lancamentos/" + idDaAna, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(comConta(bruno, HttpMethod.DELETE, "/api/v1/lancamentos/" + idDaAna, null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResumoMensalResponse resumoDoBruno = comConta(bruno, HttpMethod.GET,
                "/api/v1/lancamentos/resumo?mes=2026-08", null, ResumoMensalResponse.class).getBody();
        assertThat(resumoDoBruno.receitas()).isEqualByComparingTo("0.00");

        // O lançamento da Ana continua lá.
        assertThat(listarPorTipo("2026-08", TipoLancamento.RECEITA)).hasSize(1);
    }

    @Test
    @DisplayName("sem token a API responde 401")
    void semTokenResponde401() {
        assertThat(http.getForEntity("/api/v1/lancamentos", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- ciclo completo ----------

    @Test
    @DisplayName("ciclo completo: cria, lê, atualiza e remove")
    void cicloCompleto() {
        Long id = criar(pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.LAZER, "Cinema", "124.00",
                LocalDate.of(2026, 8, 15)));

        assertThat(enviar(HttpMethod.GET, "/api/v1/lancamentos/" + id, null, LancamentoResponse.class)
                .getBody().valor()).isEqualByComparingTo("124.00");

        enviar(HttpMethod.PUT, "/api/v1/lancamentos/" + id,
                pontual(TipoLancamento.DESPESA_VARIAVEL, Categoria.LAZER, "Cinema com pipoca", "168.00",
                        LocalDate.of(2026, 8, 15)),
                LancamentoResponse.class);

        assertThat(enviar(HttpMethod.GET, "/api/v1/lancamentos/" + id, null, LancamentoResponse.class)
                .getBody().descricao()).isEqualTo("Cinema com pipoca");

        assertThat(enviar(HttpMethod.DELETE, "/api/v1/lancamentos/" + id, null, Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(enviar(HttpMethod.GET, "/api/v1/lancamentos/" + id, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("um lançamento pontual pode virar mensal na edição")
    void pontualPodeVirarMensal() {
        Long id = criar(pontual(TipoLancamento.RECEITA, Categoria.FREELANCE, "Bico", "800.00",
                LocalDate.of(2026, 8, 10)));

        enviar(HttpMethod.PUT, "/api/v1/lancamentos/" + id,
                mensal(TipoLancamento.RECEITA, Categoria.SALARIO, "Virou salário", "5000.00",
                        LocalDate.of(2026, 8, 1), null, null),
                LancamentoResponse.class);

        LancamentoResponse depois = listar("2026-09").get(0);
        assertThat(depois.recorrente()).isTrue();
        assertThat(depois.data()).isNull();
        assertThat(depois.vigenciaInicio()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    // ---------- confirmação de e-mail ----------

    @Test
    @DisplayName("cadastro responde igual para e-mail novo e para e-mail já existente")
    void cadastroRespondeIgualNosDoisCasos() {
        // A conta "ana@exemplo.com" já foi criada no @BeforeEach.
        ResponseEntity<String> repetido = http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest("Impostor", "ana@exemplo.com", SENHA_PADRAO), String.class);

        ResponseEntity<String> novo = http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest("Novo", "ninguem@exemplo.com", SENHA_PADRAO), String.class);

        // Mesmo status e mesmo corpo: de fora não dá para saber qual existia.
        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(novo.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(repetido.getBody()).isEqualTo(novo.getBody());

        // A diferença vai só para a caixa de entrada de quem é dono do endereço.
        Mockito.verify(emails).avisarQueJaExisteConta(Mockito.argThat(u -> u.getEmail().equals("ana@exemplo.com")));
    }

    @Test
    @DisplayName("não dá para entrar antes de confirmar o e-mail")
    void loginAntesDeConfirmarEhBarrado() {
        http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest("Pendente", "pendente@exemplo.com", SENHA_PADRAO), Void.class);

        ResponseEntity<String> tentativa = http.postForEntity("/api/v1/auth/login",
                new LoginRequest("pendente@exemplo.com", SENHA_PADRAO), String.class);

        assertThat(tentativa.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(tentativa.getBody()).contains("EMAIL_NAO_CONFIRMADO");
    }

    @Test
    @DisplayName("o link de confirmação vale uma vez só")
    void linkDeConfirmacaoEhDeUsoUnico() {
        Mockito.reset(emails);
        http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest("Bruno", "bruno@exemplo.com", SENHA_PADRAO), Void.class);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emails).enviarConfirmacao(Mockito.any(), token.capture());

        assertThat(http.postForEntity("/api/v1/auth/confirmar",
                new ConfirmacaoRequest(token.getValue()), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Segunda vez com o mesmo link: 410.
        assertThat(http.postForEntity("/api/v1/auth/confirmar",
                new ConfirmacaoRequest(token.getValue()), String.class).getStatusCode())
                .isEqualTo(HttpStatus.GONE);
    }

    @Test
    @DisplayName("token de confirmação inventado é recusado")
    void tokenInventadoEhRecusado() {
        assertThat(http.postForEntity("/api/v1/auth/confirmar",
                new ConfirmacaoRequest("nao-existe-este-token"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.GONE);
    }

    // ---------- recuperação de senha ----------

    @Test
    @DisplayName("recuperação troca a senha, entra, e invalida a senha antiga")
    void recuperacaoTrocaASenha() {
        Mockito.reset(emails);
        http.postForEntity("/api/v1/auth/recuperar", new EmailRequest("ana@exemplo.com"), Void.class);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emails).enviarRecuperacao(Mockito.any(), token.capture());

        ResponseEntity<SessaoResponse> redefinicao = http.postForEntity("/api/v1/auth/redefinir",
                new RedefinicaoRequest(token.getValue(), "outra-senha-boa-9"), SessaoResponse.class);

        assertThat(redefinicao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(redefinicao.getBody().token()).isNotBlank();

        // A senha nova entra...
        assertThat(http.postForEntity("/api/v1/auth/login",
                new LoginRequest("ana@exemplo.com", "outra-senha-boa-9"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // ...e a antiga não entra mais.
        assertThat(http.postForEntity("/api/v1/auth/login",
                new LoginRequest("ana@exemplo.com", SENHA_PADRAO), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("pedir recuperação para e-mail inexistente responde igual e não manda nada")
    void recuperacaoDeEmailInexistenteNaoVaza() {
        Mockito.reset(emails);

        ResponseEntity<String> existente = http.postForEntity("/api/v1/auth/recuperar",
                new EmailRequest("ana@exemplo.com"), String.class);
        ResponseEntity<String> inexistente = http.postForEntity("/api/v1/auth/recuperar",
                new EmailRequest("ninguem@exemplo.com"), String.class);

        assertThat(existente.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(existente.getBody()).isEqualTo(inexistente.getBody());

        // Só um e-mail saiu: o do endereço que realmente tem conta.
        Mockito.verify(emails, Mockito.times(1)).enviarRecuperacao(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("o link de recuperação vale uma vez só")
    void linkDeRecuperacaoEhDeUsoUnico() {
        Mockito.reset(emails);
        http.postForEntity("/api/v1/auth/recuperar", new EmailRequest("ana@exemplo.com"), Void.class);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emails).enviarRecuperacao(Mockito.any(), token.capture());

        http.postForEntity("/api/v1/auth/redefinir",
                new RedefinicaoRequest(token.getValue(), "primeira-troca-1"), String.class);

        assertThat(http.postForEntity("/api/v1/auth/redefinir",
                new RedefinicaoRequest(token.getValue(), "segunda-troca-2"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.GONE);
    }

    @Test
    @DisplayName("pedir um token novo invalida o anterior")
    void tokenNovoInvalidaOAnterior() {
        Mockito.reset(emails);
        http.postForEntity("/api/v1/auth/recuperar", new EmailRequest("ana@exemplo.com"), Void.class);
        http.postForEntity("/api/v1/auth/recuperar", new EmailRequest("ana@exemplo.com"), Void.class);

        ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emails, Mockito.times(2)).enviarRecuperacao(Mockito.any(), tokens.capture());

        String primeiro = tokens.getAllValues().get(0);
        String segundo = tokens.getAllValues().get(1);

        // O primeiro link já não vale; o segundo sim.
        assertThat(http.postForEntity("/api/v1/auth/redefinir",
                new RedefinicaoRequest(primeiro, "nova-senha-boa-1"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.GONE);
        assertThat(http.postForEntity("/api/v1/auth/redefinir",
                new RedefinicaoRequest(segundo, "nova-senha-boa-1"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // ---------- defesas ----------

    @Test
    @DisplayName("senha igual ao e-mail é recusada, mesmo com tamanho suficiente")
    void senhaIgualAoEmailERecusada() {
        ResponseEntity<String> resposta = http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest("Alvo", "alvo@exemplo.com", "alvo@exemplo.com"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("igual ao seu e-mail");
    }

    @Test
    @DisplayName("senha campeã de lista de força bruta é recusada")
    void senhaObviaERecusada() {
        ResponseEntity<String> resposta = http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest("Alvo", "outro@exemplo.com", "senha123"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).contains("conhecida demais");
    }

    @Test
    @DisplayName("token assinado com a chave certa mas de outro emissor é recusado")
    void tokenDeOutroEmissorEhRecusado() {
        Instant agora = Instant.now();
        JwtClaimsSet reivindicacoes = JwtClaimsSet.builder()
                .issuer("outro-sistema")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .subject("1")
                .build();

        String forjado = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), reivindicacoes)).getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(forjado);

        ResponseEntity<String> resposta = http.exchange(
                "/api/v1/lancamentos", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- utilidades ----------

    private record Conta(HttpHeaders headers) {
    }

    /**
     * Cadastra e confirma, porque agora o cadastro sozinho não dá sessão.
     *
     * O envio de e-mail é mockado e o token é capturado do argumento — assim o
     * teste percorre o fluxo real (gerar token, confirmar, receber sessão) sem
     * depender de SMTP.
     */
    private Conta registrar(String nome, String endereco) {
        Mockito.reset(emails);

        ResponseEntity<Void> cadastro = http.postForEntity("/api/v1/auth/registro",
                new RegistroRequest(nome, endereco, SENHA_PADRAO), Void.class);
        assertThat(cadastro.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emails).enviarConfirmacao(Mockito.any(), token.capture());

        SessaoResponse sessao = http.postForEntity("/api/v1/auth/confirmar",
                new ConfirmacaoRequest(token.getValue()), SessaoResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sessao.token());
        return new Conta(headers);
    }

    private static LancamentoRequest pontual(TipoLancamento tipo, Categoria categoria,
            String descricao, String valor, LocalDate data) {
        return new LancamentoRequest(tipo, categoria, descricao, new BigDecimal(valor), data, null, null, null);
    }

    private static LancamentoRequest mensal(TipoLancamento tipo, Categoria categoria, String descricao,
            String valor, LocalDate inicio, LocalDate fim, Integer parcelas) {
        return new LancamentoRequest(tipo, categoria, descricao, new BigDecimal(valor),
                null, inicio, fim, parcelas);
    }

    private Long criar(LancamentoRequest request) {
        ResponseEntity<LancamentoResponse> resposta =
                enviar(HttpMethod.POST, "/api/v1/lancamentos", request, LancamentoResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resposta.getBody().id();
    }

    private List<LancamentoResponse> listar(String mes) {
        return http.exchange("/api/v1/lancamentos?mes=" + mes, HttpMethod.GET,
                new HttpEntity<>(conta.headers()),
                new ParameterizedTypeReference<List<LancamentoResponse>>() { }).getBody();
    }

    private List<LancamentoResponse> listarPorTipo(String mes, TipoLancamento tipo) {
        return http.exchange("/api/v1/lancamentos?mes=" + mes + "&tipo=" + tipo, HttpMethod.GET,
                new HttpEntity<>(conta.headers()),
                new ParameterizedTypeReference<List<LancamentoResponse>>() { }).getBody();
    }

    private ResumoMensalResponse resumo(String mes) {
        return enviar(HttpMethod.GET, "/api/v1/lancamentos/resumo?mes=" + mes, null,
                ResumoMensalResponse.class).getBody();
    }

    private <T> ResponseEntity<T> enviar(HttpMethod metodo, String caminho, Object corpo, Class<T> tipo) {
        return comConta(conta, metodo, caminho, corpo, tipo);
    }

    private <T> ResponseEntity<T> comConta(Conta usada, HttpMethod metodo, String caminho,
            Object corpo, Class<T> tipo) {
        return http.exchange(caminho, metodo, new HttpEntity<>(corpo, usada.headers()), tipo);
    }
}
