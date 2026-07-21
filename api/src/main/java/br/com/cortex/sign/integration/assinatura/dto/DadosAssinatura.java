package br.com.cortex.sign.integration.assinatura.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DadosAssinatura(
        UUID documentoId,
        String documentoTitulo,
        String documentoHashSha256,
        Long documentoTamanhoBytes,
        UUID organizacaoId,
        String organizacaoNome,
        UUID solicitacaoAssinaturaId,
        LocalDateTime solicitacaoCriadaEm,
        LocalDateTime solicitacaoExpiraEm,
        UUID signatarioId,
        String signatarioNome,
        String signatarioEmail,
        String codigoHash,
        String ip,
        String userAgent
) {
}
