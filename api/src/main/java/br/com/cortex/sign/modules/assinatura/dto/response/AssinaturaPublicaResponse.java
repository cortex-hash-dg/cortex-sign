package br.com.cortex.sign.modules.assinatura.dto.response;

import br.com.cortex.sign.modules.assinatura.enums.StatusSolicitacaoAssinatura;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssinaturaPublicaResponse(
        UUID solicitacaoAssinaturaId,
        UUID documentoId,
        String documentoTitulo,
        String signatarioNome,
        String signatarioEmail,
        Boolean acessoExternoObrigatorio,
        StatusSolicitacaoAssinatura status,
        LocalDateTime expiraEm
) {
}
