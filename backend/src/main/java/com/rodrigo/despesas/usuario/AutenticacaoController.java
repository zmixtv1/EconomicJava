package com.rodrigo.despesas.usuario;

import com.rodrigo.despesas.common.UsuarioAutenticado;
import com.rodrigo.despesas.usuario.dto.ConfirmacaoRequest;
import com.rodrigo.despesas.usuario.dto.EmailRequest;
import com.rodrigo.despesas.usuario.dto.GoogleRequest;
import com.rodrigo.despesas.usuario.dto.LoginRequest;
import com.rodrigo.despesas.usuario.dto.OpcoesDeEntradaResponse;
import com.rodrigo.despesas.usuario.dto.RedefinicaoRequest;
import com.rodrigo.despesas.usuario.dto.RegistroRequest;
import com.rodrigo.despesas.usuario.dto.SessaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {

    private final AutenticacaoService servico;
    private final VerificadorDoGoogle google;

    public AutenticacaoController(AutenticacaoService servico, VerificadorDoGoogle google) {
        this.servico = servico;
        this.google = google;
    }

    /** O front usa isto para saber se desenha o botão do Google. */
    @GetMapping("/opcoes")
    public OpcoesDeEntradaResponse opcoes() {
        return new OpcoesDeEntradaResponse(google.habilitado(),
                google.habilitado() ? google.clientIdConfigurado() : null);
    }

    /**
     * Responde 202 sem corpo, exista ou não a conta.
     *
     * É essa indiferença que impede alguém de descobrir quem tem cadastro no
     * sistema testando endereços — a informação vai para a caixa de entrada do
     * dono do e-mail, não para quem fez a requisição.
     */
    @PostMapping("/registro")
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistroRequest request) {
        servico.registrar(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reenviar-confirmacao")
    public ResponseEntity<Void> reenviarConfirmacao(@Valid @RequestBody EmailRequest request) {
        servico.reenviarConfirmacao(request.email());
        return ResponseEntity.accepted().build();
    }

    /** Confirmar já entra: a pessoa acabou de provar que é dona do e-mail. */
    @PostMapping("/confirmar")
    public SessaoResponse confirmar(@Valid @RequestBody ConfirmacaoRequest request) {
        return servico.confirmarEmail(request.token());
    }

    @PostMapping("/login")
    public SessaoResponse entrar(@Valid @RequestBody LoginRequest request) {
        return servico.autenticar(request);
    }

    @PostMapping("/google")
    public SessaoResponse entrarComGoogle(@Valid @RequestBody GoogleRequest request) {
        return servico.entrarComGoogle(request.credential());
    }

    @PostMapping("/recuperar")
    public ResponseEntity<Void> recuperar(@Valid @RequestBody EmailRequest request) {
        servico.solicitarRecuperacao(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/redefinir")
    public SessaoResponse redefinir(@Valid @RequestBody RedefinicaoRequest request) {
        return servico.redefinirSenha(request.token(), request.senha());
    }

    /** Usado pelo front para saber se o token guardado ainda vale. */
    @GetMapping("/eu")
    public SessaoResponse.UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return servico.buscarPorId(UsuarioAutenticado.id(jwt));
    }
}
