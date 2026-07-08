package br.com.cortex.sign.modules.usuario.dto.request;

import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CriarUsuarioRequest(
        UUID organizacaoId,

        @NotBlank(message = "O nome do usuário é obrigatório")
        @Size(max = 150, message = "O nome do usuário deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha,

        @NotNull(message = "O perfil é obrigatório")
        PerfilUsuario perfil
) {
}
