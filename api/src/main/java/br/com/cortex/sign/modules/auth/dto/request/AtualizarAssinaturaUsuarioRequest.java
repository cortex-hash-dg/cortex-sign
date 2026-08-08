package br.com.cortex.sign.modules.auth.dto.request;

import jakarta.validation.constraints.Size;

public record AtualizarAssinaturaUsuarioRequest(
        String assinaturaManuscritaBase64,

        @Size(max = 150, message = "O nome da assinatura deve ter no máximo 150 caracteres")
        String nomeAssinatura
) {
}
