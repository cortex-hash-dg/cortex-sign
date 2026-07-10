package br.com.cortex.sign.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "O token de atualização é obrigatório")
        String tokenAtualizacao
) {
}
