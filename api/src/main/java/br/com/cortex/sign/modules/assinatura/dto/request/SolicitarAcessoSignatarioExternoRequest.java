package br.com.cortex.sign.modules.assinatura.dto.request;

import br.com.cortex.sign.modules.assinatura.enums.CanalCodigoAssinatura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitarAcessoSignatarioExternoRequest(
        @NotBlank(message = "O CPF é obrigatório")
        @Size(max = 20, message = "O CPF deve ter no máximo 20 caracteres")
        String cpf,

        @NotNull(message = "O canal de envio é obrigatório")
        CanalCodigoAssinatura canal
) {
}
