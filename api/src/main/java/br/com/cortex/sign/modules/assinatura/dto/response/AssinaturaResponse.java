package br.com.cortex.sign.modules.assinatura.dto.response;

import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.TipoAssinatura;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssinaturaResponse(
        UUID id,
        UUID solicitacaoAssinaturaId,
        TipoAssinatura tipo,
        StatusAssinatura status,
        TipoProvedorAssinatura provedor,
        String protocolo,
        LocalDateTime assinadoEm,
        LocalDateTime rejeitadoEm,
        String motivoRejeicao,
        String metadados,
        LocalDateTime criadoEm
) {
}
