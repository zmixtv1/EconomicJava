package com.rodrigo.despesas.lancamento;

import com.rodrigo.despesas.common.UsuarioAutenticado;
import com.rodrigo.despesas.lancamento.dto.LancamentoRequest;
import com.rodrigo.despesas.lancamento.dto.LancamentoResponse;
import com.rodrigo.despesas.lancamento.dto.ResumoMensalResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lancamentos")
public class LancamentoController {

    private final LancamentoService servico;

    public LancamentoController(LancamentoService servico) {
        this.servico = servico;
    }

    /**
     * Lista o que vale no mês: os pontuais daquele mês mais os mensais cuja
     * vigência o alcança. Sem paginação de propósito — um mês de orçamento
     * pessoal cabe em uma tela, e paginar atrapalharia o total da aba.
     */
    @GetMapping
    public List<LancamentoResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) TipoLancamento tipo) {

        return servico.listar(UsuarioAutenticado.id(jwt), LancamentoService.interpretarMes(mes), tipo);
    }

    @GetMapping("/resumo")
    public ResumoMensalResponse resumo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String mes) {

        return servico.resumir(UsuarioAutenticado.id(jwt), LancamentoService.interpretarMes(mes));
    }

    @GetMapping("/{id}")
    public LancamentoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servico.buscar(UsuarioAutenticado.id(jwt), id);
    }

    @PostMapping
    public ResponseEntity<LancamentoResponse> criar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LancamentoRequest request) {

        LancamentoResponse criado = servico.criar(UsuarioAutenticado.id(jwt), request);
        return ResponseEntity
                .created(URI.create("/api/v1/lancamentos/" + criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    public LancamentoResponse atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody LancamentoRequest request) {

        return servico.atualizar(UsuarioAutenticado.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        servico.remover(UsuarioAutenticado.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
