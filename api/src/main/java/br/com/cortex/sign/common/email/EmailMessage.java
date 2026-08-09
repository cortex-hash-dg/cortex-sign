package br.com.cortex.sign.common.email;

public record EmailMessage(
        String destinatarioEmail,
        String destinatarioNome,
        String assunto,
        String texto,
        String html
) {
}
