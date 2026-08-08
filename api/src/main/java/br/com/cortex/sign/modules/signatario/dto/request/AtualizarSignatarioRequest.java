package br.com.cortex.sign.modules.signatario.dto.request;

import br.com.cortex.sign.modules.signatario.enums.TipoSignatario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarSignatarioRequest(
        @NotBlank(message = "O nome do signatário é obrigatório")
        @Size(max = 150, message = "O nome do signatário deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail do signatário é obrigatório")
        @Email(message = "O e-mail do signatário deve ser válido")
        @Size(max = 150, message = "O e-mail do signatário deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 30, message = "O documento do signatário deve ter no máximo 30 caracteres")
        String numeroDocumento,

        @Size(max = 30, message = "O telefone do signatário deve ter no máximo 30 caracteres")
        String telefone,

        @NotNull(message = "O tipo do signatário é obrigatório")
        TipoSignatario tipo,

        @Min(value = 1, message = "A ordem de assinatura deve ser maior ou igual a 1")
        Integer ordemAssinatura
) {
}
