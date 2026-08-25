package com.rodrigo.despesas.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Todo erro sai no formato ProblemDetail (RFC 9457), entao o front tem um unico
 * contrato de erro para tratar. Detalhe de implementacao (stack trace, SQL,
 * nome de classe) nunca vaza para a resposta.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, excecao.getMessage());
        problema.setTitle("Credenciais inválidas");
        return problema;
    }

    /**
     * 403 com um código que o front reconhece: é o que permite oferecer
     * "reenviar confirmação" em vez de só repetir a mensagem de erro.
     */
    @ExceptionHandler(EmailNaoConfirmadoException.class)
    public ProblemDetail emailNaoConfirmado(EmailNaoConfirmadoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, excecao.getMessage());
        problema.setTitle("E-mail não confirmado");
        problema.setProperty("codigo", "EMAIL_NAO_CONFIRMADO");
        return problema;
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ProblemDetail tokenInvalido(TokenInvalidoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, excecao.getMessage());
        problema.setTitle("Link expirado");
        return problema;
    }

    @ExceptionHandler(RecursoIndisponivelException.class)
    public ProblemDetail indisponivel(RecursoIndisponivelException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, excecao.getMessage());
        problema.setTitle("Recurso indisponível");
        return problema;
    }

    /** Token do Google recusado: assinatura, emissor, destinatário ou validade. */
    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtException.class)
    public ProblemDetail tokenDoGoogleInvalido(org.springframework.security.oauth2.jwt.JwtException excecao) {
        log.warn("Token do Google recusado: {}", excecao.getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Não foi possível validar sua conta do Google. Tente de novo.");
        problema.setTitle("Credenciais inválidas");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException excecao) {
        Map<String, String> erros = new LinkedHashMap<>();
        excecao.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos");
        problema.setTitle("Dados inválidos");
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
        problema.setTitle("Requisição inválida");
        return problema;
    }

    /** JSON malformado ou valor fora do enum (ex.: "categoria": "PIZZA"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoIlegivel(HttpMessageNotReadableException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "O corpo da requisição não pôde ser lido. Verifique o formato dos campos.");
        problema.setTitle("Requisição inválida");
        return problema;
    }

    /** Ex.: ?inicio=ontem em vez de uma data ISO. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "O parâmetro '%s' recebeu um valor inválido".formatted(excecao.getName()));
        problema.setTitle("Parâmetro inválido");
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception excecao) {
        log.error("Erro não tratado", excecao);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");
        problema.setTitle("Erro interno");
        return problema;
    }
}
