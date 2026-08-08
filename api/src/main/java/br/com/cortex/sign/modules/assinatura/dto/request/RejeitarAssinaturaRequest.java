package br.com.cortex.sign.modules.assinatura.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejeitarAssinaturaRequest(
        String codigo,

        @NotBlank(message = "O motivo da rejeição é obrigatório")
        @Size(max = 500, message = "O motivo da rejeição deve ter no máximo 500 caracteres")
        String motivo,

        String tokenAcessoExterno
) {
}
