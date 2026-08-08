package br.com.cortex.sign.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarEmailRequest(
        @NotBlank(message = "O token de confirmação é obrigatório")
        String token
) {
}
