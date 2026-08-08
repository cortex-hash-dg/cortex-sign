package br.com.cortex.sign.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AtualizarCadastroUsuarioRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome,

        @Email(message = "Informe um e-mail válido")
        @NotBlank(message = "O e-mail é obrigatório")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 20, message = "O CPF deve ter no máximo 20 caracteres")
        String cpf,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        @Size(max = 150, message = "O nome social deve ter no máximo 150 caracteres")
        String nomeSocial,

        LocalDate dataNascimento
) {
}
