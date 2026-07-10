package br.com.cortex.sign.modules.auth.jwt;

import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import java.util.UUID;

public record UsuarioAutenticado(
        UUID id,
        UUID organizacaoId,
        String nome,
        String email,
        PerfilUsuario perfil
) {
}
