package br.com.cortex.sign.modules.assinatura.dto.request;

import br.com.cortex.sign.modules.assinatura.enums.CanalCodigoAssinatura;
import jakarta.validation.constraints.NotNull;

public record SolicitarCodigoAssinaturaRequest(
        @NotNull(message = "O canal de envio é obrigatório")
        CanalCodigoAssinatura canal
) {
}
