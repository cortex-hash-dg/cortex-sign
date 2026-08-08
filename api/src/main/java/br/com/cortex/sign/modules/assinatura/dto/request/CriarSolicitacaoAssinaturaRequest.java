package br.com.cortex.sign.modules.assinatura.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CriarSolicitacaoAssinaturaRequest(
        @NotNull(message = "O signatário é obrigatório")
        UUID signatarioId,

        @Min(value = 1, message = "A validade deve ser de pelo menos 1 dia")
        Integer validadeDias,

        Boolean semValidade
) {
}
