package br.com.cortex.sign.modules.auth.dto.response;

import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import java.time.LocalDate;
import java.util.UUID;

public record UsuarioAutenticadoResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNome,
        String nome,
        String email,
        PerfilUsuario perfil,
        String cpf,
        String telefone,
        String nomeSocial,
        LocalDate dataNascimento,
        Boolean emailVerificado,
        Boolean telefoneVerificado
) {
}
