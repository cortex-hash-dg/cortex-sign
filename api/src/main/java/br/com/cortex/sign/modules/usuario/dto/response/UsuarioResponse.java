package br.com.cortex.sign.modules.usuario.dto.response;

import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNome,
        String nome,
        String email,
        String cpf,
        String telefone,
        PerfilUsuario perfil,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
