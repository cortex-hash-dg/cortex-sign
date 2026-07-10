package br.com.cortex.sign.modules.documento.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoVersaoResponse(
        UUID id,
        Integer numeroVersao,
        ArquivoDocumentoResponse arquivo,
        UUID criadoPorUsuarioId,
        String criadoPorUsuarioNome,
        LocalDateTime criadoEm
) {
}
