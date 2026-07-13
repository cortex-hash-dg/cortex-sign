package br.com.cortex.sign.modules.assinatura.dto.request;

import br.com.cortex.sign.modules.assinatura.enums.TipoAssinatura;
import jakarta.validation.constraints.NotBlank;

public record AssinarComTokenRequest(
        @NotBlank(message = "O código de assinatura é obrigatório")
        String codigo,

        TipoAssinatura tipo
) {
}
