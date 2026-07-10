package br.com.cortex.sign.modules.documento.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarDocumentoRequest(
        @NotBlank(message = "O título do documento é obrigatório")
        @Size(max = 200, message = "O título do documento deve ter no máximo 200 caracteres")
        String titulo
) {
}
