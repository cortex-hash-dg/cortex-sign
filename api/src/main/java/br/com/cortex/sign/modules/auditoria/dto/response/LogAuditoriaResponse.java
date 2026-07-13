package br.com.cortex.sign.modules.auditoria.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LogAuditoriaResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNome,
        UUID usuarioId,
        String usuarioNome,
        String acao,
        String entidadeTipo,
        UUID entidadeId,
        String detalhes,
        LocalDateTime criadoEm
) {
}
