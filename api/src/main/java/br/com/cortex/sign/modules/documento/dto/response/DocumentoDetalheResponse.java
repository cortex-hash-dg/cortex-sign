package br.com.cortex.sign.modules.documento.dto.response;

import br.com.cortex.sign.modules.documento.enums.StatusDocumento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DocumentoDetalheResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNome,
        String titulo,
        StatusDocumento status,
        UUID criadoPorUsuarioId,
        String criadoPorUsuarioNome,
        ArquivoDocumentoResponse arquivoAtual,
        List<DocumentoVersaoResponse> versoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
