package com.rodrigo.despesas.usuario;

import com.rodrigo.despesas.config.EmailProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envio dos e-mails de conta.
 *
 * Tudo aqui é @Async por dois motivos. O óbvio: ninguém deve esperar o SMTP
 * para receber a resposta HTTP. O menos óbvio, e mais importante: se o envio
 * fosse síncrono, o tempo de resposta do cadastro denunciaria se o e-mail já
 * existia ou não — a diferença entre "mandou um e-mail" e "mandou outro"
 * reabriria por baixo a enumeração que a resposta idêntica fechou.
 */
@Service
public class ServicoDeEmail {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeEmail.class);

    private final JavaMailSender remetente;
    private final EmailProperties propriedades;

    public ServicoDeEmail(JavaMailSender remetente, EmailProperties propriedades) {
        this.remetente = remetente;
        this.propriedades = propriedades;
    }

    @Async
    public void enviarConfirmacao(Usuario usuario, String token) {
        String link = propriedades.link("confirmar", token);

        enviar(usuario.getEmail(), "Confirme seu e-mail",
                """
                Olá, %s.

                Para ativar sua conta no Controle de Despesas, abra o link abaixo:

                %s

                O link vale por 24 horas e só pode ser usado uma vez.
                Se não foi você quem se cadastrou, ignore esta mensagem.
                """.formatted(usuario.getNome(), link),
                bloco("Confirme seu e-mail",
                        "Para ativar sua conta no Controle de Despesas, toque no botão abaixo.",
                        "Confirmar e-mail", link,
                        "O link vale por 24 horas e só pode ser usado uma vez. "
                                + "Se não foi você quem se cadastrou, ignore esta mensagem."));
    }

    @Async
    public void enviarRecuperacao(Usuario usuario, String token) {
        String link = propriedades.link("redefinir", token);

        enviar(usuario.getEmail(), "Redefinir sua senha",
                """
                Olá, %s.

                Pedimos a redefinição da sua senha. Se foi você, abra:

                %s

                O link vale por 1 hora e só pode ser usado uma vez.
                Se não foi você, ignore — sua senha continua a mesma.
                """.formatted(usuario.getNome(), link),
                bloco("Redefinir sua senha",
                        "Recebemos um pedido para redefinir a senha da sua conta.",
                        "Escolher nova senha", link,
                        "O link vale por 1 hora e só pode ser usado uma vez. "
                                + "Se não foi você, ignore — sua senha continua a mesma."));
    }

    /**
     * Vai para quem já tem conta e tentou se cadastrar de novo.
     *
     * É o que permite o cadastro responder igual nos dois casos: de fora, a
     * única diferença fica dentro da caixa de entrada de quem é dono do
     * endereço — exatamente onde ela não ajuda um atacante.
     */
    @Async
    public void avisarQueJaExisteConta(Usuario usuario) {
        String entrar = propriedades.link("entrar", "");

        enviar(usuario.getEmail(), "Alguém tentou criar uma conta com seu e-mail",
                """
                Olá, %s.

                Alguém tentou criar uma conta no Controle de Despesas com este e-mail,
                mas você já tem uma.

                Se foi você, é só entrar: %s
                Esqueceu a senha? Use a opção "Esqueci minha senha" na tela de entrada.

                Se não foi você, não precisa fazer nada. Ninguém teve acesso à sua conta.
                """.formatted(usuario.getNome(), entrar),
                bloco("Você já tem uma conta",
                        "Alguém tentou criar uma conta com este e-mail, mas ele já está em uso.",
                        "Ir para a entrada", entrar,
                        "Se não foi você, não precisa fazer nada — ninguém teve acesso à sua conta."));
    }

    /** Conta que só existe via Google e pediu recuperação de senha. */
    @Async
    public void avisarQueContaEhDoGoogle(Usuario usuario) {
        String entrar = propriedades.link("entrar", "");

        enviar(usuario.getEmail(), "Sua conta entra pelo Google",
                """
                Olá, %s.

                Recebemos um pedido para redefinir sua senha, mas sua conta não usa senha:
                ela entra pelo Google.

                Use o botão "Entrar com o Google" em %s
                """.formatted(usuario.getNome(), entrar),
                bloco("Sua conta entra pelo Google",
                        "Recebemos um pedido de redefinição, mas esta conta não usa senha.",
                        "Entrar com o Google", entrar,
                        "Nada foi alterado na sua conta."));
    }

    private void enviar(String destino, String assunto, String texto, String html) {
        try {
            MimeMessage mensagem = remetente.createMimeMessage();
            MimeMessageHelper ajudante = new MimeMessageHelper(mensagem, true, "UTF-8");

            ajudante.setFrom(propriedades.remetente());
            ajudante.setTo(destino);
            ajudante.setSubject(assunto);
            // Texto e HTML no mesmo envio: o cliente de e-mail escolhe, e
            // filtros de spam gostam de mensagens que têm as duas partes.
            ajudante.setText(texto, html);

            this.remetente.send(mensagem);
        } catch (MailException | jakarta.mail.MessagingException excecao) {
            // Uma falha de SMTP não pode derrubar o cadastro: a pessoa pede o
            // reenvio. O endereço fica fora do log de propósito.
            log.error("Falha ao enviar e-mail de conta (assunto: {})", assunto, excecao);
        }
    }

    /** HTML mínimo e com estilo em linha — é o que sobrevive a cliente de e-mail. */
    private static String bloco(String titulo, String texto, String rotuloDoBotao,
            String link, String rodape) {

        return """
                <div style="font-family:system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;\
                background:#eef3f8;padding:32px 16px">
                  <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:14px;\
                border:1px solid #dfe8f0;padding:32px">
                    <div style="width:44px;height:44px;border-radius:12px;\
                background:linear-gradient(135deg,#1265c6,#0aa9c7);margin-bottom:20px"></div>
                    <h1 style="margin:0 0 8px;font-size:20px;color:#0f1f2d">%s</h1>
                    <p style="margin:0 0 24px;font-size:15px;line-height:1.5;color:#64798d">%s</p>
                    <a href="%s" style="display:inline-block;padding:12px 22px;border-radius:9px;\
                background:linear-gradient(135deg,#1265c6,#0aa9c7);color:#fff;text-decoration:none;\
                font-weight:600;font-size:15px">%s</a>
                    <p style="margin:24px 0 0;font-size:13px;line-height:1.5;color:#93a8ba">%s</p>
                    <p style="margin:16px 0 0;font-size:12px;color:#93a8ba;word-break:break-all">%s</p>
                  </div>
                </div>
                """.formatted(titulo, texto, link, rotuloDoBotao, rodape, link);
    }
}
