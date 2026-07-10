package br.com.cortex.sign.modules.documento.dto.response;

import br.com.cortex.sign.integration.storage.enums.TipoProvedorStorage;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArquivoDocumentoResponse(
        UUID id,
        TipoProvedorStorage provedor,
        String nomeOriginal,
        String nomeArmazenado,
        String caminho,
        String tipoConteudo,
        Long tamanhoBytes,
        String checksumSha256,
        LocalDateTime criadoEm
) {
}
