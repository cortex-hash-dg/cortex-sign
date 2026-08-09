package br.com.cortex.sign.common.email;

import br.com.cortex.sign.common.exception.ConflitoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    @Value("${app.notificacao.email.from:}")
    private String emailOrigem;

    @Value("${app.notificacao.email.from-name:Xsign}")
    private String nomeOrigem;

    @Value("${app.notificacao.email.reply-to:}")
    private String emailResposta;

    @Value("${app.notificacao.email.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.notificacao.email.brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    public void enviar(EmailMessage mensagem) {
        validarMensagem(mensagem);

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            enviarPorBrevo(mensagem);
            return;
        }

        enviarPorSmtp(mensagem);
    }

    private void validarMensagem(EmailMessage mensagem) {
        if (emailOrigem == null || emailOrigem.isBlank()) {
            throw new ConflitoException("Envio por e-mail ainda não está configurado");
        }

        if (mensagem.destinatarioEmail() == null || mensagem.destinatarioEmail().isBlank()) {
            throw new ConflitoException("Destinatário sem e-mail cadastrado");
        }
    }

    private void enviarPorBrevo(EmailMessage mensagem) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(brevoApiUrl))
                    .timeout(HTTP_TIMEOUT)
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(criarPayloadBrevo(mensagem), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "Brevo recusou envio de e-mail para {} usando remetente {}. Status: {}. Resposta: {}",
                        mensagem.destinatarioEmail(),
                        emailOrigem,
                        response.statusCode(),
                        response.body()
                );
                throw new ConflitoException("Não foi possível enviar o e-mail pela Brevo. Verifique remetente, chave e domínio.");
            }
        } catch (IOException exception) {
            log.warn("Falha de comunicação com Brevo ao enviar e-mail para {}: {}", mensagem.destinatarioEmail(), exception.getMessage(), exception);
            throw new ConflitoException("Não foi possível comunicar com a Brevo para enviar o e-mail.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Envio de e-mail pela Brevo interrompido para {}: {}", mensagem.destinatarioEmail(), exception.getMessage(), exception);
            throw new ConflitoException("Envio de e-mail interrompido. Tente novamente em instantes.");
        }
    }

    private String criarPayloadBrevo(EmailMessage mensagem) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of(
                "name", nomeOrigem,
                "email", emailOrigem
        ));
        payload.put("to", List.of(Map.of(
                "name", mensagem.destinatarioNome() == null || mensagem.destinatarioNome().isBlank()
                        ? mensagem.destinatarioEmail()
                        : mensagem.destinatarioNome(),
                "email", mensagem.destinatarioEmail()
        )));
        payload.put("subject", mensagem.assunto());
        payload.put("textContent", mensagem.texto());
        payload.put("htmlContent", mensagem.html());

        if (emailResposta != null && !emailResposta.isBlank()) {
            payload.put("replyTo", Map.of("email", emailResposta));
        }

        return objectMapper.writeValueAsString(payload);
    }

    private void enviarPorSmtp(EmailMessage email) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new ConflitoException("Envio por e-mail ainda não está configurado");
        }

        MimeMessage mensagem = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensagem,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(emailOrigem, nomeOrigem);
            helper.setTo(email.destinatarioEmail());
            if (emailResposta != null && !emailResposta.isBlank()) {
                helper.setReplyTo(emailResposta);
            }
            helper.setSubject(email.assunto());
            helper.setText(email.texto(), email.html());

            mailSender.send(mensagem);
        } catch (MessagingException | MailException exception) {
            log.warn("Falha ao enviar e-mail via SMTP para {} usando remetente {}: {}", email.destinatarioEmail(), emailOrigem, exception.getMessage(), exception);
            throw new ConflitoException("Não foi possível enviar o e-mail via SMTP. Tente novamente em instantes.");
        } catch (java.io.UnsupportedEncodingException exception) {
            log.warn("Remetente de e-mail configurado incorretamente: {}", emailOrigem, exception);
            throw new ConflitoException("Remetente de e-mail configurado incorretamente");
        }
    }
}
