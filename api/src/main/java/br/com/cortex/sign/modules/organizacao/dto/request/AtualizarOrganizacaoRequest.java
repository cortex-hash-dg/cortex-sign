package br.com.cortex.sign.modules.organizacao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarOrganizacaoRequest(
        @NotBlank(message = "O nome da organização é obrigatório")
        @Size(max = 150, message = "O nome da organização deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 30, message = "O número do documento deve ter no máximo 30 caracteres")
        String numeroDocumento
) {
}
