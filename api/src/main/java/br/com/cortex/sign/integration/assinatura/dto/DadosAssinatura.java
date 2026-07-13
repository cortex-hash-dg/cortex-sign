package br.com.cortex.sign.integration.assinatura.dto;

import java.util.UUID;

public record DadosAssinatura(
        UUID documentoId,
        UUID solicitacaoAssinaturaId,
        UUID signatarioId,
        String signatarioNome,
        String signatarioEmail,
        String ip,
        String userAgent
) {
}
