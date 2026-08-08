package br.com.cortex.sign.modules.auth.dto.response;

public record ConfirmarEmailResponse(
        Boolean emailVerificado,
        String mensagem
) {
}
