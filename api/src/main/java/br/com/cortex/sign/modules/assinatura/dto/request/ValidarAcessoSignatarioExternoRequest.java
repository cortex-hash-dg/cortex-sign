package br.com.cortex.sign.modules.assinatura.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidarAcessoSignatarioExternoRequest(
        @NotBlank(message = "O CPF é obrigatório")
        @Size(max = 20, message = "O CPF deve ter no máximo 20 caracteres")
        String cpf,

        @NotBlank(message = "O código é obrigatório")
        @Size(max = 12, message = "O código deve ter no máximo 12 caracteres")
        String codigo
) {
}
