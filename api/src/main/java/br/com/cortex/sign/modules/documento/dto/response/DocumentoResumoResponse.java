package br.com.cortex.sign.modules.documento.dto.response;

import br.com.cortex.sign.modules.documento.enums.StatusDocumento;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoResumoResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNome,
        String titulo,
        StatusDocumento status,
        UUID criadoPorUsuarioId,
        String criadoPorUsuarioNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
