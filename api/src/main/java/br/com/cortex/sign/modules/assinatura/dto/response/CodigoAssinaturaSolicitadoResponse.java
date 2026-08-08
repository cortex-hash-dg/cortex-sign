package br.com.cortex.sign.modules.assinatura.dto.response;

import br.com.cortex.sign.modules.assinatura.enums.CanalCodigoAssinatura;
import java.time.LocalDateTime;

public record CodigoAssinaturaSolicitadoResponse(
        CanalCodigoAssinatura canal,
        String destino,
        String mensagem,
        LocalDateTime solicitadoEm
) {
}
