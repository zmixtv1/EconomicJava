package com.rodrigo.despesas.lancamento;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rodrigo.despesas.common.ApiExceptionHandler;
import com.rodrigo.despesas.common.LimitadorDeTentativas;
import com.rodrigo.despesas.common.RecursoNaoEncontradoException;
import com.rodrigo.despesas.config.CorsProperties;
import com.rodrigo.despesas.config.JwtProperties;
import com.rodrigo.despesas.config.SegurancaProperties;
import com.rodrigo.despesas.config.SecurityConfig;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(LancamentoController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, LimitadorDeTentativas.class})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class, SegurancaProperties.class})
@TestPropertySource(properties = {
        "app.jwt.segredo=segredo-de-teste-com-mais-de-32-caracteres-aqui",
        "app.cors.origens=http://localhost:5173",
        "app.seguranca.tentativas-por-minuto=10000"
})
class LancamentoControllerTest {

    private static final Long DONO = 7L;

    private static RequestPostProcessor autenticado() {
        return jwt().jwt(token -> token.subject(String.valueOf(DONO)));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LancamentoService servico;

    @Test
    @DisplayName("sem token devolve 401 e nem chega no serviço")
    void semTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/v1/lancamentos"))
                .andExpect(status().isUnauthorized());

        verify(servico, never()).listar(any(), any(), any());
    }

    @Test
    @DisplayName("o id do dono sai do token, não do que o cliente manda")
    void idDoDonoSaiDoToken() throws Exception {
        mockMvc.perform(get("/api/v1/lancamentos").param("mes", "2026-08").with(autenticado()))
                .andExpect(status().isOk());

        verify(servico).listar(eq(DONO), eq(YearMonth.of(2026, 8)), eq(null));
    }

    @Test
    @DisplayName("mês inválido vira 400 com mensagem útil")
    void mesInvalidoVira400() throws Exception {
        mockMvc.perform(get("/api/v1/lancamentos").param("mes", "agosto").with(autenticado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("aaaa-MM")));
    }

    @Test
    @DisplayName("tipo fora do enum vira 400, não 500")
    void tipoInvalidoVira400() throws Exception {
        mockMvc.perform(get("/api/v1/lancamentos").param("tipo", "PIZZA").with(autenticado()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    @DisplayName("id inexistente vira 404 em ProblemDetail")
    void idInexistenteVira404() throws Exception {
        when(servico.buscar(DONO, 99L))
                .thenThrow(new RecursoNaoEncontradoException("Lançamento 99 não encontrado"));

        mockMvc.perform(get("/api/v1/lancamentos/99").with(autenticado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("campos obrigatórios ausentes viram 400 com erro por campo")
    void camposAusentesViram400() throws Exception {
        mockMvc.perform(post("/api/v1/lancamentos")
                        .with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.tipo").exists())
                .andExpect(jsonPath("$.erros.categoria").exists())
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.valor").exists());

        verify(servico, never()).criar(any(), any());
    }

    @Test
    @DisplayName("regra de negócio violada vira 400, não 500")
    void regraDeNegocioVira400() throws Exception {
        when(servico.criar(eq(DONO), any()))
                .thenThrow(new IllegalArgumentException("Despesa fixa é mensal: informe o mês de início"));

        String corpo = """
                {"tipo":"DESPESA_FIXA","categoria":"MORADIA","descricao":"Aluguel",
                 "valor":1850.00,"data":"2026-08-05"}""";

        mockMvc.perform(post("/api/v1/lancamentos")
                        .with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Despesa fixa é mensal: informe o mês de início"));
    }
}
