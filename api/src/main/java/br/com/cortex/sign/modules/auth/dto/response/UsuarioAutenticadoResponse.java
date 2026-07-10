package br.com.cortex.sign.modules.auth.dto.response;

import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import java.util.UUID;

public record UsuarioAutenticadoResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNome,
        String nome,
        String email,
        PerfilUsuario perfil
) {
}
