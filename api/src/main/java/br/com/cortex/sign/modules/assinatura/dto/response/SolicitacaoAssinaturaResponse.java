package br.com.cortex.sign.modules.assinatura.dto.response;

import br.com.cortex.sign.modules.assinatura.enums.StatusSolicitacaoAssinatura;
import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoAssinaturaResponse(
        UUID id,
        UUID documentoId,
        String documentoTitulo,
        UUID signatarioId,
        String signatarioNome,
        String signatarioEmail,
        StatusSolicitacaoAssinatura status,
        String token,
        String codigoTeste,
        LocalDateTime expiraEm,
        AssinaturaResponse assinatura,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
