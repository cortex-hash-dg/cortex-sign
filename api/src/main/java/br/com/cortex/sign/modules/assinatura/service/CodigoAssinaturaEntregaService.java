package br.com.cortex.sign.modules.assinatura.service;

import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.email.EmailDeliveryService;
import br.com.cortex.sign.common.email.EmailMessage;
import br.com.cortex.sign.modules.assinatura.entity.SolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.CanalCodigoAssinatura;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodigoAssinaturaEntregaService {

    private static final DateTimeFormatter FORMATADOR_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final EmailDeliveryService emailDeliveryService;

    public void enviarLink(SolicitacaoAssinatura solicitacao, CanalCodigoAssinatura canal, String linkAssinatura) {
        validarCanalEmail(canal);
        enviarLinkPorEmail(solicitacao, linkAssinatura);
    }

    public void enviarCodigoAcessoExterno(SolicitacaoAssinatura solicitacao, CanalCodigoAssinatura canal, String codigo) {
        validarCanalEmail(canal);
        enviarCodigoPorEmail(solicitacao, codigo);
    }

    private void validarCanalEmail(CanalCodigoAssinatura canal) {
        if (canal != CanalCodigoAssinatura.EMAIL) {
            throw new ConflitoException("O envio está disponível apenas por e-mail");
        }
    }

    private void enviarLinkPorEmail(SolicitacaoAssinatura solicitacao, String linkAssinatura) {
        Signatario signatario = solicitacao.getSignatario();
        validarConfiguracaoEmail(signatario);

        EmailMessage mensagem = new EmailMessage(
                signatario.getEmail(),
                signatario.getNome(),
                "Documento para assinatura no Xsign",
                criarTextoSimples(solicitacao, linkAssinatura),
                criarHtml(solicitacao, linkAssinatura)
        );

        emailDeliveryService.enviar(mensagem);
    }

    private void validarConfiguracaoEmail(Signatario signatario) {
        if (signatario.getEmail() == null || signatario.getEmail().isBlank()) {
            throw new ConflitoException("O signatário não possui e-mail para receber o link de assinatura");
        }
    }

    private String criarTextoSimples(SolicitacaoAssinatura solicitacao, String linkAssinatura) {
        Signatario signatario = solicitacao.getSignatario();
        return """
                Olá, %s.

                Você recebeu uma solicitação para assinar um documento no Xsign.

                Documento: %s
                Organização: %s
                Gerado em: %s

                Acesse o link seguro para revisar e assinar:
                %s

                Se você não reconhece esta solicitação, ignore esta mensagem.
                """.formatted(
                signatario.getNome(),
                signatario.getDocumento().getTitulo(),
                signatario.getDocumento().getOrganizacao().getNome(),
                java.time.LocalDateTime.now().format(FORMATADOR_DATA),
                linkAssinatura
        );
    }

    private void enviarCodigoPorEmail(SolicitacaoAssinatura solicitacao, String codigo) {
        Signatario signatario = solicitacao.getSignatario();
        validarConfiguracaoEmail(signatario);

        EmailMessage mensagem = new EmailMessage(
                signatario.getEmail(),
                signatario.getNome(),
                "Código de acesso para assinatura no Xsign",
                criarTextoCodigoAcesso(solicitacao, codigo),
                criarHtmlCodigoAcesso(solicitacao, codigo)
        );

        emailDeliveryService.enviar(mensagem);
    }

    private String criarTextoCodigoAcesso(SolicitacaoAssinatura solicitacao, String codigo) {
        Signatario signatario = solicitacao.getSignatario();
        return """
                Olá, %s.

                Use o código abaixo para liberar seu acesso ao documento "%s" no Xsign.

                Código: %s

                Se você não reconhece esta solicitação, ignore esta mensagem.
                """.formatted(
                signatario.getNome(),
                signatario.getDocumento().getTitulo(),
                codigo
        );
    }

    private String criarHtmlCodigoAcesso(SolicitacaoAssinatura solicitacao, String codigo) {
        Signatario signatario = solicitacao.getSignatario();
        String nome = escaparHtml(signatario.getNome());
        String documento = escaparHtml(signatario.getDocumento().getTitulo());
        String codigoSeguro = escaparHtml(codigo);

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Código de acesso Xsign</title>
                </head>
                <body style="margin:0;background:#f4f8fc;font-family:Inter,Arial,sans-serif;color:#0b2345;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f8fc;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:520px;background:#ffffff;border:1px solid #dce4ee;border-radius:18px;overflow:hidden;box-shadow:0 12px 34px rgba(2,38,85,.08);">
                          <tr>
                            <td style="padding:28px 32px 18px;border-bottom:1px solid #e7edf5;">
                              <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#087ed1;">Xsign</div>
                              <h1 style="margin:12px 0 8px;font-size:24px;line-height:1.25;color:#082345;">Código de acesso</h1>
                              <p style="margin:0;font-size:15px;line-height:1.6;color:#607089;">Olá, %s. Use este código para liberar seu acesso ao documento "%s".</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 32px;text-align:center;">
                              <div style="display:inline-block;border-radius:14px;background:#edf7ff;border:1px solid #bfe2ff;padding:16px 24px;font-size:30px;font-weight:800;letter-spacing:.18em;color:#082345;">%s</div>
                              <p style="margin:22px 0 0;font-size:13px;line-height:1.6;color:#607089;">Se você não reconhece essa solicitação, ignore este e-mail.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nome, documento, codigoSeguro);
    }

    private String criarHtml(SolicitacaoAssinatura solicitacao, String linkAssinatura) {
        Signatario signatario = solicitacao.getSignatario();
        String nome = escaparHtml(signatario.getNome());
        String documento = escaparHtml(signatario.getDocumento().getTitulo());
        String organizacao = escaparHtml(signatario.getDocumento().getOrganizacao().getNome());
        String geradoEm = java.time.LocalDateTime.now().format(FORMATADOR_DATA);
        String link = escaparHtml(linkAssinatura);

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Documento para assinatura no Xsign</title>
                </head>
                <body style="margin:0;background:#f4f8fc;font-family:Inter,Arial,sans-serif;color:#0b2345;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f8fc;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #dce4ee;border-radius:18px;overflow:hidden;box-shadow:0 12px 34px rgba(2,38,85,.08);">
                          <tr>
                            <td style="padding:28px 32px 18px;border-bottom:1px solid #e7edf5;">
                              <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#087ed1;">Xsign</div>
                              <h1 style="margin:12px 0 8px;font-size:24px;line-height:1.25;color:#082345;">Documento para assinatura</h1>
                              <p style="margin:0;font-size:15px;line-height:1.6;color:#607089;">Olá, %s. Você recebeu uma solicitação para revisar e assinar um documento no Xsign.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 32px;">
                              <a href="%s" style="display:block;border-radius:12px;background:#087ed1;padding:14px 18px;text-align:center;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">Abrir documento para assinatura</a>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin-top:24px;border-collapse:collapse;">
                                <tr>
                                  <td style="padding:12px 0;border-bottom:1px solid #edf2f7;font-size:13px;color:#607089;">Documento</td>
                                  <td align="right" style="padding:12px 0;border-bottom:1px solid #edf2f7;font-size:13px;font-weight:700;color:#0b2345;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 0;border-bottom:1px solid #edf2f7;font-size:13px;color:#607089;">Organização</td>
                                  <td align="right" style="padding:12px 0;border-bottom:1px solid #edf2f7;font-size:13px;font-weight:700;color:#0b2345;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:12px 0;font-size:13px;color:#607089;">Gerado em</td>
                                  <td align="right" style="padding:12px 0;font-size:13px;font-weight:700;color:#0b2345;">%s</td>
                                </tr>
                              </table>
                              <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#607089;">Se o botão não abrir, copie e cole este endereço no navegador:<br><span style="word-break:break-all;color:#087ed1;">%s</span></p>
                              <p style="margin:12px 0 0;font-size:13px;line-height:1.6;color:#607089;">Se você não reconhece essa solicitação, ignore este e-mail.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 32px;background:#f8fbff;border-top:1px solid #e7edf5;font-size:12px;line-height:1.5;color:#607089;">
                              Mensagem automática enviada pelo Xsign. O link é individual e deve ser usado apenas pelo signatário indicado.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nome, link, documento, organizacao, geradoEm, link);
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
