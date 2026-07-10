package br.com.cortex.sign.modules.auth.dto.response;

import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoginResponse(
        String tokenAcesso,
        String tokenAtualizacao,
        String tipoToken,
        LocalDateTime tokenAcessoExpiraEm,
        LocalDateTime tokenAtualizacaoExpiraEm,
        UUID usuarioId,
        UUID organizacaoId,
        String organizacaoNome,
        String nome,
        String email,
        PerfilUsuario perfil
) {
}
