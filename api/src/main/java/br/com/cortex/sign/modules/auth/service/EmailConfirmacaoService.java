package br.com.cortex.sign.modules.auth.service;

import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.modules.auth.dto.response.ConfirmacaoEmailSolicitadaResponse;
import br.com.cortex.sign.modules.auth.dto.response.ConfirmarEmailResponse;
import br.com.cortex.sign.modules.auth.entity.TokenConfirmacaoEmail;
import br.com.cortex.sign.modules.auth.repository.TokenConfirmacaoEmailRepository;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class EmailConfirmacaoService {

    private static final int TOKEN_BYTES = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATADOR_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final TokenConfirmacaoEmailRepository tokenConfirmacaoEmailRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.notificacao.email.from:}")
    private String emailOrigem;

    @Value("${app.notificacao.email.from-name:Xsign}")
    private String nomeOrigem;

    @Value("${app.notificacao.email.reply-to:}")
    private String emailResposta;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.email-verificacao.expiration-minutes:60}")
    private long expiracaoMinutos;

    @Transactional
    public ConfirmacaoEmailSolicitadaResponse solicitar(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            return new ConfirmacaoEmailSolicitadaResponse(
                    usuario.getEmail(),
                    "Este e-mail já está verificado",
                    null
            );
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        validarConfiguracao(mailSender, usuario);

        LocalDateTime agora = LocalDateTime.now();
        tokenConfirmacaoEmailRepository.findAllByUsuarioIdAndUsadoEmIsNull(usuario.getId())
                .forEach(token -> token.setUsadoEm(agora));

        String token = gerarTokenSeguro();
        LocalDateTime expiraEm = agora.plusMinutes(expiracaoMinutos);

        TokenConfirmacaoEmail tokenConfirmacao = new TokenConfirmacaoEmail();
        tokenConfirmacao.setUsuario(usuario);
        tokenConfirmacao.setTokenHash(gerarHash(token));
        tokenConfirmacao.setExpiraEm(expiraEm);
        tokenConfirmacaoEmailRepository.saveAndFlush(tokenConfirmacao);

        enviarEmail(mailSender, usuario, token, expiraEm);

        return new ConfirmacaoEmailSolicitadaResponse(
                usuario.getEmail(),
                "Enviamos um link de confirmação para o seu e-mail",
                expiraEm
        );
    }

    @Transactional
    public ConfirmarEmailResponse confirmar(String token) {
        TokenConfirmacaoEmail tokenConfirmacao = tokenConfirmacaoEmailRepository.findByTokenHash(gerarHash(token))
                .orElseThrow(() -> new ConflitoException("Link de confirmação inválido"));

        if (!tokenConfirmacao.estaAtivo()) {
            throw new ConflitoException("Link de confirmação expirado ou já utilizado");
        }

        Usuario usuario = tokenConfirmacao.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }

        tokenConfirmacao.setUsadoEm(LocalDateTime.now());
        usuario.setEmailVerificado(true);

        return new ConfirmarEmailResponse(
                true,
                "E-mail confirmado com sucesso"
        );
    }

    private void validarConfiguracao(JavaMailSender mailSender, Usuario usuario) {
        if (mailSender == null || emailOrigem == null || emailOrigem.isBlank()) {
            throw new ConflitoException("Envio de confirmação por e-mail ainda não está configurado");
        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new ConflitoException("Usuário sem e-mail cadastrado para confirmação");
        }
    }

    private void enviarEmail(JavaMailSender mailSender, Usuario usuario, String token, LocalDateTime expiraEm) {
        MimeMessage mensagem = mailSender.createMimeMessage();
        String linkConfirmacao = criarLinkConfirmacao(token);

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensagem,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(emailOrigem, nomeOrigem);
            helper.setTo(usuario.getEmail());
            if (emailResposta != null && !emailResposta.isBlank()) {
                helper.setReplyTo(emailResposta);
            }
            helper.setSubject("Confirme seu e-mail no Xsign");
            helper.setText(criarTextoSimples(usuario, linkConfirmacao, expiraEm), criarHtml(usuario, linkConfirmacao, expiraEm));

            mailSender.send(mensagem);
        } catch (MessagingException | MailException exception) {
            throw new ConflitoException("Não foi possível enviar a confirmação por e-mail. Tente novamente em instantes.");
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new ConflitoException("Remetente de e-mail configurado incorretamente");
        }
    }

    private String criarTextoSimples(Usuario usuario, String linkConfirmacao, LocalDateTime expiraEm) {
        return """
                Olá, %s.

                Confirme seu e-mail no Xsign acessando o link abaixo:
                %s

                Este link expira em %s.

                Se você não solicitou esta confirmação, ignore esta mensagem.
                """.formatted(
                usuario.getNome(),
                linkConfirmacao,
                expiraEm.format(FORMATADOR_DATA)
        );
    }

    private String criarHtml(Usuario usuario, String linkConfirmacao, LocalDateTime expiraEm) {
        String nome = escaparHtml(usuario.getNome());
        String link = escaparHtml(linkConfirmacao);
        String validade = expiraEm.format(FORMATADOR_DATA);

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Confirme seu e-mail no Xsign</title>
                </head>
                <body style="margin:0;background:#f4f8fc;font-family:Inter,Arial,sans-serif;color:#0b2345;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f8fc;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #dce4ee;border-radius:18px;overflow:hidden;box-shadow:0 12px 34px rgba(2,38,85,.08);">
                          <tr>
                            <td style="padding:28px 32px 18px;border-bottom:1px solid #e7edf5;">
                              <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#087ed1;">Xsign</div>
                              <h1 style="margin:12px 0 8px;font-size:24px;line-height:1.25;color:#082345;">Confirme seu e-mail</h1>
                              <p style="margin:0;font-size:15px;line-height:1.6;color:#607089;">Olá, %s. Confirme este endereço para proteger seu acesso e habilitar comunicações importantes da sua conta.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 32px;">
                              <a href="%s" style="display:block;border-radius:12px;background:#087ed1;padding:14px 18px;text-align:center;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">Confirmar meu e-mail</a>
                              <p style="margin:22px 0 0;font-size:13px;line-height:1.6;color:#607089;">Este link expira em <strong style="color:#0b2345;">%s</strong>.</p>
                              <p style="margin:12px 0 0;font-size:12px;line-height:1.6;color:#607089;">Se o botão não abrir, copie e cole este endereço no navegador:<br><span style="word-break:break-all;color:#087ed1;">%s</span></p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 32px;background:#f8fbff;border-top:1px solid #e7edf5;font-size:12px;line-height:1.5;color:#607089;">
                              Mensagem automática enviada pelo Xsign. Se você não solicitou esta confirmação, ignore este e-mail.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nome, link, validade, link);
    }

    private String criarLinkConfirmacao(String token) {
        return UriComponentsBuilder
                .fromUriString(frontendBaseUrl.replaceAll("/+$", ""))
                .path("/confirmar-email")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String gerarHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o token de confirmação", exception);
        }
    }

    private String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
